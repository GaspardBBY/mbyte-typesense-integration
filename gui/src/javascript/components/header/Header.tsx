import { CHeader, CHeaderBrand, CHeaderNav, CHeaderToggler } from '@coreui/react'
import { CIcon } from '@coreui/icons-react'
import { cilMenu } from '@coreui/icons'
import { HeaderLanguage } from '../index'

export type HeaderProps = {
  onToggleSidebar: () => void
}

export function Header({ onToggleSidebar }: HeaderProps) {
  return (
    <CHeader className="mbyte-header">
      <CHeaderToggler className="ps-3" onClick={onToggleSidebar} aria-label="toggle sidebar">
        <CIcon icon={cilMenu} size="lg" className="text-white" style={{ color: '#fff' }} />
      </CHeaderToggler>
      <CHeaderBrand className="mx-3"></CHeaderBrand>
      <CHeaderNav className="ms-auto me-3">
        <HeaderLanguage />
      </CHeaderNav>
    </CHeader>
  )
}

