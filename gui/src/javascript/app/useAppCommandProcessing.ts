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

import { useCallback, useEffect, useRef, useState } from 'react'
import type { Process } from '../api/entities/Process'
import { useManagerApi } from '../api/ManagerApiProvider'

export type CommandProcessingPhase = 'idle' | 'running' | 'polling' | 'completed' | 'error'

export type UseAppCommandResult = {
  phase: CommandProcessingPhase
  procId: string | null
  currentProcess: Process | null
  error: string | null

  runCommand: (appId: string, command: string) => Promise<void>
  refresh: () => Promise<void>
}

/**
 * App command processing helper:
 * - Run a command (POST /api/apps/{id}/procs)
 * - Poll the process until completion
 */
export function useAppCommandProcessing(appId: string): UseAppCommandResult {
  const managerApi = useManagerApi()
  const managerApiRef = useRef(managerApi)
  useEffect(() => { managerApiRef.current = managerApi }, [managerApi])

  const [phase, setPhase] = useState<CommandProcessingPhase>('idle')
  const [procId, setProcId] = useState<string | null>(null)
  const [currentProcess, setCurrentProcess] = useState<Process | null>(null)
  const [error, setError] = useState<string | null>(null)

  const pollTimer = useRef<number | null>(null)

  const stopPolling = useCallback(() => {
    if (pollTimer.current != null) {
      window.clearTimeout(pollTimer.current)
      pollTimer.current = null
    }
  }, [])

  const pollProcess = useCallback(async () => {
    if (!procId) return
    try {
      const proc = await managerApiRef.current.getAppProc(appId, procId)
      setCurrentProcess(proc)

      if (proc.status === 'TASK_RUNNING') {
        setPhase('polling')
        pollTimer.current = window.setTimeout(() => {
          void pollProcess()
        }, 2000)
      } else {
        setPhase('completed')
        stopPolling()
      }
    } catch (e: unknown) {
      setPhase('error')
      setError(e instanceof Error ? e.message : String(e))
      stopPolling()
    }
  }, [procId, appId, stopPolling])

  // On mount, check for active processes
  useEffect(() => {
    if (!appId) return
    let cancelled = false
    ;(async () => {
      try {
        const processes = await managerApiRef.current.getAppProcs(appId, true)
        if (cancelled) return
        if (processes.length > 0) {
          const activeProc = processes[0] // assume one active at a time
          setProcId(activeProc.id)
          setCurrentProcess(activeProc)
          if (activeProc.status === 'TASK_RUNNING') {
            setPhase('polling')
            await pollProcess()
          } else {
            setPhase('completed')
          }
        }
      } catch (e: unknown) {
        // ignore errors on load
      }
    })()
    return () => {
      cancelled = true
      stopPolling()
    }
  }, [appId, pollProcess, stopPolling])

  const runCommand = useCallback(async (appId: string, command: string) => {
    try {
      setError(null)
      setPhase('running')
      const pid = await managerApiRef.current.runAppCommand(appId, command)
      setProcId(pid)
      setPhase('polling')
      await pollProcess()
    } catch (e: unknown) {
      setPhase('error')
      setError(e instanceof Error ? e.message : String(e))
    }
  }, [pollProcess])

  const refresh = useCallback(async () => {
    if (procId) {
      await pollProcess()
    }
  }, [pollProcess, procId])

  return { phase, procId, currentProcess, error, runCommand, refresh }
}
