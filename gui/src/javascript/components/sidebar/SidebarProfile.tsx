import { CButton } from '@coreui/react'
import { useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { useAuth } from 'react-oidc-context'
import { useProfile } from '../../auth/useProfile'
import SparkMD5 from 'spark-md5'

function truncateEmail(email: string, max: number): string {
  if (email.length <= max) return email
  if (max <= 3) return '.'.repeat(max)
  return email.slice(0, max - 3) + '...'
}

export type SidebarProfileProps = {
  compact?: boolean
}

export function SidebarProfile({ compact }: SidebarProfileProps) {
  const { t } = useTranslation()
  const auth = useAuth()
  const { profile } = useProfile()

  const fullName =
    profile?.fullname ?? auth.user?.profile?.name ?? auth.user?.profile?.preferred_username ?? t('common.user')
  const email = profile?.email ?? auth.user?.profile?.email ?? ''

  const gravatarUrl = useMemo(() => {
    const hashFromProfile = profile?.gravatarHash
    const source = hashFromProfile ?? (email ? SparkMD5.hash(email.trim().toLowerCase()) : null)
    if (!source) return null
    return `https://www.gravatar.com/avatar/${source}?s=120&d=identicon`
  }, [profile?.gravatarHash, email])

  const truncatedEmail = useMemo(() => (email ? truncateEmail(email, 50) : ''), [email])

  if (compact) {
    return (
      <div className="mbyte-userblock mbyte-userblock--compact">
        <div className="mbyte-userblock__avatar">
          {gravatarUrl ? (
            <img
              className="img-thumbnail rounded-circle"
              src={gravatarUrl}
              alt="Avatar"
              width={44}
              height={44}
            />
          ) : (
            <div className="rounded-circle bg-body-tertiary" style={{ width: 44, height: 44 }} />
          )}
        </div>
      </div>
    )
  }

  return (
    <div className="mbyte-userblock">
      <div className="mbyte-userblock__avatar">
        {gravatarUrl ? (
          <img
            className="img-thumbnail rounded-circle"
            src={gravatarUrl}
            alt="Avatar"
            width={60}
            height={60}
          />
        ) : (
          <div className="rounded-circle bg-body-tertiary" style={{ width: 60, height: 60 }} />
        )}
      </div>

      <div className="mbyte-userblock__name">{t('sidebar.hello', { name: fullName })}</div>

      {email && (
        <div className="mbyte-userblock__email" title={email}>
          {truncatedEmail}
        </div>
      )}

      <div className="mbyte-userblock__actions">
        <CButton color="light" variant="outline" size="sm" onClick={() => void auth.signoutRedirect()}>
          {t('auth.logout')}
        </CButton>
      </div>
    </div>
  )
}
