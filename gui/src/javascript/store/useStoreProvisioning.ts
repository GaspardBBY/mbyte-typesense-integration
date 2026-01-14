import { useCallback, useEffect, useRef, useState } from 'react'
import type { Store } from '../api/entities/Store'
import type { Process } from '../api/entities/Process'
import { useManagerApi } from '../api/ManagerApiProvider'
import { useProfile } from '../auth/useProfile'
import { useManagerStatus } from '../auth/useManagerStatus'

export type ProvisioningPhase = 'idle' | 'creating' | 'polling' | 'ready' | 'error'

export type UseStoreProvisioningResult = {
  phase: ProvisioningPhase
  storeId: string | null
  store: Store | null
  processes: Process[]
  error: string | null

  start: () => Promise<void>
  refresh: () => Promise<void>
}

function isStoreProvisioning(store: Store | null): boolean {
  return !!store && (store.status === 'CREATED' || store.status === 'STARTING')
}

/**
 * Store provisioning helper used by the dashboard:
 * - Creates a store (POST /api/profiles/{id}/stores)
 * - Polls store + active processes until the store is AVAILABLE/ERROR/LOST
 * - If user reloads, it re-attaches by checking /api/status + store.status.
 */
export function useStoreProvisioning(): UseStoreProvisioningResult {
  const managerApi = useManagerApi()
  const { profile } = useProfile()
  const { storeIds, reload: reloadStatus } = useManagerStatus()

  const [phase, setPhase] = useState<ProvisioningPhase>('idle')
  const [storeId, setStoreId] = useState<string | null>(null)
  const [store, setStore] = useState<Store | null>(null)
  const [processes, setProcesses] = useState<Process[]>([])
  const [error, setError] = useState<string | null>(null)

  const pollTimer = useRef<number | null>(null)

  const stopPolling = useCallback(() => {
    if (pollTimer.current != null) {
      window.clearTimeout(pollTimer.current)
      pollTimer.current = null
    }
  }, [])

  const doPollOnce = useCallback(
    async (pid: string) => {
      if (!profile?.id) return
      const [s, ps] = await Promise.all([
        managerApi.getProfileStore(profile.id, pid),
        managerApi.getProfileStoreProcesses(profile.id, pid, true),
      ])
      setStore(s)
      setProcesses(ps)

      if (isStoreProvisioning(s)) {
        setPhase('polling')
        stopPolling()
        pollTimer.current = window.setTimeout(() => {
          void doPollOnce(pid)
        }, 2000)
      } else {
        // final state
        setPhase('ready')
        stopPolling()
        await reloadStatus()
      }
    },
    [managerApi, profile?.id, reloadStatus, stopPolling],
  )

  const attachToExisting = useCallback(async () => {
    if (!profile?.id) return
    if (storeIds.length === 0) {
      setStoreId(null)
      setStore(null)
      setProcesses([])
      setPhase('idle')
      return
    }

    const sid = storeIds[0]
    setStoreId(sid)

    try {
      setError(null)
      const s = await managerApi.getProfileStore(profile.id, sid)
      setStore(s)

      if (isStoreProvisioning(s)) {
        setPhase('polling')
        await doPollOnce(sid)
      } else {
        setPhase('ready')
      }
    } catch (e: unknown) {
      setPhase('error')
      setError(e instanceof Error ? e.message : String(e))
    }
  }, [doPollOnce, managerApi, profile?.id, storeIds])

  useEffect(() => {
    void attachToExisting()
    return () => stopPolling()
  }, [attachToExisting, stopPolling])

  const start = useCallback(async () => {
    if (!profile?.id) {
      setError('Profile is not loaded')
      setPhase('error')
      return
    }

    try {
      setError(null)
      setPhase('creating')

      const name = profile.username ?? profile.id
      const sid = await managerApi.createProfileStore(profile.id, name)

      setStoreId(sid)
      setPhase('polling')
      await doPollOnce(sid)
    } catch (e: unknown) {
      setPhase('error')
      setError(e instanceof Error ? e.message : String(e))
    }
  }, [doPollOnce, managerApi, profile?.id, profile?.username])

  const refresh = useCallback(async () => {
    if (!storeId) return
    await doPollOnce(storeId)
  }, [doPollOnce, storeId])

  return { phase, storeId, store, processes, error, start, refresh }
}
