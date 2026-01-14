import { useEffect, useMemo, useState } from 'react'
import { useAuth } from 'react-oidc-context'
import { useManagerApi } from '../api/ManagerApiProvider'
import type { ManagerStatus } from '../api/entities/ManagerStatus'

export type UseManagerStatusResult = {
  status: ManagerStatus | null
  isLoading: boolean
  error: string | null
  reload: () => void
  storeIds: string[]
  hasStore: boolean
}

/**
 * Loads /api/status from the manager and exposes it as a small UI-friendly hook.
 * Used as a route/menu guard to know whether a store exists for the connected user.
 */
export function useManagerStatus(): UseManagerStatusResult {
  const auth = useAuth()
  const managerApi = useManagerApi()

  const [status, setStatus] = useState<ManagerStatus | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [reloadKey, setReloadKey] = useState(0)

  const reload = () => setReloadKey((k) => k + 1)

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

    void managerApi
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
  }, [auth.isAuthenticated, managerApi, reloadKey])

  const storeIds = useMemo(() => status?.stores ?? [], [status?.stores])
  const hasStore = storeIds.length > 0

  return { status, isLoading, error, reload, storeIds, hasStore }
}
