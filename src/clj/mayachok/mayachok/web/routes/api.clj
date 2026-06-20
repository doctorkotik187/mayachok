(ns mayachok.mayachok.web.routes.api
  (:require
    [mayachok.mayachok.web.controllers.health :as health]
    [mayachok.mayachok.web.controllers.screening :as screening]
    [mayachok.mayachok.web.middleware.exception :as exception]
    [mayachok.mayachok.web.middleware.formats :as formats]
    [integrant.core :as ig]
    [reitit.coercion.malli :as malli]
    [reitit.ring.coercion :as coercion]
    [reitit.ring.middleware.muuntaja :as muuntaja]
    [reitit.ring.middleware.parameters :as parameters]
    [reitit.swagger :as swagger]))

(defn- route-data [opts]
  (merge
    {:coercion   malli/coercion
     :muuntaja   formats/instance
     :swagger    {:id ::api}
     :middleware [;; query-params & form-params
                  parameters/parameters-middleware
                    ;; content-negotiation
                  muuntaja/format-negotiate-middleware
                    ;; encoding response body
                  muuntaja/format-response-middleware
                    ;; exception handling
                  coercion/coerce-exceptions-middleware
                    ;; decoding request body
                  muuntaja/format-request-middleware
                    ;; coercing response bodys
                  coercion/coerce-response-middleware
                    ;; coercing request parameters
                  coercion/coerce-request-middleware
                    ;; exception handling
                  exception/wrap-exception]}
    (select-keys opts [:query-fn])))

;; Routes
(defn- api-routes []
  [["/swagger.json"
    {:get {:no-doc  true
           :swagger {:info {:title "mayachok.mayachok API"}}
           :handler (swagger/create-swagger-handler)}}]
   ["/health"
    {:get #'health/healthcheck!}]
   ["/screenings"
    {:post {:handler #'screening/create-screening!}}]
   ["/screenings/:id"
    {:get {:handler #'screening/get-screening}}]
   ["/screenings/:id/survey"
    {:post {:handler #'screening/update-survey!}}]])

(derive :reitit.routes/api :reitit/routes)

(defmethod ig/init-key :reitit.routes/api
  [_ {:keys [base-path]
      :or   {base-path ""}
      :as   opts}]
  (fn [] [base-path (route-data opts) (api-routes)]))
