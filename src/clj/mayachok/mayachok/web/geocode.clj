(ns mayachok.mayachok.web.geocode
  (:require
   [clojure.data.json :as json]
   [clojure.tools.logging :as log])
  (:import
   [java.net URI]
   [java.net.http HttpClient HttpRequest HttpResponse$BodyHandlers]
   [java.time Duration]))

(def ^:private nominatim-base "https://nominatim.openstreetmap.org/search")
(def ^:private user-agent "mayachok/1.0 (https://github.com/doctorkotik187/mayachok)")

(defn- parse-result [matches]
  (when (seq matches)
    (let [best (first matches)]
      {:lat (Double/parseDouble (:lat best))
       :lng (Double/parseDouble (:lon best))
       :display (:display_name best)})))

(defn geocode
  "Geocode a location string using Nominatim (OpenStreetMap).
   Returns {:lat 59.93 :lng 30.33 :display 'Saint Petersburg, Russia'} or nil.
   The raw user query is NOT stored — only the normalized Nominatim result."
  [query]
  (when (and query (not= "" (.trim query)))
    (try
      (let [url (str nominatim-base
                     "?q=" (java.net.URLEncoder/encode query "UTF-8")
                     "&format=json&limit=1&addressdetails=1")
            client (-> (HttpClient/newBuilder)
                       (.connectTimeout (Duration/ofSeconds 5))
                       (.build))
            req (-> (HttpRequest/newBuilder)
                    (.uri (URI/create url))
                    (.header "User-Agent" user-agent)
                    (.GET)
                    (.build))
            resp (.send client req (HttpResponse$BodyHandlers/ofString))
            body (.body resp)
            matches (json/read-str body :key-fn keyword)]
        (parse-result matches))
      (catch Exception e
        (log/error e "geocoding failed for query:" query)
        nil))))