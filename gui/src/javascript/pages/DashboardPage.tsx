import {
  CAlert,
  CButton,
  CCard,
  CCardBody,
  CCardHeader,
  CCol,
  CContainer,
  CRow,
  CSpinner,
} from '@coreui/react'
import { useTranslation } from 'react-i18next'
import { CreateStoreCard } from '../components'
import { useStoreProvisioning } from '../store/useStoreProvisioning'

type DashboardPageProps = {
  onNotify: () => void
}

export function DashboardPage({ onNotify }: DashboardPageProps) {
  const { t } = useTranslation()
  const provisioning = useStoreProvisioning()

  const isCreating = provisioning.phase === 'creating' || provisioning.phase === 'polling'
  const hasStore = provisioning.phase === 'ready' && !!provisioning.store

  return (
    <CContainer fluid className="py-3">
      <CRow className="g-3 justify-content-center">
        {provisioning.error && (
          <CCol xs={12}>
            <CAlert color="danger">{provisioning.error}</CAlert>
          </CCol>
        )}

        {!hasStore && (
          <CCol xs={12}>
            <CreateStoreCard
              onCreate={() => {
                void provisioning.start().then(() => {
                  onNotify()
                })
              }}
              disabled={isCreating}
              busy={isCreating}
            />

            {isCreating && (
              <div className="mt-3 d-flex justify-content-center align-items-center gap-2 text-body-secondary">
                <CSpinner size="sm" />
                <span>{t('dashboard.createStore.creationInProgress')}</span>
              </div>
            )}
          </CCol>
        )}

        {hasStore && (
          <CCol xs={12} lg={6}>
            <CCard>
              <CCardHeader>Store</CCardHeader>
              <CCardBody>
                <div>
                  <strong>ID:</strong> {provisioning.store?.id}
                </div>
                <div>
                  <strong>Name:</strong> {provisioning.store?.name}
                </div>
                <div>
                  <strong>Status:</strong> {provisioning.store?.status}
                </div>
                {provisioning.store?.location && (
                  <div>
                    <strong>Location:</strong> {provisioning.store.location}
                  </div>
                )}

                <div className="mt-3">
                  <CButton color="primary" onClick={() => void provisioning.refresh()}>
                    Refresh
                  </CButton>
                </div>
              </CCardBody>
            </CCard>
          </CCol>
        )}

        {provisioning.phase === 'ready' && !provisioning.store && (
          <CCol xs={12}>
            <div className="text-body-secondary">{t('dashboard.callInProgress')}</div>
          </CCol>
        )}
      </CRow>
    </CContainer>
  )
}
