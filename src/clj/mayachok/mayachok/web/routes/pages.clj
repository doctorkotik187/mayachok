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
                :probable-depression "Wahrscheinliche Depression" :self-harm-risk "Selbstgefährdung"}}
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
                :self-harm-risk "Bitte suchen Sie sofort Hilfe. Sie sind nicht allein — Hilfe ist jetzt verfügbar."}}
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
            {:id screening-id :created_at now :locale locale :mode "self"
             :answers (json/write-str answers') :total_score (:total-score result)
             :q10_score (:q10-score result) :risk_level (name (:risk-level result))
             :age_range nil :time_since_birth nil :first_child nil
             :clinic_id nil :patient_ref nil})
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
                  :survey-birth-6m+ (tr locale :survey-birth-6m+)}})))
      (show-question {:params {:q (str (inc q-num)) :locale locale
                               :answers (json/write-str answers')}}))))

;; -- routes -----------------------------------------------------------------

(defn page-routes [_opts]
  [["/" {:get home}]
   ["/screening" {:get show-question}]
   ["/screening/answer" {:post submit-answer}]])

(defn- build-route-data [opts]
  (merge {:middleware [(wrap-page-defaults) parameters/parameters-middleware
                       muuntaja/format-response-middleware muuntaja/format-negotiate-middleware]}
          (select-keys opts [:query-fn])))

(derive :reitit.routes/pages :reitit/routes)

(defmethod ig/init-key :reitit.routes/pages
  [_ {:keys [base-path] :or {base-path ""} :as opts}]
  (layout/init-selmer! opts)
  (fn [] [base-path (build-route-data opts) (page-routes opts)]))
