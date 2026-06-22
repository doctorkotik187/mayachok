(ns mayachok.mayachok.web.pdf
  (:require
   [clj-pdf.core :as pdf]
   [clojure.data.json :as json]
   [clojure.java.io :as io]
   [mayachok.mayachok.domain.epds :as epds])
  (:import [java.io ByteArrayOutputStream]
           [java.time Instant ZoneId]
           [java.time.format DateTimeFormatter]))

;; -- Fonts & Colors -----------------------------------------------------------

(def ^:private font-regular "fonts/NotoSans-Regular.ttf")
(def ^:private font-bold    "fonts/NotoSans-Bold.ttf")

(def ^:private bg-page   [255 248 240])
(def ^:private accent    [232 121 249])
(def ^:private text-dark [60 50 45])
(def ^:private text-mid  [120 110 100])
(def ^:private text-light [160 150 140])
(def ^:private line-color [235 225 215])

(defn- risk-color-rgb [risk-level]
  (case risk-level
    :low-risk             [74 222 128]
    :possible-depression  [251 191 36]
    :probable-depression  [249 115 22]
    :self-harm-risk       [239 68 68]
    [148 163 184]))

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

(defn- format-timestamp [ts]
  (try
    (let [instant (Instant/parse ts)
          formatter (-> (DateTimeFormatter/ofPattern "d MMMM yyyy")
                        (.withZone (ZoneId/of "UTC")))]
      (.format formatter instant))
    (catch Exception _
      (if (and ts (> (count ts) 10))
        (subs ts 0 10)
        (str ts)))))

(defn- parse-answers [answers-field]
  (when answers-field
    (try
      (let [parsed (if (string? answers-field)
                     (json/read-str answers-field :key-fn keyword)
                     answers-field)
            data   (if (sequential? parsed) parsed [])]
        (mapv (fn [item]
                {:question (or (:q item) (:question item))
                 :answer   (or (:a item) (:answer item))})
              data))
      (catch Exception _ []))))

(defn- build-bg-graphics []
  [:graphics {:under true}
   (fn [g2d]
     (.setColor g2d (java.awt.Color. ^int (nth bg-page 0)
                                     ^int (nth bg-page 1)
                                     ^int (nth bg-page 2)))
     (.fillRect g2d 0 0 595 842))])

(defn- build-header [app-name mascot-img]
  [:pdf-table {:width-percent 100
               :spacing-after 4
               :cell-border false}
   [70 30]
   [[:pdf-cell {:align :right :valign :middle}
     [:paragraph {:align :right}
      [:chunk {:size 20 :style :bold :ttf-name font-bold :color accent}
       app-name]]]
    (when mascot-img
      [:pdf-cell {:align :right :valign :middle}
       [:image {:width 42 :height 42} mascot-img]])]])

(defn- build-score-risk [total-score risk-rgb risk-lbl rec]
  [:pdf-table {:width-percent 100
               :spacing-after 6
               :cell-border false}
   [35 65]
   [[:pdf-cell {:align :center}
     [:paragraph {:align :center}
      [:chunk {:size 38 :style :bold :ttf-name font-bold :color risk-rgb}
       (str total-score)]
      [:chunk {:size 13 :color text-mid} " / 30"]]]
    [:pdf-cell {:align :center}
     [:paragraph {:align :center :size 13 :style :bold :ttf-name font-bold :color risk-rgb}
      risk-lbl]
     [:paragraph {:align :center :size 8 :color text-dark :leading 12}
      rec]]]])

(defn- build-crisis [crisis-title label phone spb]
  [:pdf-table {:width-percent 100
               :spacing-after 6
               :cell-border false}
   [100]
   [[:pdf-cell {:background-color [254 226 226]
                :padding 8
                :set-border [:left]
                :border-width-left 3
                :border-color [239 68 68]}
     [:paragraph {:size 9 :style :bold :ttf-name font-bold :color [185 28 28]}
      crisis-title]
     [:paragraph {:size 8 :color [127 29 29]}
      [:chunk {:style :bold :ttf-name font-bold} label]
      (str " — " phone)]
     (when spb
       [:paragraph {:size 7 :color [153 27 27]}
        spb])]]])

(defn- build-survey [tr age-range time-since first-child location]
  (when (or age-range time-since first-child location)
    (vec
     (cons
      [:paragraph {:size 8 :style :bold :ttf-name font-bold :color text-dark
                   :spacing-after 3}
       (:survey-title tr)]
      (keep identity
            (cond-> []
              age-range
              (conj [:paragraph {:size 7 :color text-dark :spacing-after 1}
                     [:chunk {:style :bold :ttf-name font-bold :color text-mid}
                      (str (:survey-age tr) ": ")]
                     age-range])

              time-since
              (conj [:paragraph {:size 7 :color text-dark :spacing-after 1}
                     [:chunk {:style :bold :ttf-name font-bold :color text-mid}
                      (str (:survey-birth tr) ": ")]
                     time-since])

              first-child
              (conj [:paragraph {:size 7 :color text-dark :spacing-after 1}
                     [:chunk {:style :bold :ttf-name font-bold :color text-mid}
                      (str (:survey-first tr) ": ")]
                     (if (= first-child "yes") (:survey-yes tr) (:survey-no tr))])

              location
              (conj [:paragraph {:size 7 :color text-dark :spacing-after 1}
                     [:chunk {:style :bold :ttf-name font-bold :color text-mid}
                      (str (:location-question tr) ": ")]
                     location])))))))

(defn- build-answers-table [answers-detail risk-rgb _locale]
  (when (seq answers-detail)
    (into
     [:pdf-table {:width-percent 100
                  :cell-border false
                  :spacing-after 2}
      [6 60 34]]
     (mapcat
      (fn [{:keys [q text label]}]
        [[[:pdf-cell {:padding-top 1}
           [:paragraph {:size 7 :style :bold :ttf-name font-bold :color text-mid}
            (str "Q" q)]]
          [:pdf-cell {:padding-top 1}
           [:paragraph {:size 7 :color text-dark}
            [:chunk {:style :italic} text]]]
          [:pdf-cell {:padding-top 1}
           [:paragraph {:size 7 :color risk-rgb}
            [:chunk {:style :bold :ttf-name font-bold} label]]]]])
      answers-detail))))

;; -- Main entry point --------------------------------------------------------

(defn result-pdf
  "Generates a warm, friendly single-page PDF byte array for the EPDS screening result.
   s - screening record from the database
   tr - translation map from i18n/all-strings"
  [s tr]
  (let [locale      (or (:locale s) "ru")
        risk-level  (keyword (:risk_level s))
        total-score (:total_score s)
        q10-score   (:q10_score s)
        risk-lbl    (risk-label locale risk-level)
        risk-rgb    (risk-color-rgb risk-level)
        rec         (risk-rec locale risk-level)
        crisis      (get epds/crisis-resources (keyword locale))
        show-crisis (pos? q10-score)
        answers     (parse-answers (:answers s))
        timestamp   (format-timestamp (or (:created_at s) (str (java.util.Date.))))
        qs          (get epds/questions (keyword locale) (epds/questions :ru))

        answers-detail
        (when (seq answers)
          (mapv (fn [{:keys [question answer]}]
                  (let [qd   (get qs question)
                        text (or (:text qd) (str "Q" question))
                        opts (:options qd)
                        label (if (and opts (<= 0 answer (dec (count opts))))
                                (get opts answer)
                                (str answer))]
                    {:q     question
                     :text  text
                     :label label}))
                answers))

        age-range   (:age_range s)
        time-since  (:time_since_birth s)
        first-child (:first_child s)
        location    (:location_text s)

        app-name    (:app-name tr)
        doc-title   (str app-name " — " (:your-result tr))
        mascot-img  (io/resource "public/img/pink-sharky.png")

        disclaimer  (case locale
                      "ru" "Этот результат не является диагнозом. Обратитесь к специалисту."
                      "en" "This result is not a diagnosis. Please consult a specialist."
                      "de" "Dieses Ergebnis ist keine Diagnose. Bitte konsultieren Sie einen Spezialisten."
                      "uk" "Цей результат не є діагнозом. Зверніться до спеціаліста."
                      "This result is not a diagnosis. Please consult a specialist.")

        survey-block (build-survey tr age-range time-since first-child location)
        answers-block (build-answers-table answers-detail risk-rgb locale)

        doc
        (vec
         (concat
          [;; metadata
           {:title                  doc-title
            :subject                (:result-title tr)
            :creator                app-name
            :register-system-fonts? true
            :font                   {:ttf-name font-regular
                                     :encoding :unicode}
            :footer                 false
            :top-margin             25
            :bottom-margin          25
            :left-margin            35
            :right-margin           35}

             ;; page background
           (build-bg-graphics)

             ;; header with mascot
           (build-header app-name mascot-img)

             ;; timestamp
           [:paragraph {:align :right :size 7 :color text-light
                        :spacing-after 8}
            timestamp]

             ;; score + risk
           (build-score-risk total-score risk-rgb risk-lbl rec)

             ;; crisis alert
           (when (and show-crisis crisis)
             (build-crisis (:crisis-title tr)
                           (:label crisis)
                           (:phone crisis)
                           (:spb crisis)))

             ;; divider
           [:line {:color line-color :line-width 0.5}]
           [:spacer 4]]

            ;; survey data (vector of elements)
          (or survey-block [])

            ;; answers table (single element or nil)
          (keep identity [answers-block])

            ;; footer
          [[:spacer 6]
           [:line {:color line-color :line-width 0.5}]
           [:spacer 3]
           [:paragraph {:size 6 :color text-light :align :center}
            (:open-source tr)]
           [:paragraph {:size 6 :color text-light :align :center}
            "github.com/doctorkotik187/mayachok"]
           [:paragraph {:size 6 :color [200 195 188] :align :center}
            disclaimer]]))

        out (ByteArrayOutputStream.)]
    (pdf/pdf doc out)
    (.toByteArray out)))
