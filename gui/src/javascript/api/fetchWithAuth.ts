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
    hasBody &&
    typeof init.body === 'object' &&
    !(init.body instanceof FormData) &&
    !(init.body instanceof Blob) &&
    !(init.body instanceof URLSearchParams)

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

export async function readJsonOrThrow(res: Response): Promise<unknown> {
  const text = await res.text()
  if (!res.ok) {
    console.error('API Error:', res.status, res.statusText, text)
    throw new Error(`HTTP ${res.status}: ${text}`)
  }
  try {
    return text ? JSON.parse(text) : null
  } catch {
    return text
  }
}
