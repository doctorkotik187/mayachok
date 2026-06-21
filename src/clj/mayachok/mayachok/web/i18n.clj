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
    :lang-switch "English"
    :survey-title "Анонимный опрос"
    :survey-hint "Помогите нам лучше понять мам. Все данные анонимны."
    :survey-age "Ваш возраст"
    :survey-birth "Время с родов"
    :survey-first "Первый ребёнок?"
    :survey-skip "Предпочитаю не отвечать"
    :survey-yes "Да, первый ребёнок"
    :survey-no "Нет, у меня есть другие дети"
    :survey-submit "Отправить (необязательно)"
    :survey-birth-0-6w "0-6 недель"
    :survey-birth-6w-3m "6 недель - 3 месяца"
    :survey-birth-3-6m "3-6 месяцев"
    :survey-birth-6m+ "6+ месяцев"
    :open-source "Это проект с открытым кодом."}

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
    :lang-switch "Русский"
    :survey-title "Anonymous Survey"
    :survey-hint "Help us understand mothers better. All data is anonymous."
    :survey-age "Your age range"
    :survey-birth "Time since birth"
    :survey-first "First child?"
    :survey-skip "Prefer not to say"
    :survey-yes "Yes, first child"
    :survey-no "No, I have other children"
    :survey-submit "Submit (optional)"
    :survey-birth-0-6w "0-6 weeks"
    :survey-birth-6w-3m "6 weeks - 3 months"
    :survey-birth-3-6m "3-6 months"
    :survey-birth-6m+ "6+ months"}

   :de
   {:app-name "Mayachok"
    :app-title "Mayachok — Edinburgh Postnatal Depression Scale"
    :tagline "Ein kurzer Fragebogen über Ihr Befinden nach der Geburt"
    :description "Dies ist ein standardisierter Fragebogen (Edinburgh Postnatal Depression Scale), der weltweit von Ärzten eingesetzt wird. Er hilft zu erkennen, ob Sie zusätzliche Unterstützung benötigen."
    :start-button "Fragebogen starten →"
    :question-of "Frage %s von 10"
    :question-title "Frage %s — Mayachok"
    :next-button "Weiter →"
    :finish-button "Abschließen"
    :your-result "Ihr Ergebnis"
    :result-title "Ihr Ergebnis — Mayachok"
    :out-of "/30"
    :retake "Erneut durchführen"
    :crisis-title "🆘 Bitte rufen Sie jetzt an"
    :error-title "Fehler"
    :error-invalid-token "Ungültiges Anti-Forgery-Token"
    :lang-switch "Deutsch"

    :survey-title "Anonymous Survey"
    :survey-hint "Help us understand mothers better. All data is anonymous."
    :survey-age "Your age range"
    :survey-birth "Time since birth"
    :survey-first "First child?"
    :survey-skip "Prefer not to say"
    :survey-yes "Yes, first child"
    :survey-no "No, I have other children"
    :survey-submit "Submit (optional)"
    :survey-birth-0-6w "0-6 weeks"
    :survey-birth-6w-3m "6 weeks - 3 months"
    :survey-birth-3-6m "3-6 months"
    :survey-birth-6m+ "6+ months"
    :open-source "This is an open source project."}

   :uk
   {:app-name "Маячок"
    :app-title "Маячок — Единбурзька шкала післяполової депресії"
    :tagline "Коротке опитування про ваше самопочуття після пологів"
    :description "Це стандартна анкета (Единбурзька шкала післяполової депресії), яку використовують лікарі по всьому світу. Вона допомагає зрозуміти, чи потрібна вам додаткова підтримка."
    :start-button "Почати опитування →"
    :question-of "Питання %s з 10"
    :question-title "Питання %s — Маячок"
    :next-button "Далі →"
    :finish-button "Завершити"
    :your-result "Ваш результат"
    :result-title "Ваш результат — Маячок"
    :out-of "/30"
    :retake "Пройти знову"
    :crisis-title "🆘 Будь ласка, зателефонуйте зараз"
    :error-title "Помилка"
    :error-invalid-token "Недійсний токен захисту"
    :lang-switch "Українська"
    :survey-title "Анонімне опитування"
    :survey-hint "Допоможіть нам краще зрозуміти мам. Усі дані анонімні."
    :survey-age "Ваш вік"
    :survey-birth "Час після пологів"
    :survey-first "Перша дитина?"
    :survey-skip "Віддаю перевагу не відповідати"
    :survey-yes "Так, перша дитина"
    :survey-no "Ні, у мене є інші діти"
    :survey-submit "Надіслати (необов'язково)"
    :survey-birth-0-6w "0-6 тижнів"
    :survey-birth-6w-3m "6 тижнів - 3 місяці"
    :survey-birth-3-6m "3-6 місяців"
    :survey-birth-6m+ "6+ місяців"
    :open-source "Це проект з відкритим кодом."}})

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
