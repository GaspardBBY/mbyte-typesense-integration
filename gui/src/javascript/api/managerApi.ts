import { fetchWithAuth, type TokenProvider } from './fetchWithAuth'
import { apiConfig } from './apiConfig'
import type { Profile } from './entities/Profile'
import type { Store } from './entities/Store'
import type { Process } from './entities/Process'
import type { ManagerStatus } from './entities/ManagerStatus'

type CreateStoreResponse = {
  storeId: string
}

async function readJsonOrThrow(res: Response): Promise<unknown> {
  const text = await res.text()
  if (!res.ok) {
    throw new Error(`HTTP ${res.status}: ${text}`)
  }
  try {
    return text ? JSON.parse(text) : null
  } catch {
    return text
  }
}

export function createManagerApi(tokenProvider: TokenProvider) {
  const baseUrl = apiConfig.managerBaseUrl

  const requireBaseUrl = () => {
    if (!baseUrl) {
      throw new Error('VITE_API_MANAGER_BASE_URL is not set')
    }
    return baseUrl
  }

  const listProfileStores = async (profileId: string): Promise<string[]> => {
    const base = requireBaseUrl()
    const res = await fetchWithAuth(tokenProvider, `/api/profiles/${profileId}/stores`, { method: 'GET' }, base)
    return (await readJsonOrThrow(res)) as string[]
  }

  return {
    async getHealth(): Promise<unknown> {
      const base = requireBaseUrl()
      const res = await fetchWithAuth(tokenProvider, '/q/health', { method: 'GET' }, base)
      return readJsonOrThrow(res)
    },

    /**
     * Returns the current user's Profile.
     * The backend endpoint (/api/profiles) may reply with a redirect to /api/profiles/{id}.
     * Fetch follows redirects by default, so a single call is enough.
     */
    async getCurrentProfile(): Promise<Profile> {
      const base = requireBaseUrl()
      const res = await fetchWithAuth(tokenProvider, '/api/profiles', { method: 'GET' }, base)
      return (await readJsonOrThrow(res)) as Profile
    },

    /** Returns store ids for the given profile (GET /api/profiles/{id}/stores). */
    async listProfileStores(profileId: string): Promise<string[]> {
      return listProfileStores(profileId)
    },

    /** Returns a store by id (GET /api/profiles/{id}/stores/{sid}). */
    async getProfileStore(profileId: string, storeId: string): Promise<Store> {
      const base = requireBaseUrl()
      const res = await fetchWithAuth(tokenProvider, `/api/profiles/${profileId}/stores/${storeId}`, { method: 'GET' }, base)
      return (await readJsonOrThrow(res)) as Store
    },

    /** Returns processes for a store (GET /api/profiles/{id}/stores/{sid}/processes?active=true|false). */
    async getProfileStoreProcesses(profileId: string, storeId: string, active = true): Promise<Process[]> {
      const base = requireBaseUrl()
      const url = `/api/profiles/${profileId}/stores/${storeId}/processes?active=${active ? 'true' : 'false'}`
      const res = await fetchWithAuth(tokenProvider, url, { method: 'GET' }, base)
      return (await readJsonOrThrow(res)) as Process[]
    },

    async getStatus(): Promise<ManagerStatus> {
      const base = requireBaseUrl()
      const res = await fetchWithAuth(tokenProvider, '/api/status', { method: 'GET' }, base)
      return (await readJsonOrThrow(res)) as ManagerStatus
    },

    /** Creates a store for the given profile (POST /api/profiles/{id}/stores). */
    async createProfileStore(profileId: string, name: string): Promise<string> {
      const base = requireBaseUrl()

      const body = new URLSearchParams({ name })

      const res = await fetchWithAuth(
        tokenProvider,
        `/api/profiles/${profileId}/stores`,
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
          },
          body,
        },
        base,
      )

      const json = (await readJsonOrThrow(res)) as CreateStoreResponse
      if (!json?.storeId) {
        throw new Error('Invalid createProfileStore response: missing storeId')
      }
      return json.storeId
    },
  }
}
