import { createContext, type PropsWithChildren, useContext, useMemo } from 'react'
import { createManagerApi } from './managerApi'
import type { Profile } from './entities/Profile'
import type { TokenProvider } from './fetchWithAuth'
import { useAccessToken } from '../auth/useAccessToken'

export type ManagerApi = {
  getHealth(): Promise<unknown>
  getCurrentProfile(): Promise<Profile>
  listProfileStores(profileId: string): Promise<string[]>
  getProfileStore(profileId: string, storeId: string): Promise<import('./entities/Store').Store>
  getProfileStoreProcesses(profileId: string, storeId: string, active?: boolean): Promise<import('./entities/Process').Process[]>
  createProfileStore(profileId: string, name: string): Promise<string>
  getStatus(): Promise<import('./entities/ManagerStatus').ManagerStatus>
}

const ManagerApiContext = createContext<ManagerApi | null>(null)

export type ManagerApiProviderProps = {
  /**
   * Optional override for the token provider.
   * By default, the provider uses the OIDC access token from react-oidc-context.
   */
  tokenProvider?: TokenProvider
}

export function ManagerApiProvider({ children, tokenProvider }: PropsWithChildren<ManagerApiProviderProps>) {
  const defaultTokenProvider = useAccessToken()

  const api = useMemo(() => {
    return createManagerApi(tokenProvider ?? defaultTokenProvider)
  }, [tokenProvider, defaultTokenProvider])

  return <ManagerApiContext.Provider value={api}>{children}</ManagerApiContext.Provider>
}

export function useManagerApi(): ManagerApi {
  const api = useContext(ManagerApiContext)
  if (!api) {
    throw new Error('useManagerApi must be used within a ManagerApiProvider')
  }
  return api
}
