(ns mayachok.mayachok.web.routes.pages
  (:require
    [clojure.data.json :as json]
    [clojure.tools.logging :as log]
    [mayachok.mayachok.domain.epds :as epds]
    [mayachok.mayachok.web.pages.layout :as layout]
    [mayachok.mayachok.web.routes.utils :as utils]
    [integrant.core :as ig]
    [reitit.ring.middleware.muuntaja :as muuntaja]
    [reitit.ring.middleware.parameters :as parameters]
    [ring.middleware.anti-forgery :refer [wrap-anti-forgery]])
  (:import
    [java.util UUID]))

(defn- wrap-page-defaults []
  (let [error-page (layout/error-page
                     {:status 403
                      :title "Invalid anti-forgery token"})]
    #(wrap-anti-forgery % {:error-response error-page})))

(defn- questions-for [locale]
  (get epds/questions (keyword locale) (epds/questions :ru)))

(defn- risk-label [locale risk-level]
  (get-in {:ru {:low-risk "Низкий риск"
                :possible-depression "Возможная депрессия"
                :probable-depression "Вероятная депрессия"
                :self-harm-risk "Риск самоповреждения"}
           :en {:low-risk "Low risk"
                :possible-depression "Possible depression"
                :probable-depression "Probable depression"
                :self-harm-risk "Self-harm risk"}}
          [(keyword locale) risk-level]
          (name risk-level)))

(defn- risk-color [risk-level]
  (case risk-level
    :low-risk "#4ade80"
    :possible-depression "#fbbf24"
    :probable-depression "#f97316"
    :self-harm-risk "#ef4444"
    "#94a3b8"))

(defn- risk-rec [locale risk-level]
  (get-in {:ru {:low-risk "Результат в пределах нормы. Продолжайте заботиться о себе и своём малыше."
                :possible-depression "Рекомендуется наблюдение. Обратитесь к специалисту в течение двух недель."
                :probable-depression "Рекомендуется консультация психиатра или психотерапевта."
                :self-harm-risk "Пожалуйста, немедленно обратитесь за помощью. Вы не одни — помощь доступна прямо сейчас."}
           :en {:low-risk "Result is within normal range. Continue taking care of yourself and your baby."
                :possible-depression "Follow-up recommended. Please see a specialist within two weeks."
                :probable-depression "Consultation with a psychiatrist or therapist is recommended soon."
                :self-harm-risk "Please seek help immediately. You are not alone — help is available right now."}}
          [(keyword locale) risk-level]
          ""))

;; -- landing page -----------------------------------------------------------

(defn home [request]
  (layout/render request "home.html" {:locale "ru"}))

;; -- question page ----------------------------------------------------------

(defn show-question [request]
  (let [p      (:params request)
        locale (or (:locale p) "ru")
        q-num  (Integer/parseInt (or (:q p) "1"))
        answers (if (:answers p)
                  (json/read-str (:answers p) :key-fn keyword)
                  [])
        qs     (questions-for locale)
        qd     (get qs q-num)
        answers-json (json/write-str answers)]
    (layout/render request "question.html"
      {:locale locale
       :q-num q-num
       :question-text (:text qd)
       :options (:options qd)
       :answers answers-json
       :progress (* 10 q-num)})))

;; -- process answer, advance or show result ---------------------------------

(defn submit-answer [request]
  (let [p      (:form-params request)
        locale (or (get p "locale") "ru")
        q-num  (Integer/parseInt (get p "q"))
        idx    (Integer/parseInt (get p "answer"))
        answers (json/read-str (get p "answers" "[]") :key-fn keyword)
        answers' (conj (vec answers) {:question q-num :answer idx})]
    (if (= q-num 10)
      ;; All done — compute score, persist, show result
      (let [result (epds/score-screening answers')
            query-fn (utils/route-data-key request :query-fn)
            screening-id (str (UUID/randomUUID))
            now (str (java.time.Instant/now))]
        (try
          (query-fn :create-screening!
            {:id screening-id
             :created_at now
             :locale locale
             :mode "self"
             :answers (json/write-str answers')
             :total_score (:total-score result)
             :q10_score (:q10-score result)
             :risk_level (name (:risk-level result))
             :clinic_id nil
             :patient_ref nil})
          (catch Exception e
            (log/error e "failed to save screening")))
        (let [risk (:risk-level result)
              crisis (get epds/crisis-resources (keyword locale) (epds/crisis-resources :ru))]
          (layout/render request "result.html"
            {:locale locale
             :total-score (:total-score result)
             :q10-score (:q10-score result)
             :risk-level risk
             :risk-label (risk-label locale risk)
             :risk-color (risk-color risk)
             :recommendation (risk-rec locale risk)
             :crisis crisis
             :show-crisis (pos? (:q10-score result))})))
      ;; More questions
      (show-question {:params {:q (str (inc q-num))
                               :locale locale
                               :answers (json/write-str answers')}}))))

;; -- routes -----------------------------------------------------------------

(defn page-routes [_opts]
  [["/"                        {:get home}]
   ["/screening"               {:get show-question}]
   ["/screening/answer"        {:post submit-answer}]])

(def route-data
  {:middleware
   [(wrap-page-defaults)
    parameters/parameters-middleware
    muuntaja/format-response-middleware
    muuntaja/format-negotiate-middleware]})

(derive :reitit.routes/pages :reitit/routes)

(defmethod ig/init-key :reitit.routes/pages
  [_ {:keys [base-path]
      :or   {base-path ""}
      :as   opts}]
  (layout/init-selmer! opts)
  (fn [] [base-path route-data (page-routes opts)]))
