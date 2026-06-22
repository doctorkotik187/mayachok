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

(defn- stats-handler [request]
  (let [query-fn (utils/route-data-key request :query-fn)
        params   (:query-params request)
        region   (or (get params "region") (get params :region))
        risk     (or (get params "risk") (get params :risk))
        limit    (Integer/parseInt (or (get params "limit") (get params :limit) "100"))]
    (try
      (let [screenings (cond
                         risk   (query-fn :list-screenings-by-risk {:risk_level risk})
                         region (query-fn :list-screenings-by-region {})
                         :else  (query-fn :list-screenings-stats {}))
            total      (count screenings)
            limited    (vec (take limit screenings))
            by-risk    (reduce (fn [acc s]
                                 (update acc (keyword (:risk_level s)) (fnil inc 0)))
                               {}
                               screenings)
            by-region  (reduce (fn [acc s]
                                 (let [r (or (:location_text s) "unknown")]
                                   (update acc r (fnil inc 0))))
                               {}
                               screenings)]
        (http-response/ok
         {:total      total
          :risk       by-risk
          :regions    by-region
          :screenings limited}))
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
           :summary "Health check"
           :responses {200 {:body {:status string?}}}}}]
   ["/stats"
    {:get {:handler #'stats-handler
           :summary "Screening statistics"
           :parameters {:query [:map
                                [:risk {:optional true} string?]
                                [:region {:optional true} string?]
                                [:limit {:optional true} int?]]}
           :responses {200 {:body [:map
                                   [:total int?]
                                   [:risk [:map-of keyword? int?]]
                                   [:regions [:map-of string? int?]]
                                   [:screenings [:vector :map]]]}
                      500 {:body {:error string?}}}}}]])

(derive :reitit.routes/api :reitit/routes)

(defmethod ig/init-key :reitit.routes/api
  [_ {:keys [base-path]
      :or   {base-path ""}
      :as   opts}]
  (fn [] [base-path (route-data opts) (api-routes)]))
