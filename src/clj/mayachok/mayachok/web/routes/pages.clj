(ns mayachok.mayachok.web.routes.pages
  (:require
   [clojure.data.json :as json]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.tools.logging :as log]
   [mayachok.mayachok.domain.epds :as epds]
   [mayachok.mayachok.web.geocode :as geocode]
   [mayachok.mayachok.web.i18n :as i18n]
   [mayachok.mayachok.web.pages.layout :as layout]
   [mayachok.mayachok.web.pdf :as pdf]
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
        answers-json (json/write-str answers)
        tr (i18n/all-strings locale)]
    (layout/render request "question.html"
                   {:locale locale :q-num q-num :question-text (:text qd) :options (:options qd)
                    :answers answers-json :progress (* 10 q-num) :animal (random-animal)
                    :tr tr
                    :question-of-text (format (:question-of tr) q-num)
                    :question-title-text (format (:question-title tr) q-num)})))

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
                     :lat nil :lng nil :location_text nil})
          (catch Exception e (log/error e "failed to save screening")))
        (let [risk   (:risk-level result)
              crisis (get epds/crisis-resources (keyword locale) (epds/crisis-resources :ru))
              tr     (i18n/all-strings locale)]
          (layout/render request "result.html"
                         {:locale locale :total-score (:total-score result) :q10-score (:q10-score result)
                          :risk-level risk :risk-label (risk-label locale risk) :risk-color (risk-color risk)
                          :recommendation (risk-rec locale risk) :crisis crisis
                          :show-crisis (pos? (:q10-score result)) :screening-id screening-id
                          :tr tr})))
      (show-question {:params {:q (str (inc q-num)) :locale locale
                               :answers (json/write-str answers')}}))))

;; -- survey ------------------------------------------------------------------

(defn submit-survey [request]
  (let [p           (:form-params request)
        id          (get-in request [:path-params :id])
        age-range   (get p "age_range")
        time-since  (get p "time_since_birth")
        first-child (get p "first_child")
        location    (get p "location")
        query-fn    (utils/route-data-key request :query-fn)]
    (when query-fn
      (try
        (query-fn :update-survey! {:id id :age_range age-range :time_since_birth time-since :first_child (or first-child "")})
        (catch Exception e
          (log/error e "failed to update survey")))
      (when (and location (not= "" (.trim location)))
        (try
          (if-let [coords (geocode/geocode location)]
            (query-fn :update-location! {:id id, :lat (:lat coords), :lng (:lng coords), :location_text (:display coords)})
            (log/warn "geocoding returned nil for location:" location))
          (catch Exception e
            (log/error e "failed to geocode location")))))
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
                        (assoc row
                               :risk_color (avg-score-color (:avg_score row))))
                      raw)
        locale   (or (get-in request [:params :locale]) "en")]
    (layout/render request "heatmap.html"
                   {:locale locale
                    :tr (i18n/all-strings locale)
                    :data data
                    :data-json (json/write-str data)})))

;; -- help resources ----------------------------------------------------------

(def ^:private help-resources
  (edn/read-string (slurp (clojure.java.io/resource "help-resources.edn"))))

(defn- get-help-resources [locale]
  (let [locale-key (keyword locale)]
    (get-in help-resources [:resources locale-key]
            (get-in help-resources [:resources :en]))))

(defn show-help [request]
  (let [locale       (or (get-in request [:params :locale])
                         (locale-from request)
                         "ru")
        screening-id (get-in request [:params :screening_id])
        query-fn     (utils/route-data-key request :query-fn)
        tr           (i18n/all-strings locale)
        res          (get-help-resources locale)
        qs           (get epds/questions (keyword locale) (epds/questions :ru))

        _ (log/info "Help page: locale=" locale "params=" (:params request) "all-keys=" (keys (:params request)))

        ai-prompt
        (let [template (get-in help-resources [:resources :_global :ai-prompt
                                                (keyword (str "template-" locale))]
                               (get-in help-resources [:resources :_global :ai-prompt :template-en]))]
          (if-let [s (when (and screening-id query-fn)
                       (let [result (query-fn :get-screening-by-id {:id screening-id})]
                         (log/info "Screening lookup result:" (boolean result))
                         result))]
            (let [score      (:total_score s)
                  answers    (pdf/parse-answers (:answers s))
                  answer-str (when (seq answers)
                               (str/join "; "
                                 (map (fn [{:keys [question answer]}]
                                        (let [qd   (get qs question)
                                              text (:text qd)
                                              opts (:options qd)
                                              label (if (and opts (<= 0 answer (dec (count opts))))
                                                      (get opts answer)
                                                      (str answer))]
                                          (str "Q" question ": " text " → " label)))
                                      answers)))]
              (log/info "AI prompt: score=" score "answers=" (count answers))
              (-> template
                  (str/replace "{{score}}" (str score))
                  (str/replace "{{answers}}" (or answer-str ""))))
            (do (log/info "No screening data, using template as-is")
                (-> template
                    (str/replace "{{score}}" "[score]")
                    (str/replace "{{answers}}" "[your answers]")))))]

    (log/info "Showing help page for locale:" locale)
    (layout/render request "help.html"
      {:locale locale
       :tr tr
       :crisis (:crisis res)
       :postpartum (:postpartum res)
       :chat (:chat res)
       :telegram (:telegram res)
       :global (get-in help-resources [:resources :_global :directories])
       :ai-prompt ai-prompt
       :last-reviewed (:last-reviewed help-resources "06-2026")})))

;; -- pdf ---------------------------------------------------------------------

(defn download-pdf [request]
  (let [query-fn     (utils/route-data-key request :query-fn)
        screening-id (get-in request [:path-params :id])]
    (if-let [s (query-fn :get-screening-by-id {:id screening-id})]
      (let [locale (or (:locale s) "ru")
            tr     (i18n/all-strings locale)]
        (log/info "Generating PDF for screening" screening-id)
        (let [pdf-bytes (pdf/result-pdf s tr)]
          {:status  200
           :headers {"Content-Type"        "application/pdf"
                     "Content-Disposition" (str "attachment; filename=\"mayachok-" screening-id ".pdf\"")}
           :body    pdf-bytes}))
      (layout/render request "error.html" {:status 404 :title "Screening not found"}))))

;; -- routes -----------------------------------------------------------------

(defn page-routes [_opts]
  [["/" {:get home}]
   ["/screening" {:get show-question}]
   ["/screening/answer" {:post submit-answer}]
   ["/screenings/:id/pdf" {:get download-pdf}]
   ["/help" {:get show-help}]
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
