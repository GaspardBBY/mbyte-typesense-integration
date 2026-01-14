export type ApiBaseUrl = string

export type TokenProvider = () => Promise<string>

export async function fetchWithAuth(
  tokenProvider: TokenProvider,
  input: string,
  init: RequestInit = {},
  baseUrl?: ApiBaseUrl,
) {
  const token = await tokenProvider()

  const headers = new Headers(init.headers)
  headers.set('Authorization', `Bearer ${token}`)

  // Defaults to JSON if the body is a plain object (small helper).
  // Does not touch the body if it's already a string / FormData / Blob / etc.

  const hasBody = init.body !== undefined
  const isPlainObjectBody =
    hasBody && typeof init.body === 'object' && !(init.body instanceof FormData) && !(init.body instanceof Blob)

  if (isPlainObjectBody && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  const url = baseUrl ? new URL(input, baseUrl).toString() : input

  return fetch(url, {
    ...init,
    headers,
    body: isPlainObjectBody ? JSON.stringify(init.body) : init.body,
  })
}
