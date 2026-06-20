(ns mayachok.mayachok.domain.epds)

;; -- Scoring constants -------------------------------------------------------

(def ^:private threshold-possible-depression 10)
(def ^:private threshold-probable-depression 13)

(def ^:private reverse-scored-questions #{1 2 4})

;; -- EPDS Questions (validated translations) ---------------------------------

(def questions
  {:ru
   {1  {:text "Я могла смеяться и видеть смешную сторону происходящего"
        :options ["Как всегда" "Не настолько, как раньше" "Значительно меньше, чем раньше" "Совсем не могла"]}
    2  {:text "Я смотрела в будущее с удовольствием"
        :options ["Как всегда" "Несколько меньше, чем раньше" "Значительно меньше, чем раньше" "Совсем не смотрела"]}
    3  {:text "Когда что-то шло не так, я обвиняла себя без достаточных оснований"
        :options ["Да, большую часть времени" "Да, иногда" "Не очень часто" "Нет, никогда"]}
    4  {:text "Я чувствовала беспокойство и волнение без видимой причины"
        :options ["Совсем нет" "Почти никогда" "Да, иногда" "Да, очень часто"]}
    5  {:text "Я чувствовала страх или панику без видимой причины"
        :options ["Да, довольно часто" "Да, иногда" "Не очень часто" "Совсем нет"]}
    6  {:text "Всё навалилось на меня"
        :options ["Да, большую часть времени я не справлялась" "Да, иногда я не справлялась" "Нет, большую часть времени я справлялась хорошо" "Нет, я справлялась так же, как всегда"]}
    7  {:text "Мне было так плохо, что я плохо спала"
        :options ["Да, большую часть времени" "Да, иногда" "Не очень часто" "Совсем нет"]}
    8  {:text "Мне было грустно и я чувствовала себя несчастной"
        :options ["Да, большую часть времени" "Да, довольно часто" "Не очень часто" "Совсем нет"]}
    9  {:text "Я была настолько несчастна, что плакала"
        :options ["Да, большую часть времени" "Да, довольно часто" "Только иногда" "Нет, никогда"]}
    10 {:text "Мне приходили мысли о причинении себе вреда"
        :options ["Да, довольно часто" "Иногда" "Почти никогда" "Никогда"]}}

   :en
   {1  {:text "I have been able to laugh and see the funny side of things"
        :options ["As much as I always could" "Not quite so much now" "Definitely not so much now" "Not at all"]}
    2  {:text "I have looked forward with enjoyment to things"
        :options ["As much as I ever did" "Rather less than I used to" "Definitely less than I used to" "Hardly at all"]}
    3  {:text "I have blamed myself unnecessarily when things went wrong"
        :options ["Yes, most of the time" "Yes, some of the time" "Not very often" "No, never"]}
    4  {:text "I have been anxious or worried for no good reason"
        :options ["No, not at all" "Hardly ever" "Yes, sometimes" "Yes, very often"]}
    5  {:text "I have felt scared or panicky for no very good reason"
        :options ["Yes, quite a lot" "Yes, sometimes" "No, not much" "No, not at all"]}
    6  {:text "Things have been getting on top of me"
        :options ["Yes, most of the time I haven't been able to cope at all" "Yes, sometimes I haven't been coping as well as usual" "No, most of the time I have coped quite well" "No, I have been coping as well as ever"]}
    7  {:text "I have been so unhappy that I have had difficulty sleeping"
        :options ["Yes, most of the time" "Yes, sometimes" "Not very often" "No, not at all"]}
    8  {:text "I have felt sad or miserable"
        :options ["Yes, most of the time" "Yes, quite often" "Not very often" "No, not at all"]}
    9  {:text "I have been so unhappy that I have been crying"
        :options ["Yes, most of the time" "Yes, quite often" "Only occasionally" "No, never"]}
    10 {:text "The thought of harming myself has occurred to me"
        :options ["Yes, quite often" "Sometimes" "Hardly ever" "Never"]}}

   :de
   {1  {:text "Ich konnte lachen und die lustigen Seiten des Lebens sehen"
        :options ["So wie immer" "Nicht mehr so sehr" "Deutlich weniger" "Überhaupt nicht"]}
    2  {:text "Ich habe mit Freude an die Zukunft gedacht"
        :options ["So wie immer" "Weniger als früher" "Deutlich weniger als früher" "Kaum noch"]}
    3  {:text "Ich habe mir unnötig Vorwürfe gemacht, wenn etwas schiefgelaufen ist"
        :options ["Ja, die meiste Zeit" "Ja, manchmal" "Nicht sehr oft" "Nein, nie"]}
    4  {:text "Ich war ohne Grund ängstlich oder besorgt"
        :options ["Nein, überhaupt nicht" "Kaum je" "Ja, manchmal" "Ja, sehr oft"]}
    5  {:text "Ich habe mich ohne Grund gefürchtet oder panisch gefühlt"
        :options ["Ja, ziemlich oft" "Ja, manchmal" "Nicht sehr oft" "Nein, überhaupt nicht"]}
    6  {:text "Alles hat sich aufgestaut"
        :options ["Ja, die meiste Zeit konnte ich nicht zurechtkommen" "Ja, manchmal konnte ich nicht so gut zurechtkommen wie sonst" "Nein, die meiste Zeit habe ich gut zurechtkommen" "Nein, ich habe wie immer gut zurechtkommen"]}
    7  {:text "Ich war so unglücklich, dass ich Schlafprobleme hatte"
        :options ["Ja, die meiste Zeit" "Ja, manchmal" "Nicht sehr oft" "Nein, überhaupt nicht"]}
    8  {:text "Ich habe mich traurig und elend gefühlt"
        :options ["Ja, die meiste Zeit" "Ja, ziemlich oft" "Nicht sehr oft" "Nein, überhaupt nicht"]}
    9  {:text "Ich war so unglücklich, dass ich geweint habe"
        :options ["Ja, die meiste Zeit" "Ja, ziemlich oft" "Nur gelegentlich" "Nein, nie"]}
    10 {:text "Mir ist es gekommen, mir selbst Schaden zuzufügen"
        :options ["Ja, ziemlich oft" "Manchmal" "Kaum je" "Nie"]}}})

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

(defn score-answer
  "Scores a single EPDS answer. Questions 1, 2, 4 are reverse-scored.
   answer-index is 0-3 as provided by the user."
  [question-number answer-index]
  (if (contains? reverse-scored-questions question-number)
    (- 3 answer-index)
    answer-index))

(defn total-score
  "Calculates total EPDS score from a vector of answers.
   Each answer is a map with :question (1-10) and :answer (0-3)."
  [answers]
  (->> answers
       (map (fn [{:keys [question answer]}]
              (score-answer question answer)))
       (reduce +)))

(defn q10-score
  "Extracts the Q10 score from answers vector."
  [answers]
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
