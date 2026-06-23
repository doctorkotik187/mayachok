(ns mayachok.mayachok.core
  (:require
   [clojure.tools.logging :as log]
   [clojure.string :as str]
   [integrant.core :as ig]
   [mayachok.mayachok.config :as config]
   [mayachok.mayachok.env :refer [defaults]]

    ;; Edges
   [kit.edge.server.undertow]
   [mayachok.mayachok.web.handler]

    ;; Routes
   [mayachok.mayachok.web.routes.api]
   [kit.edge.db.sql.conman]
   [kit.edge.db.sql.migratus]
   [mayachok.mayachok.web.routes.pages])
  (:gen-class))

;; log uncaught exceptions in threads
(Thread/setDefaultUncaughtExceptionHandler
 (fn [thread ex]
   (log/error {:what :uncaught-exception
               :exception ex
               :where (str "Uncaught exception on" (.getName thread))})))

(defonce system (atom nil))

(def ^:private default-retention-days 365)

(defn- parse-retention-days []
  (let [val (System/getenv "MAYACHOK_RETENTION_DAYS")]
    (if (str/blank? val)
      default-retention-days
      (try
        (Integer/parseInt val)
        (catch Exception _
          default-retention-days)))))

(defn- run-cleanup []
  (let [days (parse-retention-days)]
    (when (pos? days)
      (try
        (let [query-fn (:db.sql/query-fn @system)]
          (when query-fn
            (query-fn :delete-old-screenings! {:days days})
            (log/info {:event :retention/cleanup :days days})))
        (catch Exception e
          (log/error e "retention cleanup failed"))))))

(defn stop-app []
  ((or (:stop defaults) (fn [])))
  (some-> (deref system) (ig/halt!)))

(defn- ensure-cookie-secret []
  (when (or (nil? (System/getenv "COOKIE_SECRET"))
            (= "" (System/getenv "COOKIE_SECRET")))
    (let [generated (str (java.util.UUID/randomUUID)
                         (java.util.UUID/randomUUID))]
      (log/warn "COOKIE_SECRET not set — generating a random secret for this session")
      (log/warn "Set COOKIE_SECRET env var to persist sessions across restarts")
      (System/setProperty "COOKIE_SECRET" generated))))

(defn start-app [& [params]]
  (ensure-cookie-secret)
  ((or (:start params) (:start defaults) (fn [])))
  (->> (config/system-config (or (:opts params) (:opts defaults) {}))
       (ig/expand)
       (ig/init)
       (reset! system))
  (run-cleanup))

(defn -main [& _]
  (start-app)
  (.addShutdownHook (Runtime/getRuntime) (Thread. (fn [] (stop-app) (shutdown-agents)))))
