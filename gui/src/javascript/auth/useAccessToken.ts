import { useAuth } from 'react-oidc-context'

export function useAccessToken() {
  const auth = useAuth()

  return async () => {
    const token = auth.user?.access_token
    if (!token) {
      throw new Error('Missing access token (is the user authenticated?)')
    }
    return token
  }
}
