(ns mayachok.mayachok.domain.epds)

;; -- Scoring constants -------------------------------------------------------

(def ^:private threshold-possible-depression 10)
(def ^:private threshold-probable-depression 13)

(def ^:private reverse-scored-questions #{1 2 4})

;; -- EPDS Questions (validated translations) ---------------------------------
;; All options ordered from least severe (index 0, score 0) to most severe (index 3, score 3).
;; Reverse-scored questions (1, 2, 4) invert the index: score = 3 - index.

(def questions
  {:ru
   {1  {:text "Я могла смеяться и видеть смешную сторону происходящего"
        :options ["Совсем не могла" "Значительно меньше, чем раньше" "Не настолько, как раньше" "Как всегда"]}
    2  {:text "Я смотрела в будущее с удовольствием"
        :options ["Совсем не смотрела" "Значительно меньше, чем раньше" "Несколько меньше, чем раньше" "Как всегда"]}
    3  {:text "Когда что-то шло не так, я обвиняла себя без достаточных оснований"
        :options ["Нет, никогда" "Не очень часто" "Да, иногда" "Да, большую часть времени"]}
    4  {:text "Я чувствовала беспокойство и волнение без видимой причины"
        :options ["Да, очень часто" "Да, иногда" "Почти никогда" "Совсем нет"]}
    5  {:text "Я чувствовала страх или панику без видимой причины"
        :options ["Совсем нет" "Не очень часто" "Да, иногда" "Да, довольно часто"]}
    6  {:text "Всё навалилось на меня"
        :options ["Нет, я справлялась так же, как всегда" "Нет, большую часть времени я справлялась хорошо" "Да, иногда я не справлялась" "Да, большую часть времени я не справлялась"]}
    7  {:text "Мне было так плохо, что я плохо спала"
        :options ["Совсем нет" "Не очень часто" "Да, иногда" "Да, большую часть времени"]}
    8  {:text "Мне было грустно и я чувствовала себя несчастной"
        :options ["Совсем нет" "Не очень часто" "Да, довольно часто" "Да, большую часть времени"]}
    9  {:text "Я была настолько несчастна, что плакала"
        :options ["Нет, никогда" "Только иногда" "Да, довольно часто" "Да, большую часть времени"]}
    10 {:text "Мне приходили мысли о причинении себе вреда"
        :options ["Никогда" "Почти никогда" "Иногда" "Да, довольно часто"]}}

   :en
   {1  {:text "I have been able to laugh and see the funny side of things"
        :options ["Not at all" "Definitely not so much now" "Not quite so much now" "As much as I always could"]}
    2  {:text "I have looked forward with enjoyment to things"
        :options ["Hardly at all" "Definitely less than I used to" "Rather less than I used to" "As much as I ever did"]}
    3  {:text "I have blamed myself unnecessarily when things went wrong"
        :options ["No, never" "Not very often" "Yes, some of the time" "Yes, most of the time"]}
    4  {:text "I have been anxious or worried for no good reason"
        :options ["No, not at all" "Hardly ever" "Yes, sometimes" "Yes, very often"]}
    5  {:text "I have felt scared or panicky for no very good reason"
        :options ["No, not at all" "No, not much" "Yes, sometimes" "Yes, quite a lot"]}
    6  {:text "Things have been getting on top of me"
        :options ["No, I have been coping as well as ever" "No, most of the time I have coped quite well" "Yes, sometimes I haven't been coping as well as usual" "Yes, most of the time I haven't been able to cope at all"]}
    7  {:text "I have been so unhappy that I have had difficulty sleeping"
        :options ["No, not at all" "Not very often" "Yes, sometimes" "Yes, most of the time"]}
    8  {:text "I have felt sad or miserable"
        :options ["No, not at all" "Not very often" "Yes, quite often" "Yes, most of the time"]}
    9  {:text "I have been so unhappy that I have been crying"
        :options ["No, never" "Only occasionally" "Yes, quite often" "Yes, most of the time"]}
    10 {:text "The thought of harming myself has occurred to me"
        :options ["Never" "Hardly ever" "Sometimes" "Yes, quite often"]}}

   :de
   {1  {:text "Ich konnte lachen und die lustigen Seiten des Lebens sehen"
        :options ["Überhaupt nicht" "Deutlich weniger" "Nicht mehr so sehr" "So wie immer"]}
    2  {:text "Ich habe mit Freude an die Zukunft gedacht"
        :options ["Kaum noch" "Deutlich weniger als früher" "Weniger als früher" "So wie immer"]}
    3  {:text "Ich habe mir unnötig Vorwürfe gemacht, wenn etwas schiefgelaufen ist"
        :options ["Nein, nie" "Nicht sehr oft" "Ja, manchmal" "Ja, die meiste Zeit"]}
    4  {:text "Ich war ohne Grund ängstlich oder besorgt"
        :options ["Ja, sehr oft" "Ja, manchmal" "Kaum je" "Nein, überhaupt nicht"]}
    5  {:text "Ich habe mich ohne Grund gefürchtet oder panisch gefühlt"
        :options ["Nein, überhaupt nicht" "Nicht sehr oft" "Ja, manchmal" "Ja, ziemlich oft"]}
    6  {:text "Alles hat sich aufgestaut"
        :options ["Nein, ich habe wie immer gut zurechtkommen" "Nein, die meiste Zeit habe ich gut zurechtkommen" "Ja, manchmal konnte ich nicht so gut zurechtkommen wie sonst" "Ja, die meiste Zeit konnte ich nicht zurechtkommen"]}
    7  {:text "Ich war so unglücklich, dass ich Schlafprobleme hatte"
        :options ["Nein, überhaupt nicht" "Nicht sehr oft" "Ja, manchmal" "Ja, die meiste Zeit"]}
    8  {:text "Ich habe mich traurig und elend gefühlt"
        :options ["Nein, überhaupt nicht" "Nicht sehr oft" "Ja, ziemlich oft" "Ja, die meiste Zeit"]}
    9  {:text "Ich war so unglücklich, dass ich geweint habe"
        :options ["Nein, nie" "Nur gelegentlich" "Ja, ziemlich oft" "Ja, die meiste Zeit"]}
    10 {:text "Mir ist es gekommen, mir selbst Schaden zuzufügen"
        :options ["Nie" "Kaum je" "Manchmal" "Ja, ziemlich oft"]}}})

;; -- Crisis resources (hardcoded, never fetched from API) --------------------

(def crisis-resources
  {:ru {:phone "8-800-2000-122"
        :label "Телефон доверия (бесплатно)"
        :spb   "004 — Экстренная психологическая помощь, Санкт-Петербург"}
   :en {:phone "988"
        :label "Suicide & Crisis Lifeline (US)"}
   :de {:phone "0800-1110111"
        :label "Telefonseelsorge (kostenlos)"}})

;; -- Scoring functions (pure, no I/O) ---------------------------------------
;; All options ordered least severe (index 0) to most severe (index 3).
;; Q1, Q2, Q4 are reverse-scored: score = 3 - index.
;; All other questions: score = index.

(defn score-answer [q-idx answer-idx]
  (if (contains? reverse-scored-questions q-idx)
    (- 3 answer-idx)
    answer-idx))

(defn total-score [answers]
  (->> answers
       (map (fn [{:keys [question answer]}]
              (score-answer question answer)))
       (reduce +)))

(defn q10-score [answers]
  (->> answers
       (filter #(= 10 (:question %)))
       first
       :answer
       (score-answer 10)))

(defn risk-level
  "Determines clinical risk level from total score and Q10 score.
   Q10 > 0 ALWAYS returns :self-harm-risk regardless of total."
  [total q10]
  (cond
    (pos? q10)       :self-harm-risk
    (>= total threshold-probable-depression) :probable-depression
    (>= total threshold-possible-depression) :possible-depression
    :else            :low-risk))

(defn score-screening
  "Takes a vector of answers, returns a map with total-score, q10-score, and risk-level."
  [answers]
  (let [total (total-score answers)
        q10   (q10-score answers)]
    {:total-score total
     :q10-score   q10
     :risk-level  (risk-level total q10)}))
