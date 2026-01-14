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
