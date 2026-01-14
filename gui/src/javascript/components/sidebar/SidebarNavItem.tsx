import { CNavItem } from '@coreui/react'
import type { PropsWithChildren, ReactNode } from 'react'
import { NavLink } from 'react-router-dom'

export type SidebarNavItemProps = PropsWithChildren<{
  to: string
  icon?: ReactNode
  compact?: boolean
}>

/**
 * Sidebar nav item that uses React Router for SPA navigation,
 * while keeping CoreUI sidebar/nav styling.
 */
export function SidebarNavItem({ to, icon, compact, children }: SidebarNavItemProps) {
  return (
    <CNavItem>
      <NavLink
        to={to}
        className={({ isActive }) =>
          `mbyte-sidebar-link ${compact ? 'is-compact' : ''} ${isActive ? 'is-active' : ''}`.trim()
        }
        style={{ cursor: 'pointer' }}
      >
        <span className="mbyte-sidebar-link__inner">
          {icon && <span className="mbyte-sidebar-link__icon">{icon}</span>}
          {!compact && <span className="mbyte-sidebar-link__label">{children}</span>}
        </span>
      </NavLink>
    </CNavItem>
  )
}

