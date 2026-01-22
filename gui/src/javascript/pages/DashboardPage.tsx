import {CCol, CContainer, CRow} from '@coreui/react'
import {useTranslation} from 'react-i18next'
import {CreateStoreCard, DetailStoreCard} from '../components'
import {useManagerStatus} from '../auth/useManagerStatus'
import {useProfile} from '../auth/useProfile'
import {useManagerApi} from '../api/ManagerApiProvider'

type DashboardPageProps = {
  onNotify: (message: string) => void
}

export function DashboardPage({ onNotify }: DashboardPageProps) {
  const { t } = useTranslation()
  const { apps, hasStore, reload: reloadStatus } = useManagerStatus()
  const { profile } = useProfile()
  const managerApi = useManagerApi()

  const storeApp = apps.find(a => a.type === 'DOCKER_STORE')

  const handleCreateStore = async () => {
    if (!profile?.id) return
    try {
      const name = profile.username ?? profile.id
      await managerApi.createApp('DOCKER_STORE', name)
      reloadStatus()
      onNotify(t('dashboard.storeCreated'))
    } catch (error) {
      console.error('Failed to create store:', error)
      onNotify(t('dashboard.storeCreationFailed'))
    }
  }

  return (
    <CContainer fluid className="py-3">
      <CRow className="g-3 justify-content-center">
        {!hasStore && (
          <CCol xs={12}>
            <CreateStoreCard
              onCreate={handleCreateStore}
              disabled={false}
              busy={false}
            />
          </CCol>
        )}

        {hasStore && storeApp && (
          <CCol xs={12}>
            <DetailStoreCard
              app={storeApp}
              onRefresh={() => reloadStatus()}
            />
          </CCol>
        )}
      </CRow>
    </CContainer>
  )
}
