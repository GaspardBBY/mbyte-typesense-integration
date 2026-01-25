import { useState } from 'react'
import { CModal, CModalHeader, CModalTitle, CModalBody, CModalFooter, CButton, CFormInput, CFormLabel } from '@coreui/react'
import { useTranslation } from 'react-i18next'

type CreateModalProps = {
  visible: boolean
  onClose: () => void
  onConfirm: (data: string | File) => void
  type: 'folder' | 'file'
}

export function CreateModal({ visible, onClose, onConfirm, type }: CreateModalProps) {
  const { t } = useTranslation()
  const [name, setName] = useState('')
  const [file, setFile] = useState<File | null>(null)

  const handleConfirm = () => {
    if (type === 'folder') {
      if (name.trim()) {
        onConfirm(name.trim())
        setName('')
      }
    } else {
      if (file) {
        onConfirm(file)
        setFile(null)
      }
    }
  }

  const handleClose = () => {
    setName('')
    setFile(null)
    onClose()
  }

  return (
    <CModal visible={visible} onClose={handleClose}>
      <CModalHeader>
        <CModalTitle>
          {type === 'folder' ? t('store.createModal.titleFolder') : t('store.createModal.titleFile')}
        </CModalTitle>
      </CModalHeader>
      <CModalBody>
        {type === 'folder' ? (
          <div>
            <CFormLabel htmlFor="folderName">{t('store.createModal.nameLabel')}</CFormLabel>
            <CFormInput
              id="folderName"
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder={t('store.enterFolderName')}
            />
          </div>
        ) : (
          <div>
            <CFormLabel htmlFor="fileInput">{t('store.createModal.fileLabel')}</CFormLabel>
            <CFormInput
              id="fileInput"
              type="file"
              onChange={(e) => setFile(e.target.files?.[0] || null)}
            />
          </div>
        )}
      </CModalBody>
      <CModalFooter>
        <CButton color="secondary" onClick={handleClose}>
          {t('store.createModal.cancel')}
        </CButton>
        <CButton color="primary" onClick={handleConfirm} disabled={type === 'folder' ? !name.trim() : !file}>
          {t('store.createModal.confirm')}
        </CButton>
      </CModalFooter>
    </CModal>
  )
}
