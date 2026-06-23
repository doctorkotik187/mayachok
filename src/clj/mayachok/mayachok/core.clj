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
            (log/info "retention cleanup: deleted screenings older than" days "days")))
        (catch Exception e
          (log/error e "retention cleanup failed"))))))

(defn stop-app []
  ((or (:stop defaults) (fn [])))
  (some-> (deref system) (ig/halt!)))

(defn start-app [& [params]]
  ((or (:start params) (:start defaults) (fn [])))
  (->> (config/system-config (or (:opts params) (:opts defaults) {}))
       (ig/expand)
       (ig/init)
       (reset! system))
  (run-cleanup))

(defn -main [& _]
  (start-app)
  (.addShutdownHook (Runtime/getRuntime) (Thread. (fn [] (stop-app) (shutdown-agents)))))
