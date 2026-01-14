import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'

import fr from '../assets/i18n/fr.json'
import en from '../assets/i18n/en.json'

const resources = {
  fr: { translation: fr },
  en: { translation: en },
} as const

const STORAGE_KEY = 'mbyte:lang'

type SupportedLanguage = keyof typeof resources

function detectBrowserLanguage(): SupportedLanguage {
  const lang = (navigator.languages?.[0] ?? navigator.language ?? 'en').toLowerCase()
  return lang.startsWith('fr') ? 'fr' : 'en'
}

function getInitialLanguage(): SupportedLanguage {
  const stored = window.localStorage.getItem(STORAGE_KEY)
  if (stored === 'fr' || stored === 'en') return stored
  return detectBrowserLanguage()
}

void i18n
  .use(initReactI18next)
  .init({
    resources,
    lng: getInitialLanguage(),
    fallbackLng: 'en',
    interpolation: {
      escapeValue: false,
    },
  })
  .then(() => {
    i18n.on('languageChanged', (lng) => {
      if (lng === 'fr' || lng === 'en') {
        window.localStorage.setItem(STORAGE_KEY, lng)
      }
    })
  })
