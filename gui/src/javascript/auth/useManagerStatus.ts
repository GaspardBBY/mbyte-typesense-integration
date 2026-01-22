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

import { useEffect, useMemo, useState, useRef, useCallback } from 'react'
import { useAuth } from 'react-oidc-context'
import { useManagerApi } from '../api/ManagerApiProvider'
import type { ManagerStatus } from '../api/entities/ManagerStatus'
import type {Application} from "../api/entities/Application.ts";

export type UseManagerStatusResult = {
  status: ManagerStatus | null
  isLoading: boolean
  error: string | null
  reload: () => void
  apps: Application[]
  hasApp: boolean
  hasStore: boolean
}

/**
 * Loads /api/status from the manager and exposes it as a small UI-friendly hook.
 * Used as a route/menu guard to know whether a store exists for the connected user.
 */
export function useManagerStatus(): UseManagerStatusResult {
  const auth = useAuth()
  const managerApi = useManagerApi()
  const managerApiRef = useRef(managerApi)
  useEffect(() => { managerApiRef.current = managerApi }, [managerApi])

  const [status, setStatus] = useState<ManagerStatus | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [reloadKey, setReloadKey] = useState(0)

  const reload = useCallback(() => setReloadKey((k) => k + 1), [])

  useEffect(() => {
    if (!auth.isAuthenticated) {
      setStatus(null)
      setError(null)
      setIsLoading(false)
      return
    }

    let cancelled = false
    setIsLoading(true)
    setError(null)

    void managerApiRef.current
      .getStatus()
      .then((s) => {
        if (!cancelled) setStatus(s)
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

  const apps = useMemo(() => status?.apps ?? [], [status?.apps])
  const hasApp = apps.length > 0
  const hasStore = useMemo(() => apps.some((a) => a?.type === 'DOCKER_STORE'), [apps])

  return { status, isLoading, error, reload, apps, hasApp, hasStore }
}
