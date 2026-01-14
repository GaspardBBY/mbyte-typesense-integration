import { type PropsWithChildren, useEffect, useMemo, useRef, useState } from 'react'
import { useAuth } from 'react-oidc-context'

const MAX_REDIRECT_ATTEMPTS = 5
const COOLDOWN_MS = 60_000

const STORAGE_KEY_ATTEMPTS = 'oidc:redirectAttempts'
const STORAGE_KEY_UNTIL = 'oidc:redirectBlockedUntil'

function now(): number {
  return Date.now()
}

function readInt(key: string): number {
  const raw = window.sessionStorage.getItem(key)
  const n = raw ? Number.parseInt(raw, 10) : 0
  return Number.isFinite(n) ? n : 0
}

function writeInt(key: string, value: number): void {
  window.sessionStorage.setItem(key, String(value))
}

function resetGuard(): void {
  window.sessionStorage.removeItem(STORAGE_KEY_ATTEMPTS)
  window.sessionStorage.removeItem(STORAGE_KEY_UNTIL)
}

function AuthStatusScreen({ title, details }: { title: string; details?: string }) {
  return (
    <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
      <div style={{ padding: 16, maxWidth: 560, width: '100%' }}>
        <h2 style={{ marginBottom: 8 }}>{title}</h2>
        {details && <pre style={{ whiteSpace: 'pre-wrap', color: '#666' }}>{details}</pre>}
      </div>
    </div>
  )
}

export function RequireAuth({ children }: PropsWithChildren) {
  const auth = useAuth()
  const [blocked, setBlocked] = useState(false)
  const lastLogRef = useRef<string | null>(null)

  const callbackParams = useMemo(() => {
    const params = new URLSearchParams(window.location.search)
    return {
      hasCode: params.has('code'),
      hasState: params.has('state'),
      error: params.get('error'),
      errorDescription: params.get('error_description'),
    }
  }, [])

  const isOidcCallback = callbackParams.hasCode || callbackParams.hasState || !!callbackParams.error

  useEffect(() => {
    const blockedUntil = readInt(STORAGE_KEY_UNTIL)
    setBlocked(blockedUntil > now())
  }, [])

  // Debug: log key auth state transitions (without spamming every render).
  useEffect(() => {
    const snapshot = JSON.stringify({
      isLoading: auth.isLoading,
      isAuthenticated: auth.isAuthenticated,
      activeNavigator: auth.activeNavigator,
      isOidcCallback,
      error: auth.error ? String(auth.error) : null,
    })

    if (snapshot !== lastLogRef.current) {
      lastLogRef.current = snapshot
      console.debug('[auth] state', {
        isLoading: auth.isLoading,
        isAuthenticated: auth.isAuthenticated,
        activeNavigator: auth.activeNavigator,
        isOidcCallback,
        url: window.location.href,
        error: auth.error,
      })
    }
  }, [auth.isLoading, auth.isAuthenticated, auth.activeNavigator, auth.error, isOidcCallback])

  // Triggers a redirect to the IdP when the user is not authenticated.
  useEffect(() => {
    if (auth.isAuthenticated) {
      resetGuard()
      setBlocked(false)
      return
    }

    if (auth.isLoading) return
    if (isOidcCallback) return
    if (auth.activeNavigator) return

    const blockedUntil = readInt(STORAGE_KEY_UNTIL)
    if (blockedUntil > now()) {
      console.debug('[auth] redirect blocked (cooldown)', { blockedUntil, url: window.location.href })
      setBlocked(true)
      return
    }

    const attempts = readInt(STORAGE_KEY_ATTEMPTS)
    if (attempts >= MAX_REDIRECT_ATTEMPTS) {
      const until = now() + COOLDOWN_MS
      console.debug('[auth] redirect blocked (max attempts reached)', { attempts, until, url: window.location.href })
      writeInt(STORAGE_KEY_UNTIL, until)
      setBlocked(true)
      return
    }

    console.debug('[auth] signinRedirect()', { attempts: attempts + 1, url: window.location.href })
    writeInt(STORAGE_KEY_ATTEMPTS, attempts + 1)
    void auth.signinRedirect()
  }, [auth.isAuthenticated, auth.isLoading, auth.activeNavigator, isOidcCallback])

  // Possible errors: token 401, invalid redirect_uri, CORS issues, bad client config, etc.
  if (auth.error) {
    console.debug('[auth] error', auth.error)
    return (
      <AuthStatusScreen
        title="Authentication error"
        details={
          String(auth.error) +
          '\n\nPlease verify your Keycloak client configuration: public client, standard flow + PKCE, valid redirect URIs and web origins.'
        }
      />
    )
  }

  // IdP error returned in the URL (?error=...)
  if (callbackParams.error) {
    console.debug('[auth] idp error in callback params', callbackParams)
    return (
      <AuthStatusScreen
        title="Authentication error (IdP)"
        details={`${callbackParams.error}${callbackParams.errorDescription ? `: ${callbackParams.errorDescription}` : ''}`}
      />
    )
  }

  if (auth.isLoading) return <AuthStatusScreen title="Loading session…" />

  // If the user is already authenticated, render the app even if the URL hasn't been cleaned yet.
  if (auth.isAuthenticated) {
    return <>{children}</>
  }

  if (isOidcCallback || auth.activeNavigator) {
    return <AuthStatusScreen title="Signing in…" details="Back from Keycloak, exchanging authorization code for tokens." />
  }

  if (blocked) {
    return (
      <div style={{ padding: 16 }}>
        <h2>Authentication temporarily blocked</h2>
        <p>
          Too many redirect attempts ({MAX_REDIRECT_ATTEMPTS}). The redirect loop has been stopped temporarily.
        </p>
        <button
          className="btn btn-primary"
          onClick={() => {
            console.debug('[auth] retry after block: resetting guard and redirecting')
            resetGuard()
            setBlocked(false)
            void auth.signinRedirect()
          }}
        >
          Retry
        </button>
      </div>
    )
  }

  return <AuthStatusScreen title="Redirecting to the identity provider…" />
}
