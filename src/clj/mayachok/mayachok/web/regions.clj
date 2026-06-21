(ns mayachok.mayachok.web.regions)

;; Region data: country code -> list of [code label] pairs.
;; Codes are coarse, not city-level. Designed for heatmap aggregation.

(def regions
  {"ru" [["ru-mow" "Moscow area"]
         ["ru-spb" "Saint Petersburg area"]
         ["ru-nw" "Northwest"]
         ["ru-cent" "Central Russia"]
         ["ru-vol" "Volga region"]
         ["ru-ural" "Ural region"]
         ["ru-sib" "Siberia"]
         ["ru-fe" "Far East"]
         ["ru-south" "Southern Russia"]
         ["ru-cauc" "North Caucasus"]]
   "ua" [["ua-kyiv" "Kyiv area"]
         ["ua-east" "Eastern Ukraine"]
         ["ua-west" "Western Ukraine"]
         ["ua-south" "Southern Ukraine"]
         ["ua-cent" "Central Ukraine"]]
   "de" [["de-bav" "Bavaria"]
         ["de-nrw" "North Rhine-Westphalia"]
         ["de-bb" "Brandenburg / Berlin"]
         ["de-hh" "Hamburg / Schleswig-Holstein"]
         ["de-he" "Hesse"]
         ["de-sn" "Saxony"]
         ["de-bw" "Baden-Württemberg"]
         ["de-other" "Other regions"]]
   "us" [["us-ne" "Northeast"]
         ["us-se" "Southeast"]
         ["us-mw" "Midwest"]
         ["us-sw" "Southwest"]
         ["us-w" "West Coast"]
         ["us-other" "Other"]]})

(def country-names
  {"ru" "Russia"
   "ua" "Ukraine"
   "de" "Germany"
   "us" "United States"})

(defn regions-for [country]
  (get regions country []))

(defn country-name [country]
  (get country-names country country))
