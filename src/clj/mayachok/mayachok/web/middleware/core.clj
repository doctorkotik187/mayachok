(ns mayachok.mayachok.web.middleware.core
  (:require
   [mayachok.mayachok.env :as env]
   [ring.middleware.defaults :as defaults]
   [ring.middleware.session.cookie :as cookie]))

(def ^:private csp-header
  (str "default-src 'self'; "
       "style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://unpkg.com https://*.unpkg.com; "
       "script-src 'self' 'unsafe-inline' https://unpkg.com https://*.unpkg.com; "
       "img-src 'self' data: https://unpkg.com https://*.unpkg.com https://*.tile.openstreetmap.org https://*.basemaps.cartocdn.com; "
       "font-src 'self' https://cdn.jsdelivr.net; "
       "connect-src 'self' https://*.tile.openstreetmap.org https://*.basemaps.cartocdn.com; "
       "frame-ancestors 'none'"))

(defn- wrap-csp [handler]
  (fn [request]
    (let [response (handler request)]
      (assoc-in response [:headers "Content-Security-Policy"] csp-header))))

(defn wrap-base
  [{:keys [metrics site-defaults-config cookie-secret] :as opts}]
  (let [cookie-store (cookie/cookie-store {:key (.getBytes ^String cookie-secret)})]
    (fn [handler]
      (cond-> ((:middleware env/defaults) handler opts)
        true (defaults/wrap-defaults
              (assoc-in site-defaults-config [:session :store] cookie-store))
        true wrap-csp))))
