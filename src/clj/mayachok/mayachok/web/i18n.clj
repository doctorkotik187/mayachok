(ns mayachok.mayachok.web.i18n)

;; -- Translation map --------------------------------------------------------
;; All user-facing strings organized by locale.
;; To add a new language, add a new top-level key (:de, :fr, etc.) with the
;; same structure as :ru and :en.

(def translations
  {:ru
   {:app-name "Маячок"
    :app-title "Маячок — Эдинбургская шкала послеродовой депрессии"
    :tagline "Короткий опрос о вашем самочувствии после родов"
    :description "Это стандартная анкета (Эдинбургская шкала послеродовой депрессии), которую используют врачи по всему миру. Она помогает понять, нужна ли вам дополнительная поддержка."
    :start-button "Начать опрос →"
    :question-of "Вопрос %s из 10"
    :question-title "Вопрос %s — Маячок"
    :next-button "Далее →"
    :finish-button "Завершить"
    :your-result "Ваш результат"
    :result-title "Ваш результат — Маячок"
    :out-of "/30"
    :retake "Пройти заново"
    :crisis-title "🆘 Пожалуйста, позвоните сейчас"
    :error-title "Ошибка"
    :error-invalid-token "Недействительный токен защиты"
    :lang-switch "English"}

   :en
   {:app-name "Mayachok"
    :app-title "Mayachok — Edinburgh Postnatal Depression Scale"
    :tagline "A brief questionnaire about how you've been feeling after childbirth"
    :description "This is a standard questionnaire (Edinburgh Postnatal Depression Scale) used by clinicians worldwide. It helps determine whether you might benefit from additional support."
    :start-button "Start questionnaire →"
    :question-of "Question %s of 10"
    :question-title "Question %s — Mayachok"
    :next-button "Next →"
    :finish-button "Finish"
    :your-result "Your result"
    :result-title "Your result — Mayachok"
    :out-of "/30"
    :retake "Take again"
    :crisis-title "🆘 Please call now"
    :error-title "Error"
    :error-invalid-token "Invalid anti-forgery token"
    :lang-switch "Русский"}})

;; -- Lookup -----------------------------------------------------------------

(defn t
  "Look up a translation key for the given locale.
   Falls back to :en if locale is missing, then to the key name itself."
  [locale k]
  (let [locale (keyword locale)]
    (get-in translations [locale k]
      (get-in translations [:en k] (name k)))))

(defn all-strings
  "Return all translation strings for a locale as a flat map.
   Useful for passing to Selmer templates."
  [locale]
  (let [locale (keyword locale)
        base (get translations locale {})
        en   (get translations :en {})]
    (merge en base)))
