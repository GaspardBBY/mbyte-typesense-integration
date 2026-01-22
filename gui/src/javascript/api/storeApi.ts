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

import { fetchWithAuth, type TokenProvider } from './fetchWithAuth'
import { apiConfig } from './apiConfig'

export type StoreLocator = {
  /** User login used to compute <login>.<storesDomain> */
  username: string
  /** Forward-compatible hook: multiple stores could exist per profile */
  index?: number
}

export function computeStoreBaseUrl(locator: StoreLocator): string {
  const username = locator.username.trim()
  if (!username) {
    throw new Error('Cannot compute store base URL: empty username')
  }

  const host = `${username}.${apiConfig.storesDomain}`
  const scheme = apiConfig.storesScheme

  // Ensure trailing slash so new URL('/q/health', baseUrl) works as expected
  return `${scheme}://${host}/`
}

export type CreateStoreApiOptions = {
  /** Optional explicit base URL (overrides env + computed FQDN) */
  baseUrlOverride?: string
  /** Used to compute the store FQDN when baseUrlOverride + env baseUrl are not provided */
  storeLocator?: StoreLocator
}

export function createStoreApi(tokenProvider: TokenProvider, options: CreateStoreApiOptions = {}) {
  const baseUrl =
    options.baseUrlOverride ??
    apiConfig.storeBaseUrl ??
    (options.storeLocator ? computeStoreBaseUrl(options.storeLocator) : undefined)

  return {
    async getHealth(): Promise<unknown> {
      if (!baseUrl) {
        throw new Error(
          'Store base URL is not configured. Set VITE_API_STORE_BASE_URL or provide storeLocator (username + STORES_DOMAIN).',
        )
      }
      const res = await fetchWithAuth(tokenProvider, '/q/health', { method: 'GET' }, baseUrl)
      const text = await res.text()
      if (!res.ok) {
        throw new Error(`Store health failed (${res.status}): ${text}`)
      }
      try {
        return JSON.parse(text)
      } catch {
        return text
      }
    },
  }
}
