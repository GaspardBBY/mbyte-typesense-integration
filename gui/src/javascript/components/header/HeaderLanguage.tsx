import {CDropdown, CDropdownItem, CDropdownMenu, CDropdownToggle} from '@coreui/react'
import {CIcon} from '@coreui/icons-react'
import {cilGlobeAlt} from '@coreui/icons'
import {useTranslation} from 'react-i18next'

export function HeaderLanguage() {
  const { t, i18n } = useTranslation()

  return (
    <div className="d-flex align-items-center">
      <CDropdown variant="nav-item" alignment="end">
        <CDropdownToggle
          color="link"
          className="p-0 text-white d-inline-flex align-items-center gap-1"
          aria-label={
            i18n.language?.startsWith('fr') ? t('i18n.switchToEnglish') : t('i18n.switchToFrench')
          }
        >
          <CIcon icon={cilGlobeAlt} className="text-white" style={{ color: '#fff' }} />
          <small className="text-white" style={{ color: '#fff' }}>
            {i18n.language?.startsWith('fr') ? 'FR' : 'EN'}
          </small>
        </CDropdownToggle>
        <CDropdownMenu>
          <CDropdownItem
            active={i18n.language?.startsWith('fr')}
            onClick={() => void i18n.changeLanguage('fr')}
          >
            FR
          </CDropdownItem>
          <CDropdownItem
            active={i18n.language?.startsWith('en')}
            onClick={() => void i18n.changeLanguage('en')}
          >
            EN
          </CDropdownItem>
        </CDropdownMenu>
      </CDropdown>
    </div>
  )
}

