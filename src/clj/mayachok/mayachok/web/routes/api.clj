(ns mayachok.mayachok.web.routes.api
  (:require
   [mayachok.mayachok.web.controllers.health :as health]
   [mayachok.mayachok.web.middleware.exception :as exception]
   [mayachok.mayachok.web.middleware.formats :as formats]
   [mayachok.mayachok.web.routes.utils :as utils]
   [clojure.string :as str]
   [clojure.tools.logging :as log]
   [integrant.core :as ig]
   [reitit.coercion.malli :as malli]
   [reitit.ring.coercion :as coercion]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.parameters :as parameters]
   [reitit.swagger :as swagger]
   [reitit.swagger-ui :as swagger-ui]
   [ring.util.http-response :as http-response]))

;; -- rate limiter (in-memory, per-IP) ---------------------------------------

(def ^:private api-rate-limiter (atom {}))

(def ^:private api-max-requests-per-minute 60)

(defn- api-rate-limit-check [ip]
  (let [now (System/currentTimeMillis)
        minute-ago (- now (* 60 1000))]
    (swap! api-rate-limiter
           (fn [m]
             (let [timestamps (get m ip [])
                   recent    (filterv #(> % minute-ago) timestamps)]
               (if (>= (count recent) api-max-requests-per-minute)
                 m
                 (assoc m ip (conj recent now))))))
    (< (count (get @api-rate-limiter ip [])) api-max-requests-per-minute)))

(defn reset-api-rate-limiter! []
  (reset! api-rate-limiter {}))

(defn- wrap-api-rate-limit [handler]
  (fn [request]
    (let [ip (or (get-in request [:headers "x-forwarded-for"])
                 (:remote-addr request))]
      (if (api-rate-limit-check ip)
        (handler request)
        (do (log/warn "API rate limited, ip=" ip)
            {:status 429
             :headers {"Content-Type" "application/json"}
             :body "{\"error\":\"Too many requests. Please slow down.\"}"})))))

;; -- route data --------------------------------------------------------------

(defn- route-data [opts]
  (merge
   {:coercion   malli/coercion
    :muuntaja   formats/instance
    :swagger    {:id ::api}
    :middleware [parameters/parameters-middleware
                 muuntaja/format-negotiate-middleware
                 muuntaja/format-response-middleware
                 coercion/coerce-exceptions-middleware
                 muuntaja/format-request-middleware
                 coercion/coerce-response-middleware
                 coercion/coerce-request-middleware
                 exception/wrap-exception]}
   (select-keys opts [:query-fn])))

;; -- stats -------------------------------------------------------------------

(defn- build-survey-map [survey-rows]
  (let [by-age    (reduce (fn [acc r]
                            (if (:age_range r)
                              (update acc (str (:age_range r)) (fnil + 0) (:count r))
                              acc))
                          {}
                          survey-rows)
        by-time   (reduce (fn [acc r]
                            (if (:time_since_birth r)
                              (update acc (str (:time_since_birth r)) (fnil + 0) (:count r))
                              acc))
                          {}
                          survey-rows)
        by-first  (reduce (fn [acc r]
                            (if (:first_child r)
                              (update acc (str (:first_child r)) (fnil + 0) (:count r))
                              acc))
                          {}
                          survey-rows)]
    {:age_range       (if (empty? by-age) nil by-age)
     :time_since_birth (if (empty? by-time) nil by-time)
     :first_child      (if (empty? by-first) nil by-first)}))

(defn- stats-handler [request]
  (let [query-fn (utils/route-data-key request :query-fn)]
    (try
      (let [global      (or (query-fn :stats-global {}) {})
            survey-rows (query-fn :stats-survey-breakdown {})
            regions     (query-fn :heatmap-data {})]
        (http-response/ok
         {:total     (or (:total global) 0)
          :avg_score (:avg_score global)
          :risk      {:low-risk             (or (:low_risk global) 0)
                      :possible-depression  (or (:possible_depression global) 0)
                      :probable-depression  (or (:probable_depression global) 0)
                      :self-harm-risk       (or (:self_harm_risk global) 0)}
          :survey    (build-survey-map survey-rows)
          :regions   (vec (for [r regions]
                            {:region          (:region r)
                             :total           (:total r)
                             :avg_score       (:avg_score r)
                             :self_harm_count (:self_harm_count r)
                             :probable_count  (:probable_count r)
                             :possible_count  (:possible_count r)
                             :low_count       (:low_count r)
                             :lat             (:avg_lat r)
                             :lng             (:avg_lng r)}))}))
      (catch Exception e
        (log/error e "failed to fetch stats")
        (http-response/internal-server-error {:error "Failed to fetch stats"})))))

;; -- export ------------------------------------------------------------------

(defn- csv-escape [s]
  (if (string? s)
    (str "\"" (str/replace s "\"" "\"\"") "\"")
    (str s)))

(defn- screenings-to-csv [screenings]
  (let [headers ["id" "created_at" "locale" "total_score" "q10_score" "risk_level"
                 "age_range" "time_since_birth" "first_child" "location_text"]
        rows (map (fn [s]
                    (map #(csv-escape (get s (keyword %) "")) headers))
                  screenings)]
    (str/join "\n"
              (cons (str/join "," headers)
                    (map #(str/join "," %) rows)))))

(defn- export-handler [request]
  (let [query-fn (utils/route-data-key request :query-fn)]
    (try
      (let [screenings (query-fn :list-screenings-stats {})
            csv (screenings-to-csv screenings)]
        {:status 200
         :headers {"Content-Type" "text/csv; charset=utf-8"
                   "Content-Disposition" "attachment; filename=\"mayachok-screenings.csv\""}
         :body csv})
      (catch Exception e
        (log/error e "failed to export screenings")
        (http-response/internal-server-error {:error "Failed to export data"})))))

;; -- routes ------------------------------------------------------------------

(defn- api-routes []
  [["/swagger.json"
    {:get {:no-doc  true
           :swagger {:info {:title "Mayachok API"
                           :description "Postpartum depression screening API"
                           :version "0.1.0"}}
           :handler (swagger/create-swagger-handler)}}]
   ["/api-docs/*"
    {:get (swagger-ui/create-swagger-ui-handler
           {:url "/api/swagger.json"
            :config {:validator-url nil}})}]
   ["/health"
    {:get {:handler (-> #'health/healthcheck! wrap-api-rate-limit)
           :summary "Health check"}}]
   ["/stats"
    {:get {:handler (-> #'stats-handler wrap-api-rate-limit)
           :summary "Screening statistics"}}]
   ["/export"
    {:get {:handler #'export-handler
           :summary "Export all screenings as CSV"}}]])

(derive :reitit.routes/api :reitit/routes)

(defmethod ig/init-key :reitit.routes/api
  [_ {:keys [base-path]
      :or   {base-path ""}
      :as   opts}]
  (fn [] [base-path (route-data opts) (api-routes)]))
