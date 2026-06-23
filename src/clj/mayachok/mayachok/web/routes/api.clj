(ns mayachok.mayachok.web.routes.api
  (:require
   [mayachok.mayachok.web.controllers.health :as health]
   [mayachok.mayachok.web.middleware.exception :as exception]
   [mayachok.mayachok.web.middleware.formats :as formats]
   [mayachok.mayachok.web.routes.utils :as utils]
   [clojure.tools.logging :as log]
   [integrant.core :as ig]
   [reitit.coercion.malli :as malli]
   [reitit.ring.coercion :as coercion]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.parameters :as parameters]
   [reitit.swagger :as swagger]
   [reitit.swagger-ui :as swagger-ui]
   [ring.util.http-response :as http-response]))

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

;; -- routes ------------------------------------------------------------------

(defn- api-routes []
  [["/swagger.json"
    {:get {:no-doc  true
           :swagger {:info {:title "Mayachok API"
                           :description "Postpartum depression screening API"
                           :version "1.0.0"}}
           :handler (swagger/create-swagger-handler)}}]
   ["/api-docs/*"
    {:get (swagger-ui/create-swagger-ui-handler
           {:url "/api/swagger.json"
            :config {:validator-url nil}})}]
   ["/health"
    {:get {:handler #'health/healthcheck!
           :summary "Health check"}}]
   ["/stats"
    {:get {:handler #'stats-handler
           :summary "Screening statistics"}}]])

(derive :reitit.routes/api :reitit/routes)

(defmethod ig/init-key :reitit.routes/api
  [_ {:keys [base-path]
      :or   {base-path ""}
      :as   opts}]
  (fn [] [base-path (route-data opts) (api-routes)]))
