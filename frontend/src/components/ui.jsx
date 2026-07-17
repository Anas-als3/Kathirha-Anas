import { IconSparkles } from './icons.jsx'

export function Loader({ full = false, label = 'جارٍ التحميل…' }) {
  return (
    <div className={`${full ? 'min-h-screen' : 'py-10'} space-y-3`} role="status" aria-label={label}>
      <div className="skeleton h-24 w-full" />
      <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
        <div className="skeleton h-24" /><div className="skeleton h-24" />
        <div className="skeleton hidden h-24 lg:block" /><div className="skeleton hidden h-24 lg:block" />
      </div>
      <div className="skeleton h-48 w-full" />
    </div>
  )
}

export function Card({ className = '', children }) {
  return <div className={`card ${className}`}>{children}</div>
}

/** Stat with an SVG icon in a tinted coin — no emojis. */
export function StatCard({ label, value, sub, icon: Ico, accent = 'brand' }) {
  const tints = {
    brand: 'bg-brand-50 text-brand-600',
    navy: 'bg-navy-50 text-navy-700',
    gold: 'bg-gold-100 text-gold-700',
    green: 'bg-emerald-50 text-emerald-700',
  }
  return (
    <div className="card card-hover relative overflow-hidden">
      <div className={`flex h-10 w-10 items-center justify-center rounded-xl ${tints[accent] || tints.brand}`}>
        {Ico ? <Ico size={20} /> : null}
      </div>
      <div className="num mt-3 text-2xl font-extrabold text-navy-800">{value}</div>
      <div className="text-sm font-semibold text-slate-500">{label}</div>
      {sub && <div className="mt-1 text-xs leading-relaxed text-slate-400">{sub}</div>}
    </div>
  )
}

export function Progress({ value = 0, coral = false, dark = false }) {
  const v = Math.max(0, Math.min(100, Number(value) || 0))
  return (
    <div className={`bar ${dark ? 'bar-dark' : ''}`} role="progressbar" aria-valuenow={Math.round(v)} aria-valuemin={0} aria-valuemax={100}>
      <i className={coral ? 'coral' : ''} style={{ width: `${v}%` }} />
    </div>
  )
}

export function Badge({ children, color = 'brand' }) {
  const colors = {
    brand: 'bg-brand-50 text-brand-700',
    slate: 'bg-slate-100 text-slate-600',
    amber: 'bg-gold-100 text-gold-700',
    gold: 'bg-gold-100 text-gold-700',
    red: 'bg-red-50 text-red-700',
    blue: 'bg-navy-50 text-navy-700',
    violet: 'bg-violet-50 text-violet-700',
    green: 'bg-emerald-50 text-emerald-700',
  }
  return <span className={`badge ${colors[color] || colors.brand}`}>{children}</span>
}

export function AiChip({ source }) {
  const openai = source === 'OPENAI' || source === 'openai'
  return (
    <span className="badge bg-violet-50 text-violet-700">
      <IconSparkles size={13} /> {openai ? 'ذكاء اصطناعي · OpenAI' : 'ذكاء اصطناعي · محرّك ذكي'}
    </span>
  )
}

export function SectionTitle({ children, icon: Ico, action }) {
  return (
    <div className="mb-3 flex items-center justify-between gap-3">
      <h2 className="flex items-center gap-2.5 text-lg font-extrabold text-navy-800">
        {Ico && (
          <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-brand-50 text-brand-600">
            <Ico size={19} />
          </span>
        )}
        {children}
      </h2>
      {action}
    </div>
  )
}

export function ScoreRing({ score = 0, size = 108 }) {
  const r = (size - 18) / 2
  const c = 2 * Math.PI * r
  const v = Math.max(0, Math.min(100, score))
  const offset = c - (v / 100) * c
  const color = v >= 80 ? '#2F8F5B' : v >= 65 ? '#C9A24B' : v >= 50 ? '#DD8463' : '#C25A33'
  return (
    <svg width={size} height={size} className="-rotate-90" role="img" aria-label={`المؤشر ${Math.round(v)} من 100`}>
      <circle cx={size / 2} cy={size / 2} r={r} stroke="#EDE4D7" strokeWidth="11" fill="none" />
      <circle cx={size / 2} cy={size / 2} r={r} stroke={color} strokeWidth="11" fill="none"
        strokeDasharray={c} strokeDashoffset={offset} strokeLinecap="round"
        style={{ transition: 'stroke-dashoffset .8s cubic-bezier(.22,.8,.36,1)' }} />
      <text x="50%" y="50%" dy="0.35em" textAnchor="middle" className="rotate-90 num"
        style={{ transformOrigin: 'center', fontWeight: 800, fontSize: '1.55rem', fill: '#15243F' }}>
        {Math.round(v)}
      </text>
    </svg>
  )
}

export function Empty({ title = 'لا شيء هنا بعد', icon: Ico, children }) {
  return (
    <div className="card flex flex-col items-center justify-center py-12 text-center text-slate-400">
      {Ico && <span className="mb-2 flex h-12 w-12 items-center justify-center rounded-2xl bg-slate-100 text-slate-400"><Ico size={24} /></span>}
      <div className="font-bold text-slate-600">{title}</div>
      {children && <div className="mt-1 text-sm">{children}</div>}
    </div>
  )
}
