import { CBadge, CButton, CCard, CCardBody, CCardFooter } from '@coreui/react'
import { useTranslation } from 'react-i18next'
import { CIcon } from '@coreui/icons-react'
import { cilCheckCircle } from '@coreui/icons'

export type CreateStoreCardProps = {
  onCreate: () => void
  disabled?: boolean
  busy?: boolean
}

export function CreateStoreCard({ onCreate, disabled, busy }: CreateStoreCardProps) {
  const { t } = useTranslation()

  return (
    <CCard className="mx-auto shadow-sm" style={{ maxWidth: 520 }}>
      <CCardBody className="text-center py-4">
        <div className="fw-semibold fs-5">{t('dashboard.createStore.offerTitle')}</div>

        <div className="mx-auto my-3" style={{ height: 1, width: '50%', background: 'rgba(0,0,0,0.12)' }} />

        <div className="mt-4 d-flex justify-content-center align-items-center gap-2">
          <CBadge color="success">{t('dashboard.createStore.freePlan')}</CBadge>
        </div>

        <div className="mx-auto my-3" style={{ height: 1, width: '50%', background: 'rgba(0,0,0,0.12)' }} />

        <div className="d-inline-flex flex-column gap-2 text-start" style={{ marginTop: 8 }}>
          <div className="d-flex align-items-center gap-2 text-body-secondary">
            <CIcon icon={cilCheckCircle} />
            <span>{t('dashboard.createStore.storageSpace')}</span>
          </div>
          <div className="d-flex align-items-center gap-2 text-body-secondary">
            <CIcon icon={cilCheckCircle} />
            <span>{t('dashboard.createStore.rateLimit')}</span>
          </div>
          <div className="d-flex align-items-center gap-2 text-body-secondary">
            <CIcon icon={cilCheckCircle} />
            <span>{t('dashboard.createStore.downloadQuota')}</span>
          </div>
        </div>
      </CCardBody>

      <CCardFooter style={{ background: 'rgba(0,0,0,0.03)' }} className="py-3">
        <div className="d-flex justify-content-center">
          <CButton color="primary" onClick={onCreate} disabled={disabled}>
            {busy ? t('dashboard.createStore.creationInProgress') : t('dashboard.createStore.startNow')}
          </CButton>
        </div>
      </CCardFooter>
    </CCard>
  )
}
