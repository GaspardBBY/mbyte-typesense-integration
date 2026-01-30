import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'

import './i18n'

import '@coreui/coreui/dist/css/coreui.min.css'
import '@algolia/autocomplete-theme-classic/dist/theme.css'
import './index.css'

import { BrowserRouter } from 'react-router-dom'
import App from './App.tsx'

import { AuthProvider } from 'react-oidc-context'
import { oidcSettings } from './auth/oidc'
import { ManagerApiProvider } from './api/ManagerApiProvider'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <AuthProvider
      {...oidcSettings}
      onSigninCallback={() => {
        // Clean URL after Keycloak redirect (remove code/state from query string)
        window.history.replaceState({}, document.title, window.location.pathname)
      }}
      onSignoutCallback={() => {
        window.history.replaceState({}, document.title, window.location.pathname)
      }}
    >
      <ManagerApiProvider>
        <BrowserRouter>
          <App />
        </BrowserRouter>
      </ManagerApiProvider>
    </AuthProvider>
  </StrictMode>,
)
