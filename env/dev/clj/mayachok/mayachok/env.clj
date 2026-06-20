(ns mayachok.mayachok.env
  (:require
    [clojure.tools.logging :as log]
    [mayachok.mayachok.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init       (fn []
                 (log/info "\n-=[mayachok starting using the development or test profile]=-"))
   :start      (fn []
                 (log/info "\n-=[mayachok started successfully using the development or test profile]=-"))
   :stop       (fn []
                 (log/info "\n-=[mayachok has shut down successfully]=-"))
   :middleware wrap-dev
   :opts       {:profile       :dev}})
