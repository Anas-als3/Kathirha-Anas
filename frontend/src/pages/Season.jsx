import { useCallback, useEffect, useRef, useState } from 'react'
import api from '../lib/api'
import { num } from '../lib/format'
import { Card, Badge, Loader, SectionTitle } from '../components/ui.jsx'
import {
  IconSparkles, IconClock, IconCoins, IconStore, IconFlame, IconTrophy,
  IconMedal, IconLock, IconCheck, IconCrown, IconGift, IconBank, IconWallet,
} from '../components/icons.jsx'

const TIER_ICONS = {
  1: IconCoins, 2: IconStore, 3: IconFlame, 4: IconCoins, 5: IconGift, 6: IconMedal, 7: IconCrown,
}

const PLUS_PATHS = [
  { icon: IconBank, title: 'حوّل راتبك إلى الإنماء', desc: 'يتفعّل تلقائيًا مع أول راتب يصلك' },
  { icon: IconWallet, title: 'رصيد ادّخار 5,000 ريال فأكثر', desc: 'يبقى مفعّلًا ما دام رصيدك محفوظًا' },
  { icon: IconSparkles, title: 'اشتراك شهري', desc: '9.99 ريال شهريًا — ألغِه متى شئت' },
]

// Arabic number agreement for the countdown: يوم واحد / يومان / 3-10 أيام / 11+ يومًا
const daysLabel = (n) =>
  n === 0 ? 'ينتهي اليوم' : n === 1 ? 'يوم واحد' : n === 2 ? 'يومان' : n <= 10 ? `${n} أيام` : `${n} يومًا`

export default function Season() {
  const [pass, setPass] = useState(null)
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState(false)
  const [busyTier, setBusyTier] = useState(null)
  const [busyPlus, setBusyPlus] = useState(false)
  const [sheet, setSheet] = useState(false)
  const [toast, setToast] = useState('')
  const focusRef = useRef(null)

  const load = useCallback(() => {
    setLoading(true)
    setLoadError(false)
    api.get('/seasons/pass')
      .then(({ data }) => setPass(data))
      .catch((e) => { if (e.response?.status !== 404) setLoadError(true) })
      .finally(() => setLoading(false))
  }, [])
  useEffect(() => { load() }, [load])

  // Center the track on the player's current frontier, like games do
  useEffect(() => {
    if (!loading && focusRef.current) {
      focusRef.current.scrollIntoView({ inline: 'center', block: 'nearest' })
    }
  }, [loading])

  const say = (msg) => { setToast(msg); setTimeout(() => setToast(''), 5000) }

  const claim = async (tier) => {
    setBusyTier(tier)
    try {
      const { data } = await api.post(`/seasons/pass/claim/${tier}`)
      setPass(data.pass)
      say(data.message)
      window.dispatchEvent(new Event('points-changed'))
    } catch (e) {
      say(e.response?.data?.message || 'تعذّر الاستلام')
    } finally { setBusyTier(null) }
  }

  const activatePlus = async () => {
    setBusyPlus(true)
    try {
      const { data } = await api.post('/seasons/plus/activate')
      setPass(data.pass)
      setSheet(false)
      say(data.message)
    } catch (e) {
      say(e.response?.data?.message || 'تعذّر التفعيل')
    } finally { setBusyPlus(false) }
  }

  if (loading) return <Loader />
  if (loadError) return (
    <Card className="text-center">
      <div className="font-bold text-slate-700">تعذّر تحميل الموسم.</div>
      <button onClick={load} className="btn-primary mt-3">أعد المحاولة</button>
    </Card>
  )
  if (!pass) return <Card>لا يوجد موسم نشط حاليًا.</Card>

  const hasPlus = Array.isArray(pass.plusTiers) && pass.plusTiers.length === pass.tiers.length
  const grand = pass.tiers[pass.tiers.length - 1]
  const progress = Math.min(100, (pass.seasonalPoints / pass.grandPrizeThreshold) * 100)
  const remaining = Math.max(0, pass.grandPrizeThreshold - pass.seasonalPoints)
  const focusTier = Math.min(pass.level + 1, pass.maxLevel)

  return (
    <div className="reveal space-y-4">
      <SectionTitle icon={IconSparkles}
        action={<Badge color="violet"><IconClock size={12} /> {daysLabel(pass.daysLeft)}</Badge>}>
        {pass.seasonName}
      </SectionTitle>

      <div className="flex flex-wrap items-center gap-2">
        <span className="badge num bg-violet-50 text-violet-700"><IconSparkles size={13} /> {num(pass.seasonalPoints)} نقطة موسمية</span>
        <span className="badge num bg-gold-100 text-gold-700"><IconTrophy size={13} /> المستوى {pass.level} من {pass.maxLevel}</span>
        {hasPlus && (pass.plusActive
          ? <span className="badge bg-gold-100 text-gold-700"><IconCrown size={13} /> كثّرها+ مفعّل</span>
          : (
            <button onClick={() => setSheet(true)}
              className="badge cursor-pointer bg-navy-800 text-gold-400 transition-transform hover:scale-105">
              <IconCrown size={13} /> فعّل كثّرها+
            </button>
          ))}
      </div>

      {toast && (
        <div role="status" aria-live="polite"
          className="pop rounded-xl border border-emerald-200 bg-emerald-50 px-3.5 py-2.5 text-sm font-bold text-emerald-800">
          {toast}
        </div>
      )}

      {/* Dual reward track — plus on top, free below, level rail in the middle */}
      <div>
        <div className="mb-1.5 flex items-center justify-between px-0.5">
          {hasPlus
            ? <span className="flex items-center gap-1 text-[11px] font-extrabold text-gold-700"><IconCrown size={13} /> مسار كثّرها+</span>
            : <span className="text-[11px] font-extrabold text-slate-500">مسار المكافآت</span>}
          <span className="text-[10px] font-bold text-slate-400">اسحب لاستكشاف المستويات ←</span>
        </div>

        <div className="no-scrollbar -mx-4 flex snap-x overflow-x-auto px-3 pb-1">
          {pass.tiers.map((t, i) => {
            const p = hasPlus ? pass.plusTiers[i] : null
            const reached = pass.seasonalPoints >= t.threshold
            const isFirst = i === 0
            const isLast = i === pass.tiers.length - 1
            return (
              <div key={t.tier} ref={t.tier === focusTier ? focusRef : null}
                className="w-[118px] shrink-0 snap-center px-1">
                {p && (
                  <TierTile t={p} plus plusActive={pass.plusActive}
                    busyTier={busyTier} onClaim={claim} onLockedPlus={() => setSheet(true)} />
                )}

                {/* Level rail */}
                <div className="relative my-1.5 h-8">
                  <span className={`absolute top-1/2 h-1.5 -translate-y-1/2 rounded-full ${reached ? 'bg-gold-400' : 'bg-[#EDE4D7]'}`}
                    style={{ left: isLast ? '50%' : 0, right: isFirst ? '50%' : 0 }} />
                  <span className={`num absolute left-1/2 top-1/2 flex h-7 w-7 -translate-x-1/2 -translate-y-1/2 items-center justify-center rounded-full border-2 text-[11px] font-extrabold ${
                    reached ? 'border-gold-500 bg-gold-400 text-navy-900 shadow-md shadow-gold-500/40' : 'border-[#E5DACA] bg-white text-slate-400'}`}>
                    {t.tier}
                  </span>
                </div>

                <TierTile t={t} busyTier={busyTier} onClaim={claim} />
              </div>
            )
          })}
        </div>

        {hasPlus && <div className="mt-1.5 px-0.5 text-[11px] font-extrabold text-slate-500">المسار المجاني</div>}
      </div>

      {/* Grand prize — tier 7 */}
      <div className="relative overflow-hidden rounded-3xl p-5 text-white"
        style={{ background: 'linear-gradient(140deg, #2A0E4A, #5B2A8C)', boxShadow: '0 18px 40px -14px rgba(42,14,74,.5)' }}>
        <span className={`absolute top-3.5 left-3.5 flex h-7 w-7 items-center justify-center rounded-full ${
          grand.claimed ? 'bg-emerald-500' : 'bg-white/15'}`}>
          {grand.claimed ? <IconCheck size={14} strokeWidth={3} /> : grand.unlocked ? <IconCrown size={14} /> : <IconLock size={13} />}
        </span>
        <span className="badge bg-white/15 text-violet-100">المستوى 7 · الجائزة الكبرى</span>
        <div className="mt-2.5 flex items-center gap-2 text-xl font-extrabold"><IconCrown size={21} /> {grand.title}</div>
        <div className="mt-1 text-xs leading-relaxed text-violet-200">{grand.desc}</div>
        <div className="mt-3.5 h-2.5 overflow-hidden rounded-full bg-white/15">
          <i className="block h-full rounded-full transition-all duration-700"
            style={{ width: `${progress}%`, background: 'linear-gradient(270deg,#E8C06B,#F2CD7B)', float: 'right' }} />
        </div>
        <div className="mt-1.5 flex items-center justify-between text-[11px] font-bold">
          <span className="num text-violet-200">{num(pass.seasonalPoints)} / {num(pass.grandPrizeThreshold)} نقطة موسمية</span>
          {grand.claimed
            ? <span className="text-emerald-300">تم الاستلام 👑</span>
            : grand.unlocked
              ? <button onClick={() => claim(7)} disabled={busyTier === 7} className="btn-primary cursor-pointer px-3 py-1 text-[11px]">{busyTier === 7 ? '…' : 'استلم الجائزة 👑'}</button>
              : <span className="num text-gold-400">تبقّى {num(remaining)}</span>}
        </div>
      </div>

      <p className="text-center text-[11px] leading-relaxed text-slate-400">
        النقاط الموسمية تُكتسب من تحدّيات الموسم وسؤال اليوم — ومكافآت كثّرها+ تذهب لمحفظتك فقط، فنقاط المنافسة تبقى بالجهد وحده.
      </p>

      {/* كثّرها+ activation sheet */}
      {sheet && (
        <div className="fixed inset-0 z-50 flex items-end justify-center" role="dialog" aria-modal="true" aria-label="تفعيل كثّرها+">
          <div className="absolute inset-0 bg-navy-900/50 backdrop-blur-[2px]" onClick={() => setSheet(false)} />
          <div className="pop relative w-full max-w-[430px] rounded-t-3xl bg-white p-5 pb-7 shadow-2xl">
            <div className="mx-auto mb-4 h-1 w-10 rounded-full bg-slate-200" />
            <div className="flex items-center gap-2 text-xl font-extrabold text-navy-800">
              <span className="text-gold-500"><IconCrown size={22} /></span> كثّرها+
            </div>
            <p className="mt-1 text-sm leading-relaxed text-slate-500">
              مسار مكافآت مميّز بقيمة أعلى في كل مستوى — المكافآت تذهب لمحفظتك فقط، ونقاط المنافسة تبقى بالجهد وحده.
            </p>
            <div className="mt-4 space-y-2.5">
              {PLUS_PATHS.map((p) => (
                <div key={p.title} className="flex items-start gap-3 rounded-xl border border-[#F0E9DD] bg-[#FBF8F3] px-3.5 py-3">
                  <span className="mt-0.5 shrink-0 text-gold-600"><p.icon size={18} /></span>
                  <span className="text-sm">
                    <b className="font-extrabold text-navy-800">{p.title}</b>
                    <span className="block text-xs leading-relaxed text-slate-500">{p.desc}</span>
                  </span>
                </div>
              ))}
            </div>
            <button onClick={activatePlus} disabled={busyPlus} className="btn-primary mt-5 w-full py-3 text-sm">
              {busyPlus ? 'جارٍ التفعيل…' : 'تفعيل للعرض التجريبي 👑'}
            </button>
            <button onClick={() => setSheet(false)} className="mt-2 w-full cursor-pointer py-2 text-sm font-bold text-slate-400 hover:text-slate-600">
              إغلاق
            </button>
          </div>
        </div>
      )}
    </div>
  )
}

function TierTile({ t, plus = false, plusActive = true, busyTier, onClaim, onLockedPlus }) {
  const Ico = TIER_ICONS[t.tier % 100] || IconGift
  const lockedPlus = plus && !plusActive
  const claimable = t.unlocked && !t.claimed && !lockedPlus
  const dim = !t.unlocked || lockedPlus

  const shell = plus
    ? 'border-gold-400/50 bg-gradient-to-b from-[#FFF7E4] to-[#FAEECF]'
    : 'border-[#F0E9DD] bg-white/85 backdrop-blur'
  const ring = claimable ? (plus ? 'ring-2 ring-gold-500 shadow-lg shadow-gold-500/25' : 'ring-2 ring-brand-500 shadow-lg shadow-brand-500/20') : ''

  const body = (
    <>
      <span className={`absolute top-1.5 left-1.5 flex h-5 w-5 items-center justify-center rounded-full ${
        t.claimed ? 'bg-emerald-500 text-white'
          : lockedPlus ? 'bg-navy-800 text-gold-400'
            : t.unlocked ? (plus ? 'bg-gold-400/30 text-gold-700' : 'bg-brand-100 text-brand-700')
              : 'bg-[#EDE4D7] text-slate-400'}`}>
        {t.claimed ? <IconCheck size={11} strokeWidth={3} />
          : lockedPlus ? <IconCrown size={10} />
            : t.unlocked ? <IconSparkles size={11} /> : <IconLock size={10} />}
      </span>
      <span className={`mt-2.5 ${dim ? 'text-slate-400' : plus ? 'text-gold-700' : 'text-brand-600'}`}><Ico size={20} /></span>
      <div className={`mt-1 w-full text-[10.5px] font-extrabold leading-tight text-navy-800 ${dim ? 'opacity-60' : ''}`}>{t.title}</div>
      <div className="num mt-0.5 text-[9px] font-semibold text-slate-400">{num(t.threshold)} نقطة</div>
      <div className="mt-auto w-full pt-1">
        {claimable ? (
          <button onClick={() => onClaim(t.tier)} disabled={busyTier === t.tier}
            className="btn-primary w-full px-1 py-1 text-[10.5px]">
            {busyTier === t.tier ? '…' : 'استلم'}
          </button>
        ) : (
          <div className="text-[9px] leading-tight text-slate-400">
            {t.claimed ? 'تم الاستلام ✓' : lockedPlus ? 'خاص بمشتركي كثّرها+' : t.desc}
          </div>
        )}
      </div>
    </>
  )

  if (lockedPlus) {
    return (
      <button onClick={onLockedPlus} aria-label={`مكافأة كثّرها+: ${t.title} — فعّل كثّرها+ لفتحها`}
        className={`relative flex h-[148px] w-full cursor-pointer flex-col items-center rounded-2xl border p-2 text-center transition-transform hover:scale-[1.02] ${shell}`}>
        {body}
      </button>
    )
  }
  return (
    <div className={`relative flex h-[148px] w-full flex-col items-center rounded-2xl border p-2 text-center transition-all ${shell} ${ring} ${!t.unlocked ? 'opacity-60' : ''}`}>
      {body}
    </div>
  )
}
