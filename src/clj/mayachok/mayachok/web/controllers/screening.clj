(ns mayachok.mayachok.web.controllers.screening
  (:require
   [clojure.data.json :as json]
   [clojure.tools.logging :as log]
   [mayachok.mayachok.domain.epds :as epds]
   [mayachok.mayachok.web.routes.utils :as utils]
   [ring.util.http-response :as http-response])
  (:import
   [java.util UUID]))

(defn- validate-answers [answers]
  (and (vector? answers)
       (= 10 (count answers))
       (every? (fn [{:keys [question answer]}]
                 (and (int? question) (<= 1 question 10)
                      (int? answer) (<= 0 answer 3)))
               answers)
       (= (set (map :question answers))
          (set (range 1 11)))))

(defn create-screening!
  [request]
  (let [query-fn   (utils/route-data-key request :query-fn)
        body       (:body request)
        answers    (:answers body)
        locale     (or (:locale body) "ru")]
    (if-not (validate-answers answers)
      (http-response/bad-request
       {:error "Invalid answers. Expected a vector of 10 maps with :question (1-10) and :answer (0-3)."})
      (try
        (let [score-result (epds/score-screening answers)
              screening-id (str (UUID/randomUUID))
              now          (str (java.time.Instant/now))]
          (query-fn :create-screening!
                    {:id          screening-id
                     :created_at  now
                     :locale      locale
                     :answers     (json/write-str answers)
                     :total_score (:total-score score-result)
                     :q10_score   (:q10-score score-result)
                     :risk_level  (name (:risk-level score-result))})
          (http-response/ok
           (assoc score-result :id screening-id)))
        (catch Exception e
          (log/error e "failed to create screening")
          (http-response/internal-server-error
           {:error "Failed to create screening"}))))))

(defn get-screening
  [request]
  (let [query-fn   (utils/route-data-key request :query-fn)
        id        (get-in request [:path-params :id])]
    (if-let [screening (query-fn :get-screening-by-id {:id id})]
      (http-response/ok screening)
      (http-response/not-found {:error "Screening not found"}))))

(defn update-survey!
  [request]
  (let [query-fn   (utils/route-data-key request :query-fn)
        id        (get-in request [:path-params :id])
        form      (:form-params request)
        age-range  (get form "age_range")
        time-since (get form "time_since_birth")
        first-child (get form "first_child")]
    (try
      (query-fn :update-survey! {:id id :age_range age-range :time_since_birth time-since :first_child first-child})
      (http-response/ok {:ok true})
      (catch Exception e
        (log/error e "failed to update survey")
        (http-response/internal-server-error {:error "Failed to update survey"})))))
