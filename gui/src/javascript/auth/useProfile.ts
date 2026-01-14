import { useEffect, useState } from 'react'
import { useAuth } from 'react-oidc-context'
import type { Profile } from '../api/entities/Profile'
import { useManagerApi } from '../api/ManagerApiProvider'

type UseProfileResult = {
  profile: Profile | null
  isLoading: boolean
  error: string | null
  reload: () => void
}

/**
 * Loads the current user's Profile from the manager API.
 *
 * - Fetch is triggered only when the user is authenticated.
 * - The backend endpoint (/api/profiles) may redirect to /api/profiles/{id}. Fetch follows redirects.
 */
export function useProfile(): UseProfileResult {
  const auth = useAuth()
  const managerApi = useManagerApi()

  const [profile, setProfile] = useState<Profile | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [reloadKey, setReloadKey] = useState(0)

  const reload = () => setReloadKey((k) => k + 1)

  useEffect(() => {
    if (!auth.isAuthenticated) {
      setProfile(null)
      setError(null)
      setIsLoading(false)
      return
    }

    let cancelled = false
    setIsLoading(true)
    setError(null)

    void managerApi
      .getCurrentProfile()
      .then((p) => {
        if (!cancelled) setProfile(p)
      })
      .catch((e: unknown) => {
        if (!cancelled) setError(e instanceof Error ? e.message : String(e))
      })
      .finally(() => {
        if (!cancelled) setIsLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [auth.isAuthenticated, managerApi, reloadKey])

  return { profile, isLoading, error, reload }
}
