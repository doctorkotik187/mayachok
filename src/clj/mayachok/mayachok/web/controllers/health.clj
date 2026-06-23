(ns mayachok.mayachok.web.controllers.health
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [ring.util.http-response :as http-response])
  (:import
   [java.util Date]))

(defn- read-version []
  (try
    (some-> (io/resource "VERSION") slurp str/trim)
    (catch Exception _
      "unknown")))

(defn healthcheck!
  [_req]
  (http-response/ok
   {:time     (str (Date. (System/currentTimeMillis)))
    :up-since (str (Date. (.getStartTime (java.lang.management.ManagementFactory/getRuntimeMXBean))))
    :version  (read-version)
    :app      {:status  "up"
               :message ""}}))
