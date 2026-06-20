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
        locale     (or (:locale body) "ru")
        mode       (or (:mode body) "clinician")
        clinic-id  (:clinic_id body)
        patient-ref (:patient_ref body)]
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
             :mode        mode
             :answers     (json/write-str answers)
             :total_score (:total-score score-result)
             :q10_score   (:q10-score score-result)
             :risk_level  (name (:risk-level score-result))
             :clinic_id   clinic-id
             :patient_ref patient-ref})
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
