import type { PropsWithChildren } from 'react'
import { Navigate } from 'react-router-dom'
import { useManagerStatus } from './useManagerStatus'

export function RequireApp({ children }: PropsWithChildren) {
  const { hasApp, isLoading, error } = useManagerStatus()

  if (isLoading) return null
  if (error) return <Navigate to="/dashboard" replace />
  if (!hasApp) return <Navigate to="/dashboard" replace />

  return <>{children}</>
}
