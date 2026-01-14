import { CSidebarBrand } from '@coreui/react'

import logo from '../../../assets/img/logo.png'
import logoSingle from '../../../assets/img/logo-single.png'

export type SidebarBrandProps = {
  /** When true, shows the compact logo (used when the sidebar is collapsed). */
  compact?: boolean
}

export function SidebarBrand({ compact }: SidebarBrandProps) {
  return (
    <CSidebarBrand className="mbyte-brand">
      <img
        src={compact ? logoSingle : logo}
        alt="MByte"
        className="mbyte-brand__img"
        height={32}
      />
    </CSidebarBrand>
  )
}
