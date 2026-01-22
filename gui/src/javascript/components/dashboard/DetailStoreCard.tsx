import { CButton, CCard, CCardBody, CCardHeader, CBadge, CSpinner } from '@coreui/react'
import { CIcon } from '@coreui/icons-react'
import { cilReload } from '@coreui/icons'
import { useTranslation } from 'react-i18next'
import type { Application } from '../../api/entities/Application'
import { formatTimeAgo } from '../../utils/time'
import { useAppCommandProcessing } from '../../app/useAppCommandProcessing'

export type DetailStoreCardProps = {
  app: Application
  onRefresh: () => void
}

export function DetailStoreCard({ app, onRefresh }: DetailStoreCardProps) {
  const { t } = useTranslation()
  const commandProcessing = useAppCommandProcessing(app.id)

  const canStart = app.status === 'CREATED' || app.status === 'ERROR'

  const getStatusColor = (status: string | undefined) => {
    switch (status) {
      case 'AVAILABLE':
        return 'success'
      case 'STARTING':
        return 'warning'
      case 'CREATED':
        return 'info'
      case 'ERROR':
        return 'danger'
      case 'LOST':
        return 'secondary'
      default:
        return 'primary'
    }
  }

  const createdSince = app.creationDate ? formatTimeAgo(new Date(app.creationDate)) : t('common.unknown')

  const handleStart = () => {
    void commandProcessing.runCommand(app.id, 'START')
  }

  return (
    <CCard>
      <CCardHeader className="d-flex justify-content-between align-items-center">
        <div className="d-flex align-items-center gap-2">
          <strong>'{app.name || t('common.unknown')}' {t('dashboard.storeDetail.title')}</strong>
          <CBadge color={getStatusColor(app.status)}>{app.status}</CBadge>
        </div>
        <CButton
          color="light"
          size="sm"
          onClick={onRefresh}
          title={t('dashboard.storeDetail.refresh')}
        >
          <CIcon icon={cilReload} />
        </CButton>
      </CCardHeader>
      <CCardBody>
        <div className="mb-3">
          <p className="text-muted small mb-2">{t('dashboard.storeDetail.id')}: {app.id}</p>
          {app.creationDate && (
            <div className="text-muted small">
              {t('dashboard.storeDetail.createdSince')}: {createdSince}
            </div>
          )}
        </div>

        {commandProcessing.phase === 'running' && (
          <div className="mb-3 d-flex align-items-center gap-2">
            <CSpinner size="sm" />
            <span>{t('dashboard.storeDetail.starting')}</span>
          </div>
        )}

        {commandProcessing.phase === 'polling' && (
          <div className="mb-3 d-flex align-items-center gap-2">
            <CSpinner size="sm" />
            <span>{t('dashboard.storeDetail.running')}</span>
          </div>
        )}

        {commandProcessing.phase === 'completed' && commandProcessing.currentProcess && (
          <div className="mb-3">
            <CBadge color={commandProcessing.currentProcess.status === 'COMPLETED' ? 'success' : 'danger'}>
              {commandProcessing.currentProcess.status}
            </CBadge>
          </div>
        )}

        {commandProcessing.error && (
          <div className="mb-3 text-danger">
            {commandProcessing.error}
          </div>
        )}

        <div className="d-flex gap-2">
          {canStart && (commandProcessing.phase === 'idle' || commandProcessing.phase === 'error') && (
            <CButton color="success" onClick={handleStart}>
              {t('dashboard.storeDetail.start')}
            </CButton>
          )}
        </div>
      </CCardBody>
    </CCard>
  )
}
