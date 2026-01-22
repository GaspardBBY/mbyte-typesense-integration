///
/// Copyright (C) 2025 Jerome Blanchard <jayblanc@gmail.com>
///
/// This program is free software: you can redistribute it and/or modify
/// it under the terms of the GNU General Public License as published by
/// the Free Software Foundation, either version 3 of the License, or
/// (at your option) any later version.
///
/// This program is distributed in the hope that it will be useful,
/// but WITHOUT ANY WARRANTY; without even the implied warranty of
/// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
/// GNU General Public License for more details.
///
/// You should have received a copy of the GNU General Public License
/// along with this program.  If not, see <https://www.gnu.org/licenses/>.
///

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
