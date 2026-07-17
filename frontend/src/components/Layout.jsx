import { useCallback, useEffect, useState } from 'react'
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../lib/auth.jsx'
import api from '../lib/api'
import { num } from '../lib/format'
import Logo from './Logo.jsx'
import {
  IconHome, IconTarget, IconPodium, IconStore, IconSparkles, IconCoins, IconShield,
} from './icons.jsx'

const NAV = [
  { to: '/', label: 'الرئيسية', icon: IconHome },
  { to: '/missions', label: 'المهام', icon: IconTarget },
  { to: '/leaderboard', label: 'الدوري', icon: IconPodium },
  { to: '/season', label: 'الموسم', icon: IconSparkles },
  { to: '/shop', label: 'المتجر', icon: IconStore },
]

export default function Layout() {
  const { user, logout } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()
  const [points, setPoints] = useState({ normalBalance: 0, seasonalBalance: 0 })

  const refreshPoints = useCallback(() => {
    api.get('/points').then(({ data }) => setPoints(data)).catch(() => {})
  }, [])

  useEffect(() => { refreshPoints() }, [location.pathname, refreshPoints])
  useEffect(() => {
    window.addEventListener('points-changed', refreshPoints)
    return () => window.removeEventListener('points-changed', refreshPoints)
  }, [refreshPoints])

  const doLogout = () => { logout(); navigate('/login') }

  return (
    <div className="flex min-h-dvh justify-center lg:items-center lg:py-6">
      {/* Phone frame — full-bleed on real phones, device chrome on big screens */}
      <div className="relative flex h-dvh w-full max-w-[430px] flex-col overflow-hidden bg-[#F8F3EC]
                      lg:h-[min(880px,94dvh)] lg:rounded-[2.4rem] lg:border-[6px] lg:border-navy-900
                      lg:shadow-[0_40px_90px_-20px_rgba(16,38,66,0.5)]">
        {/* Top bar */}
        <header className="z-20 flex items-center justify-between border-b border-[#EDE4D7] bg-[#F8F3EC]/90 px-4 py-2.5 backdrop-blur">
          <Brand />
          <div className="flex items-center gap-1.5">
            <span className="badge num bg-gold-100 text-gold-700"><IconCoins size={12} /> {num(points.normalBalance)}</span>
            {points.seasonalBalance > 0 && (
              <Link to="/season" className="badge num bg-violet-50 text-violet-700"><IconSparkles size={12} /> {num(points.seasonalBalance)}</Link>
            )}
            {user?.role === 'ADMIN' && (
              <Link to="/admin" aria-label="الإدارة" className="flex h-8 w-8 items-center justify-center rounded-full bg-navy-50 text-navy-700"><IconShield size={15} /></Link>
            )}
            {user && (
              <button onClick={doLogout} aria-label="خروج"
                className="flex h-8 w-8 cursor-pointer items-center justify-center rounded-full bg-gradient-to-br from-brand-400 to-brand-700 text-sm font-extrabold text-white transition-transform hover:scale-105"
                title="خروج">
                {(user.displayName || user.phone || '؟').charAt(0)}
              </button>
            )}
          </div>
        </header>

        {/* Scrollable page content */}
        <main className="flex-1 overflow-y-auto overflow-x-clip px-4 pb-28 pt-4">
          <Outlet />
        </main>

        {/* Floating dock */}
        <nav className="dock" aria-label="التنقّل">
          {NAV.map((n) => (
            <NavLink key={n.to} to={n.to} end={n.to === '/'}
              className={({ isActive }) =>
                `flex min-w-[56px] flex-col items-center gap-0.5 rounded-xl px-1.5 py-1 text-[10px] font-bold transition-colors ${
                  isActive ? 'text-brand-700' : 'text-slate-400'
                }`}>
              {({ isActive }) => (
                <>
                  <span className={`flex h-7 w-12 items-center justify-center rounded-full transition-all duration-200 ${
                    isActive ? 'bg-gradient-to-br from-brand-500 to-brand-700 text-white shadow-md shadow-brand-600/40' : ''}`}>
                    <n.icon size={18} />
                  </span>
                  {n.label}
                </>
              )}
            </NavLink>
          ))}
        </nav>
      </div>
    </div>
  )
}

function Brand() {
  return (
    <Link to="/" className="flex items-center gap-2">
      <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-gradient-to-br from-brand-50 to-gold-100">
        <Logo size={28} />
      </div>
      <span className="text-lg font-extrabold text-navy-800">كثّرها</span>
    </Link>
  )
}
