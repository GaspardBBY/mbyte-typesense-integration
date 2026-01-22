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

import { useEffect, useState, useRef } from 'react'
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
  const managerApiRef = useRef(managerApi)
  // keep ref updated when managerApi identity changes
  useEffect(() => {
    managerApiRef.current = managerApi
  }, [managerApi])

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

    void managerApiRef.current
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
  }, [auth.isAuthenticated, reloadKey])

  return { profile, isLoading, error, reload }
}
