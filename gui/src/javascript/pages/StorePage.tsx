import {
  CCard,
  CCardBody,
  CCardHeader,
  CCol,
  CContainer,
  CRow,
  CSpinner,
} from '@coreui/react'
import { useMemo } from 'react'
import { useParams } from 'react-router-dom'
import { useProfile } from '../auth/useProfile'
import { computeStoreBaseUrl } from '../api/storeApi'

function parseStoreIndex(raw: string | undefined): number {
  if (!raw) return 0
  const n = Number.parseInt(raw, 10)
  return Number.isFinite(n) && n >= 0 ? n : 0
}

export function StorePage() {
  const { index } = useParams()
  const storeIndex = parseStoreIndex(index)

  const { profile, isLoading: profileLoading, error: profileError } = useProfile()

  const storeBaseUrl = useMemo(() => {
    const username = profile?.username
    if (!username) return null

    // For now we compute from username + STORES_DOMAIN.
    // Later, when multiple stores exist, storeIndex can map to a specific store.
    return computeStoreBaseUrl({ username, index: storeIndex })
  }, [profile?.username, storeIndex])

  return (
    <CContainer fluid className="py-3">
      <CRow className="g-3">
        <CCol xs={12}>
          <CCard>
            <CCardHeader>Store</CCardHeader>
            <CCardBody>
              {profileLoading && (
                <div className="d-flex align-items-center gap-2">
                  <CSpinner size="sm" />
                  <span>Loading profile…</span>
                </div>
              )}

              {profileError && <p className="text-danger">Profile error: {profileError}</p>}

              {!profileLoading && !profileError && !profile?.username && (
                <p className="text-warning">
                  Profile loaded but username is missing, cannot compute store FQDN.
                </p>
              )}

              {storeBaseUrl && (
                <div>
                  <div>
                    <strong>Store base URL:</strong> {storeBaseUrl}
                  </div>
                  <div className="text-body-secondary">
                    Route index: <code>{storeIndex}</code>
                  </div>
                </div>
              )}

              {!storeBaseUrl && !profileLoading && !profileError && (
                <p className="text-body-secondary">No store URL computed yet.</p>
              )}
            </CCardBody>
          </CCard>
        </CCol>
      </CRow>
    </CContainer>
  )
}
