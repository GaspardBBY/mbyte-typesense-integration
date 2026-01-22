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

import { WebStorageStateStore, type UserManagerSettings } from 'oidc-client-ts'

function requireEnv(name: string): string {
  const value = import.meta.env[name]
  if (!value || typeof value !== 'string') {
    throw new Error(`Missing env variable: ${name}`)
  }
  return value
}

const baseUrl = window.location.origin

export const oidcSettings: UserManagerSettings = {
  authority: requireEnv('VITE_OIDC_AUTHORITY'),
  client_id: requireEnv('VITE_OIDC_CLIENT_ID'),

  redirect_uri: `${baseUrl}/`,
  post_logout_redirect_uri: `${baseUrl}/`,

  response_type: 'code',
  scope: requireEnv('VITE_OIDC_SCOPE'),

  // Enable silent renew so tokens are refreshed automatically.
  automaticSilentRenew: true,
  monitorSession: false,

  userStore: new WebStorageStateStore({ store: window.sessionStorage }),
}
