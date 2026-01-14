import { CSidebar, CSidebarHeader, CSidebarNav } from '@coreui/react'
import { DashboardIcon, SidebarBrand, SidebarNavItem, SidebarProfile, StoreIcon } from '../index'
import { useManagerStatus } from '../../auth/useManagerStatus'

export type SideBarProps = {
  narrow: boolean
}

export function SideBar({ narrow }: SideBarProps) {
  const { hasStore } = useManagerStatus()

  return (
    <CSidebar narrow={narrow} className="mbyte-sidebar">
      <CSidebarHeader className="mbyte-header">
        <SidebarBrand compact={narrow} />
      </CSidebarHeader>

      <div className="border-bottom p-3">
        <SidebarProfile compact={narrow} />
      </div>

      <CSidebarNav>
        <SidebarNavItem to="/dashboard" icon={<DashboardIcon size={narrow ? 'lg' : undefined} />} compact={narrow}>
          Dashboard
        </SidebarNavItem>

        {hasStore && (
          <SidebarNavItem to="/s/0/" icon={<StoreIcon size={narrow ? 'lg' : undefined} />} compact={narrow}>
            Store
          </SidebarNavItem>
        )}
      </CSidebarNav>
    </CSidebar>
  )
}
