import { CTable, CTableHead, CTableRow, CTableHeaderCell, CTableBody, CTableDataCell } from '@coreui/react'
import { CIcon } from '@coreui/icons-react'
import { cilFolderOpen, cilFile } from '@coreui/icons'
import { useTranslation } from 'react-i18next'
import Node from '../../api/entities/Node'

type BrowserAreaProps = Readonly<{
  files: Node[]
  viewMode: 'table' | 'grid'
  onSelect: (f: Node) => void
  onOpenFolder?: (folderId: string) => void
  onAction?: (action: string, file: Node) => void
}>

export function BrowserArea({ files, viewMode, onSelect, onOpenFolder, onAction }: BrowserAreaProps) {
  const { t } = useTranslation()
  // fill all available space, no padding
  return (
    <div style={{ height: '100%', width: '100%', overflow: 'auto' }}>
      {viewMode === 'table' ? (
        <CTable hover responsive className="mb-0" style={{ borderRadius: 0, width: '100%', borderCollapse: 'collapse' }}>
          <CTableHead style={{ boxSizing: 'border-box' }}>
            <CTableRow style={{ height: 56, minHeight: 56, boxSizing: 'border-box' }}>
              <CTableHeaderCell style={{ background: '#f5f6f7', width: '55%', padding: '12px 16px', verticalAlign: 'middle', height: 56, minHeight: 56, lineHeight: '20px', fontWeight: 600, boxSizing: 'border-box' }}>{t('store.name')}</CTableHeaderCell>
              <CTableHeaderCell style={{ background: '#f5f6f7', width: '8%', padding: '12px 16px', verticalAlign: 'middle', height: 56, minHeight: 56, lineHeight: '20px', fontWeight: 600, boxSizing: 'border-box' }} className="text-end">{t('store.size')}</CTableHeaderCell>
              <CTableHeaderCell style={{ background: '#f5f6f7', width: '8%', padding: '12px 16px', verticalAlign: 'middle', height: 56, minHeight: 56, lineHeight: '20px', fontWeight: 600, boxSizing: 'border-box' }} className="text-end">{t('store.type')}</CTableHeaderCell>
              <CTableHeaderCell style={{ background: '#f5f6f7', width: '8%', padding: '12px 16px', verticalAlign: 'middle', height: 56, minHeight: 56, lineHeight: '20px', fontWeight: 600, boxSizing: 'border-box' }} className="text-end">{t('store.creation')}</CTableHeaderCell>
              <CTableHeaderCell style={{ background: '#f5f6f7', width: '7%', padding: '12px 16px', verticalAlign: 'middle', height: 56, minHeight: 56, lineHeight: '20px', fontWeight: 600, boxSizing: 'border-box' }} className="text-end">{t('store.modification')}</CTableHeaderCell>
              <CTableHeaderCell style={{ background: '#f5f6f7', width: '10%', padding: '12px 16px', verticalAlign: 'middle', height: 56, minHeight: 56, lineHeight: '20px', fontWeight: 600, boxSizing: 'border-box', whiteSpace: 'nowrap' }} className="text-end">{t('store.actions')}</CTableHeaderCell>
            </CTableRow>
          </CTableHead>
          <CTableBody>
            {files.map((f, idx) => (
              <CTableRow
                key={f.id}
                onClick={() => onSelect(f)}
                style={{ cursor: 'pointer', minHeight: 56, boxSizing: 'border-box' }}
              >
                <CTableDataCell style={{ background: idx % 2 === 0 ? '#fafafa' : '#f5f6f7', padding: '12px 16px', verticalAlign: 'middle', lineHeight: '20px', minHeight: 56, boxSizing: 'border-box', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                  <span style={{ marginRight: 8 }}>
                    <CIcon icon={f.isFolder ? cilFolderOpen : cilFile} />
                  </span>
                  {f.isFolder ? <strong>{f.name}</strong> : f.name}
                </CTableDataCell>
                <CTableDataCell style={{ background: idx % 2 === 0 ? '#fafafa' : '#f5f6f7', padding: '12px 16px', verticalAlign: 'middle', lineHeight: '20px', minHeight: 56, boxSizing: 'border-box' }} className="text-end">{f.size ? `${Math.round(f.size / 1024)} KB` : ''}</CTableDataCell>
                <CTableDataCell style={{ background: idx % 2 === 0 ? '#fafafa' : '#f5f6f7', padding: '12px 16px', verticalAlign: 'middle', lineHeight: '20px', minHeight: 56, boxSizing: 'border-box' }} className="text-end">{f.isFolder ? t('store.folder') : t('store.file')}</CTableDataCell>
                <CTableDataCell style={{ background: idx % 2 === 0 ? '#fafafa' : '#f5f6f7', padding: '12px 16px', verticalAlign: 'middle', lineHeight: '20px', minHeight: 56, boxSizing: 'border-box' }} className="text-end">{f.creationTs ? new Date(f.creationTs).toLocaleDateString() : ''}</CTableDataCell>
                <CTableDataCell style={{ background: idx % 2 === 0 ? '#fafafa' : '#f5f6f7', padding: '12px 16px', verticalAlign: 'middle', lineHeight: '20px', minHeight: 56, boxSizing: 'border-box' }} className="text-end">{f.modificationTs ? new Date(f.modificationTs).toLocaleDateString() : ''}</CTableDataCell>
                <CTableDataCell style={{ background: idx % 2 === 0 ? '#fafafa' : '#f5f6f7', width: '10%', padding: '12px 16px', verticalAlign: 'middle', lineHeight: '20px', minHeight: 56, boxSizing: 'border-box', whiteSpace: 'nowrap' }} className="text-end">
                  {f.isFolder ? (
                    <button className="btn btn-sm btn-outline-primary" style={{ whiteSpace: 'nowrap' }} onClick={(e) => { e.stopPropagation(); onOpenFolder?.(f.id) }}>
                      {t('store.open')}
                    </button>
                  ) : (
                    <div className="d-flex gap-2 justify-content-end" style={{ whiteSpace: 'nowrap' }}>
                      <button className="btn btn-sm btn-outline-secondary" style={{ whiteSpace: 'nowrap' }} onClick={(e) => { e.stopPropagation(); onAction?.('download', f) }}>
                        {t('store.download')}
                      </button>
                      <button className="btn btn-sm btn-outline-secondary" style={{ whiteSpace: 'nowrap' }} onClick={(e) => { e.stopPropagation(); onAction?.('info', f) }}>
                        {t('store.info')}
                      </button>
                    </div>
                  )}
                </CTableDataCell>
              </CTableRow>
             ))}
           </CTableBody>
         </CTable>
      ) : (
        <div className="row g-3 p-3">
          {files.map((f) => (
            <div key={f.id} className="col-6 col-md-4 col-lg-3">
              <button type="button" className="btn p-0" onClick={() => onSelect(f)} style={{ cursor: 'pointer', display: 'block', textAlign: 'left', border: 'none', background: 'transparent' }}>
                <div className="card h-100">
                  <div className="card-body">
                    <div className="d-flex align-items-center gap-2">
                      <CIcon icon={f.isFolder ? cilFolderOpen : cilFile} />
                      <div>
                        <div className="fw-semibold">{f.name}</div>
                        <div className="text-muted small">{f.isFolder ? t('store.folder') : t('store.file')} • {f.modificationTs ? new Date(f.modificationTs).toLocaleDateString() : ''}</div>
                      </div>
                    </div>
                  </div>
                </div>
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
