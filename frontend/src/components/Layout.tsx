import clsx from 'clsx'
import {
  BedDouble, ChefHat, ClipboardList, Home, LayoutDashboard, LogOut, MessageSquareText, Receipt, Sparkles,
  UtensilsCrossed, Users, Wallet, Wrench, BarChart3, type LucideIcon,
} from 'lucide-react'
import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../auth'
import { roleLabel } from '../lib/format'
import type { Role } from '../lib/types'
import { Logo } from './Logo'

interface NavItem { to: string; label: string; icon: LucideIcon; end?: boolean }

const NAV: Record<Role, NavItem[]> = {
  STUDENT: [
    { to: '/', label: 'Home', icon: Home, end: true },
    { to: '/meals', label: 'Meals', icon: UtensilsCrossed },
    { to: '/account', label: 'Account', icon: Wallet },
    { to: '/services', label: 'Room services', icon: Sparkles },
  ],
  MESS_STAFF: [
    { to: '/mess', label: "Today's board", icon: ChefHat, end: true },
    { to: '/mess/menu', label: 'Menu & prices', icon: UtensilsCrossed },
    { to: '/mess/feedback', label: 'Feedback', icon: MessageSquareText },
  ],
  ADMIN: [
    { to: '/admin', label: 'Overview', icon: LayoutDashboard, end: true },
    { to: '/admin/mess', label: 'Mess analytics', icon: BarChart3 },
    { to: '/mess', label: 'Mess operations', icon: ChefHat, end: true },
    { to: '/mess/menu', label: 'Menu & prices', icon: UtensilsCrossed },
    { to: '/mess/feedback', label: 'Food feedback', icon: MessageSquareText },
    { to: '/admin/finance', label: 'Finance', icon: Receipt },
    { to: '/admin/residents', label: 'Residents & rooms', icon: Users },
    { to: '/admin/requests', label: 'Service requests', icon: ClipboardList },
  ],
  WARDEN: [
    { to: '/warden', label: 'Overview', icon: LayoutDashboard, end: true },
    { to: '/admin/residents', label: 'Residents & rooms', icon: BedDouble },
    { to: '/admin/requests', label: 'Service requests', icon: ClipboardList },
  ],
  HOUSEKEEPING_STAFF: [{ to: '/housekeeping', label: 'Cleaning jobs', icon: Sparkles, end: true }],
  MAINTENANCE_STAFF: [{ to: '/maintenance', label: 'Complaints', icon: Wrench, end: true }],
}

export function Layout() {
  const { user, logout } = useAuth()
  if (!user) return null
  const items = NAV[user.role]
  return (
    <div className="min-h-screen lg:flex">
      {/* Desktop sidebar */}
      <aside className="hidden w-64 shrink-0 flex-col bg-forest-dark px-4 py-6 text-ivory lg:fixed lg:inset-y-0 lg:flex">
        <div className="px-2"><Logo light /></div>
        <p className="mt-1 px-2 text-xs text-ivory/60">A better way to live on campus.</p>
        <nav className="mt-8 flex flex-1 flex-col gap-0.5" aria-label="Main">
          {items.map((i) => (
            <NavLink key={i.to + i.label} to={i.to} end={i.end}
              className={({ isActive }) => clsx('flex items-center gap-3 rounded-md px-3 py-2.5 text-sm transition-colors',
                isActive ? 'bg-ivory/12 font-medium text-ivory' : 'text-ivory/70 hover:bg-ivory/8 hover:text-ivory')}>
              <i.icon className="h-[18px] w-[18px]" strokeWidth={1.8} /> {i.label}
            </NavLink>
          ))}
        </nav>
        <div className="border-t border-ivory/15 px-2 pt-4">
          <p className="truncate text-sm font-medium">{user.fullName}</p>
          <p className="text-xs text-ivory/60">{roleLabel(user.role)}</p>
          <button onClick={logout} className="mt-3 flex items-center gap-2 text-sm text-ivory/70 hover:text-ivory">
            <LogOut className="h-4 w-4" /> Sign out
          </button>
        </div>
      </aside>

      {/* Mobile top bar */}
      <header className="sticky top-0 z-30 flex items-center justify-between border-b border-line bg-ivory/95 px-4 py-3 backdrop-blur lg:hidden">
        <Logo />
        <button onClick={logout} aria-label="Sign out" className="flex items-center gap-1.5 text-sm text-muted"><LogOut className="h-4 w-4" /> Sign out</button>
      </header>

      <main className="min-w-0 flex-1 px-4 pb-28 pt-6 sm:px-8 lg:ml-64 lg:px-10 lg:pb-12 lg:pt-9">
        <div className="mx-auto max-w-6xl"><Outlet /></div>
      </main>

      {/* Mobile bottom navigation */}
      {items.length > 1 && (
        <nav aria-label="Main mobile" className="fixed inset-x-0 bottom-0 z-30 flex overflow-x-auto border-t border-line bg-paper lg:hidden">
          {items.map((i) => (
            <NavLink key={i.to + i.label} to={i.to} end={i.end}
              className={({ isActive }) => clsx('flex min-w-[76px] flex-1 flex-col items-center gap-0.5 px-2 py-2 text-[11px]', isActive ? 'font-semibold text-forest' : 'text-muted')}>
              <i.icon className="h-5 w-5" strokeWidth={1.8} /> <span className="whitespace-nowrap">{i.label}</span>
            </NavLink>
          ))}
        </nav>
      )}
    </div>
  )
}
