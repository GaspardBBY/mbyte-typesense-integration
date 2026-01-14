function optionalEnv(name: string): string | undefined {
  const v = import.meta.env[name]
  return typeof v === 'string' && v.length > 0 ? v : undefined
}

function requiredEnv(name: string): string {
  const v = optionalEnv(name)
  if (!v) {
    throw new Error(`Missing env variable: ${name}`)
  }
  return v
}

export const apiConfig = {
  managerBaseUrl: requiredEnv('VITE_API_MANAGER_BASE_URL'),

  // Optional explicit store base URL (mainly for local development)
  storeBaseUrl: optionalEnv('VITE_API_STORE_BASE_URL'),

  // Store routing config (used when storeBaseUrl is not set)
  storesDomain: optionalEnv('VITE_STORES_DOMAIN') ?? 's.mbyte.fr',
  storesScheme: optionalEnv('VITE_STORES_SCHEME') ?? 'http',
}
