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

import { useAuth } from 'react-oidc-context'
import { useCallback } from 'react'

export function useAccessToken() {
  const auth = useAuth()
  const rawToken = auth.user?.access_token

  return useCallback(async () => {
    const token = rawToken
    if (!token) {
      throw new Error('Missing access token (is the user authenticated?)')
    }
    return token
  }, [rawToken])
}
