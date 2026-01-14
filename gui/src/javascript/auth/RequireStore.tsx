import type { PropsWithChildren } from 'react'
import { Navigate } from 'react-router-dom'
import { useManagerStatus } from './useManagerStatus'

export function RequireStore({ children }: PropsWithChildren) {
  const { hasStore, isLoading, error } = useManagerStatus()

  if (isLoading) return null
  if (error) return <Navigate to="/dashboard" replace />
  if (!hasStore) return <Navigate to="/dashboard" replace />

  return <>{children}</>
}
