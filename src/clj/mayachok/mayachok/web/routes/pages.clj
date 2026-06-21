(ns mayachok.mayachok.web.routes.pages
  (:require
    [clojure.data.json :as json]
    [clojure.tools.logging :as log]
    [mayachok.mayachok.domain.epds :as epds]
    [mayachok.mayachok.web.i18n :as i18n]
    [mayachok.mayachok.web.pages.layout :as layout]
    [mayachok.mayachok.web.routes.utils :as utils]
    [integrant.core :as ig]
    [reitit.ring.middleware.muuntaja :as muuntaja]
    [reitit.ring.middleware.parameters :as parameters]
    [ring.middleware.anti-forgery :refer [wrap-anti-forgery]])
  (:import [java.util UUID]))

(defn- wrap-page-defaults []
  (let [error-page (layout/error-page {:status 403 :title "Invalid anti-forgery token"})]
    #(wrap-anti-forgery % {:error-response error-page})))

(defn- questions-for [locale]
  (get epds/questions (keyword locale) (epds/questions :ru)))

(defn- risk-label [locale risk-level]
  (get-in {:ru {:low-risk "Низкий риск" :possible-depression "Возможная депрессия"
                :probable-depression "Вероятная депрессия" :self-harm-risk "Риск самоповреждения"}
           :en {:low-risk "Low risk" :possible-depression "Possible depression"
                :probable-depression "Probable depression" :self-harm-risk "Self-harm risk"}
           :de {:low-risk "Geringes Risiko" :possible-depression "Mögliche Depression"
                :probable-depression "Wahrscheinliche Depression" :self-harm-risk "Selbstgefährdung"}
           :uk {:low-risk "Низький ризик" :possible-depression "Можлива депресія"
                :probable-depression "Ймовірна депресія" :self-harm-risk "Ризик самоушкодження"}}
          [(keyword locale) risk-level] (name risk-level)))

(defn- risk-color [risk-level]
  (case risk-level
    :low-risk "#4ade80" :possible-depression "#fbbf24"
    :probable-depression "#f97316" :self-harm-risk "#ef4444" "#94a3b8"))

(defn- risk-rec [locale risk-level]
  (get-in {:ru {:low-risk "Результат в пределах нормы. Продолжайте заботиться о себе и своём малыше."
                :possible-depression "Рекомендуется наблюдение. Обратитесь к специалисту в течение двух недель."
                :probable-depression "Рекомендуется консультация психиатра или психотерапевта."
                :self-harm-risk "Пожалуйста, немедленно обратитесь за помощью. Вы не одни — помощь доступна прямо сейчас."}
           :en {:low-risk "Result is within normal range. Continue taking care of yourself and your baby."
                :possible-depression "Follow-up recommended. Please see a specialist within two weeks."
                :probable-depression "Consultation with a psychiatrist or therapist is recommended soon."
                :self-harm-risk "Please seek help immediately. You are not alone — help is available right now."}
           :de {:low-risk "Ergebnis im normalen Bereich. Weiter gute Pflege für sich und Ihr Baby."
                :possible-depression "Nachsorge empfohlen. Bitte suchen Sie innerhalb von zwei Wochen einen Spezialisten auf."
                :probable-depression "Konsultation mit einem Psychiater oder Therapeuten wird empfohlen."
                :self-harm-risk "Bitte suchen Sie sofort Hilfe. Sie sind nicht allein — Hilfe ist jetzt verfügbar."}
           :uk {:low-risk "Результат у нормі. Продовжуйте піклуватися про себе та свою дитину."
                :possible-depression "Рекомендоване спостереження. Зверніться до спеціаліста протягом двох тижнів."
                :probable-depression "Рекомендована консультація психіатра або психотерапевта."
                :self-harm-risk "Будь ласка, негайно зверніться за допомогою. Ви не самі — допомога доступна прямо зараз."}}
          [(keyword locale) risk-level] ""))

(def ^:private animals
  ["🐻" "🐰" "🦊" "🐱" "🐶" "🦋" "🐢" "🦉" "🐧" "🐨" "🦁" "🐯" "🐸" "🐙" "🦄" "🐝" "🐞" "🦜" "🐬" "🦩"])

(defn- random-animal []
  (rand-nth animals))

(defn- locale-from [request]
  (or (:locale (:params request)) (:locale (:query-params request)) "ru"))

(defn- tr [locale k & args]
  (let [s (i18n/t locale k)]
    (if args (apply format s args) s)))

;; -- landing page -----------------------------------------------------------

(defn home [request]
  (let [locale (locale-from request)]
    (layout/render request "home.html" {:locale locale :tr (i18n/all-strings locale)})))

;; -- question page ----------------------------------------------------------

(defn show-question [request]
  (let [p (:params request)
        locale (or (:locale p) "ru")
        q-num (Integer/parseInt (or (:q p) "1"))
        answers (if (:answers p) (json/read-str (:answers p) :key-fn keyword) [])
        qs (questions-for locale)
        qd (get qs q-num)
        answers-json (json/write-str answers)]
    (layout/render request "question.html"
      {:locale locale :q-num q-num :question-text (:text qd) :options (:options qd)
       :answers answers-json :progress (* 10 q-num) :animal (random-animal)
       :tr {:app-name (tr locale :app-name) :question-of (tr locale :question-of q-num)
            :question-title (tr locale :question-title q-num) :next-button (tr locale :next-button)
            :finish-button (tr locale :finish-button) :lang-switch (tr locale :lang-switch)}})))

;; -- process answer ----------------------------------------------------------

(defn submit-answer [request]
  (let [p          (:form-params request)
        locale     (or (get p "locale") "ru")
        q-num      (Integer/parseInt (get p "q"))
        idx        (Integer/parseInt (get p "answer"))
        answers    (json/read-str (get p "answers" "[]") :key-fn keyword)
        answers'   (conj (vec answers) {:question q-num :answer idx})]
    (if (= q-num 10)
      (let [result       (epds/score-screening answers')
            query-fn     (utils/route-data-key request :query-fn)
            screening-id (str (UUID/randomUUID))
            now          (str (java.time.Instant/now))]
        (try
          (query-fn :create-screening!
            {:id screening-id :created_at now :locale locale
             :answers (json/write-str answers') :total_score (:total-score result)
             :q10_score (:q10-score result) :risk_level (name (:risk-level result))
             :age_range nil :time_since_birth nil :first_child nil
             :region_code nil})
          (catch Exception e (log/error e "failed to save screening")))
        (let [risk   (:risk-level result)
              crisis (get epds/crisis-resources (keyword locale) (epds/crisis-resources :ru))]
          (layout/render request "result.html"
            {:locale locale :total-score (:total-score result) :q10-score (:q10-score result)
             :risk-level risk :risk-label (risk-label locale risk) :risk-color (risk-color risk)
             :recommendation (risk-rec locale risk) :crisis crisis
             :show-crisis (pos? (:q10-score result)) :screening-id screening-id
             :tr {:app-name (tr locale :app-name) :your-result (tr locale :your-result)
                  :result-title (tr locale :result-title) :out-of (tr locale :out-of)
                  :retake (tr locale :retake) :crisis-title (tr locale :crisis-title)
                  :open-source (tr locale :open-source)
                  :survey-title (tr locale :survey-title) :survey-hint (tr locale :survey-hint)
                  :survey-age (tr locale :survey-age) :survey-birth (tr locale :survey-birth)
                  :survey-first (tr locale :survey-first) :survey-skip (tr locale :survey-skip)
                  :survey-yes (tr locale :survey-yes) :survey-no (tr locale :survey-no)
                  :survey-submit (tr locale :survey-submit)
                  :survey-birth-0-6w (tr locale :survey-birth-0-6w)
                  :survey-birth-6w-3m (tr locale :survey-birth-6w-3m)
                  :survey-birth-3-6m (tr locale :survey-birth-3-6m)
                  :survey-birth-6m+ (tr locale :survey-birth-6m+)
                  :region-question (tr locale :region-question)
                  :region-hint (tr locale :region-hint)
                  :region-select-country (tr locale :region-select-country)
                  :region-select-region (tr locale :region-select-region)
                  :region-submit (tr locale :region-submit)
                  :region-skip (tr locale :region-skip)}})))
      (show-question {:params {:q (str (inc q-num)) :locale locale
                               :answers (json/write-str answers')}}))))

;; -- survey ------------------------------------------------------------------

(defn submit-survey [request]
  (let [p          (:form-params request)
        id         (get-in request [:path-params :id])
        age-range  (get p "age_range")
        time-since (get p "time_since_birth")
        first-child (get p "first_child")
        country    (get p "country_code")
        region     (get p "region_code")
        query-fn   (utils/route-data-key request :query-fn)]
    (when query-fn
      (try
        (query-fn :update-survey! {:id id :age_range age-range :time_since_birth time-since :first_child (or first-child "")})
        (catch Exception e
          (log/error e "failed to update survey")))
      (when (and country region)
        (try
          (query-fn :update-region! {:id id :region_code region})
          (catch Exception e
            (log/error e "failed to update region")))))
    (layout/render request "survey-thankyou.html"
      {:locale (or (get p "locale") "ru")
       :tr (i18n/all-strings (or (get p "locale") "ru"))})))

;; -- region ------------------------------------------------------------------

(defn submit-region [request]
  (let [p          (:form-params request)
        id         (get-in request [:path-params :id])
        country    (get p "country_code")
        region     (get p "region_code")
        query-fn   (utils/route-data-key request :query-fn)]
    (when (and country region)
      (query-fn :update-region! {:id id :region_code region}))
    (layout/render request "region-thankyou.html"
      {:locale (or (get p "locale") "ru")
       :tr (i18n/all-strings (or (get p "locale") "ru"))})))

(def ^:private region-coords
  {"ru-mow" {:lat 55.75 :lng 37.62} "ru-spb" {:lat 59.93 :lng 30.32}
   "ru-nw" {:lat 60.0 :lng 32.0} "ru-cent" {:lat 56.0 :lng 40.0}
   "ru-vol" {:lat 56.0 :lng 44.0} "ru-ural" {:lat 56.8 :lng 60.6}
   "ru-sib" {:lat 56.0 :lng 83.0} "ru-fe" {:lat 48.0 :lng 135.0}
   "ru-south" {:lat 47.0 :lng 40.0} "ru-cauc" {:lat 44.0 :lng 44.0}
   "ua-kyiv" {:lat 50.45 :lng 30.52} "ua-east" {:lat 48.5 :lng 37.0}
   "ua-west" {:lat 49.0 :lng 25.0} "ua-south" {:lat 46.5 :lng 32.0}
   "ua-cent" {:lat 49.0 :lng 31.0}
   "de-bav" {:lat 48.8 :lng 11.5} "de-nrw" {:lat 51.5 :lng 7.5}
   "de-bb" {:lat 52.0 :lng 13.5} "de-hh" {:lat 53.6 :lng 10.0}
   "de-he" {:lat 50.1 :lng 8.7} "de-sn" {:lat 51.0 :lng 13.0}
   "de-bw" {:lat 48.8 :lng 9.2} "de-other" {:lat 51.0 :lng 10.0}
   "us-ne" {:lat 40.7 :lng -74.0} "us-se" {:lat 33.7 :lng -84.4}
   "us-mw" {:lat 41.9 :lng -87.6} "us-sw" {:lat 33.4 :lng -112.1}
   "us-w" {:lat 37.8 :lng -122.4} "us-other" {:lat 39.8 :lng -98.6}})

(defn- avg-score-color [avg]
  (cond
    (>= avg 15) "#ef4444"
    (>= avg 12) "#f97316"
    (>= avg 9)  "#fbbf24"
    :else       "#4ade80"))

(defn show-heatmap [request]
  (let [query-fn (utils/route-data-key request :query-fn)
        raw     (or (query-fn :heatmap-data {}) [])
        data    (mapv (fn [row]
                        (let [code (:region_code row)
                              coords (get region-coords code)]
                          (assoc row
                                 :risk_color (avg-score-color (:avg_score row))
                                 :coords coords)))
                      raw)
        locale   (or (get-in request [:params :locale]) "en")]
    (layout/render request "heatmap.html"
      {:locale locale
       :tr (i18n/all-strings locale)
       :data data
       :data-json (json/write-str data)})))

;; -- routes -----------------------------------------------------------------

(defn page-routes [_opts]
  [["/" {:get home}]
   ["/screening" {:get show-question}]
   ["/screening/answer" {:post submit-answer}]
   ["/screenings/:id/survey" {:post submit-survey}]
   ["/screenings/:id/region" {:post submit-region}]
   ["/map" {:get show-heatmap}]])

(defn- build-route-data [opts]
  (merge {:middleware [(wrap-page-defaults) parameters/parameters-middleware
                       muuntaja/format-response-middleware muuntaja/format-negotiate-middleware]}
          (select-keys opts [:query-fn])))

(derive :reitit.routes/pages :reitit/routes)

(defmethod ig/init-key :reitit.routes/pages
  [_ {:keys [base-path] :or {base-path ""} :as opts}]
  (layout/init-selmer! opts)
  (fn [] [base-path (build-route-data opts) (page-routes opts)]))
