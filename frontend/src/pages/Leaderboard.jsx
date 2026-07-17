import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import api from '../lib/api'
import { num } from '../lib/format'
import { Card, Badge, Progress, Loader, SectionTitle } from '../components/ui.jsx'
import { IconPodium, IconCrown, IconSparkles, IconMedal, IconScale } from '../components/icons.jsx'

export default function Leaderboard() {
  const [lb, setLb] = useState(null)
  const [explain, setExplain] = useState(null)
  const [explainError, setExplainError] = useState(null)
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    const load = () => api.get('/leaderboard')
      .then(({ data }) => setLb(data))
      .catch(() => {})
      .finally(() => setLoading(false))
    load()
    const id = setInterval(() => { if (!document.hidden) load() }, 8000)   // live refresh
    return () => clearInterval(id)
  }, [])

  const doExplain = async () => {
    setBusy(true)
    setExplainError(null)
    try { const { data } = await api.get('/leaderboard/explain'); setExplain(data) }
    catch { setExplainError('تعذّر جلب التفسير — أعد المحاولة') }
    finally { setBusy(false) }
  }

  if (loading) return <Loader />
  if (!lb) return <Card>تعذّر تحميل لوحة الصدارة.</Card>

  const top3 = lb.entries.slice(0, 3)
  const rest = lb.entries.slice(3)
  const topScore = Math.max(1, ...lb.entries.map((e) => e.scorePoints))
  // Podium visual order (RTL): #2 · #1 (center, tallest) · #3
  const podium = [top3[1], top3[0], top3[2]].filter(Boolean)

  return (
    <div className="reveal space-y-6">
      <SectionTitle icon={IconPodium}
        action={<button onClick={doExplain} disabled={busy} className="btn-ghost text-sm">
          {busy ? '…' : '🤖 لماذا هذا ترتيبي؟'}</button>}>
        لوحة الصدارة العادلة
      </SectionTitle>

      {/* League hero + podium */}
      <section className="hero-navy">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <div>
            <div className="flex items-center gap-2 text-sm font-bold text-gold-400">
              <IconMedal size={17} /> {lb.leagueLabel} · دوري إنجاز
            </div>
            <div className="mt-1 text-2xl font-extrabold">ترتيبك {lb.viewerRank} من {lb.totalPlayers}</div>
          </div>
          <div className="flex flex-col items-end gap-1.5">
            <span className="badge bg-white/10 text-emerald-300">
              <span className="relative flex h-2 w-2"><span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-emerald-400 opacity-60" /><span className="relative inline-flex h-2 w-2 rounded-full bg-emerald-400" /></span>
              مباشر
            </span>
            <span className="badge bg-white/10 text-white/85">
              <IconScale size={13} /> الترتيب بالنقاط المكتسبة — والادّخار يمنحها حتى {lb.capPercent}% من دخلك
            </span>
          </div>
        </div>

        {podium.length === 3 && (
          <div className="mt-6 flex items-end justify-center gap-3 sm:gap-5">
            {podium.map((e) => {
              const first = e.rank === 1
              const h = first ? 'h-24' : e.rank === 2 ? 'h-16' : 'h-12'
              return (
                <div key={e.rank} className="flex w-24 flex-col items-center sm:w-28">
                  {first && <span className="pop mb-1 text-gold-400"><IconCrown size={22} /></span>}
                  <div className={`flex items-center justify-center rounded-full font-extrabold ${
                    first ? 'h-14 w-14 border-2 border-gold-400 bg-gold-100 text-lg text-gold-700'
                          : 'h-11 w-11 border-2 border-white/25 bg-white/10 text-white'} ${e.currentUser ? 'ring-2 ring-brand-400' : ''}`}>
                    {(e.displayName || '؟').charAt(0)}
                  </div>
                  <div className="mt-1.5 max-w-full truncate text-xs font-bold text-white/85">{e.displayName}</div>
                  <div className={`num text-sm font-extrabold ${first ? 'text-gold-400' : 'text-white/70'}`}>{num(e.scorePoints)}</div>
                  <div className={`mt-1.5 w-full rounded-t-xl ${h} ${
                    first ? 'bg-gradient-to-b from-gold-400/60 to-gold-400/10'
                          : 'bg-gradient-to-b from-white/25 to-white/5'}`} />
                </div>
              )
            })}
          </div>
        )}
      </section>

      {explainError && (
        <Card className="border-red-200 bg-red-50 text-sm font-semibold text-red-700">{explainError}</Card>
      )}

      {explain && (
        <Card className="border-violet-200 bg-violet-50/80">
          <div className="flex items-center gap-2 font-extrabold text-violet-800"><IconSparkles size={16} /> لماذا ترتيبك {explain.rank}؟</div>
          <p className="mt-1.5 text-sm leading-relaxed text-violet-900">{explain.rankReason}</p>
          <p className="mt-2 text-sm leading-relaxed text-violet-900"><b>خطوتك التالية:</b> {explain.nextStep}</p>
          <p className="mt-2 text-xs leading-relaxed text-violet-700">{explain.capExplanation}</p>
        </Card>
      )}

      {/* Full standings */}
      <Card>
        <div className="space-y-1">
          {lb.entries.map((e) => (
            <div key={e.rank}
              className={`flex items-center gap-3 rounded-xl px-3 py-2.5 transition-colors ${
                e.currentUser ? 'bg-brand-50 ring-1 ring-brand-200' : 'hover:bg-[#FBF8F3]'}`}>
              <div className={`num flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-sm font-extrabold ${
                e.rank === 1 ? 'bg-gold-100 text-gold-700' : e.rank <= 3 ? 'bg-navy-50 text-navy-700' : 'text-slate-400'
              }`}>
                {e.rank === 1 ? <IconCrown size={15} /> : e.rank}
              </div>
              <div className="min-w-0 flex-1">
                <div className="flex flex-wrap items-center gap-2 text-sm font-bold text-navy-800">
                  <span className="truncate">{e.displayName}</span>
                  {e.currentUser && <Badge color="brand">أنت</Badge>}
                  {e.capped && <Badge color="violet">عند السقف</Badge>}
                </div>
                <div className="mt-1"><Progress value={(e.scorePoints / topScore) * 100} /></div>
              </div>
              <div className="text-start">
                <div className="num text-sm font-extrabold text-navy-800">{num(e.scorePoints)}</div>
                <div className="text-[10px] font-semibold text-slate-400">نقطة</div>
              </div>
            </div>
          ))}
        </div>
      </Card>

      {/* prize ladder — announced ranks, never draws; partner-funded at the top */}
      <Card>
        <div className="font-bold text-slate-900">🏆 جوائز الصدارة</div>
        <div className="mt-3 space-y-2 text-sm">
          <div className="flex items-center justify-between rounded-xl bg-[#FBF8F3] px-3 py-2.5">
            <span className="font-bold text-navy-800">🌙 أول الأسبوع</span>
            <span className="text-slate-600">بطاقة «هلال السبق» + قسيمة قهوة 50 ريال</span>
          </div>
          <div className="flex items-center justify-between rounded-xl bg-[#FBF8F3] px-3 py-2.5">
            <span className="font-bold text-navy-800">🥈🥉 الثاني والثالث</span>
            <span className="text-slate-600">قسيمة شريك 25 ريال</span>
          </div>
          <div className="flex items-center justify-between rounded-xl bg-[#FBF8F3] px-3 py-2.5">
            <span className="font-bold text-navy-800">📈 الأكثر تقدّمًا</span>
            <span className="text-slate-600">+100 نقطة محفظة — مكافأة الجهد الصاعد</span>
          </div>
          <div className="flex items-center justify-between rounded-xl bg-[#FBF8F3] px-3 py-2.5">
            <span className="font-bold text-navy-800">🌕 أول الشهر</span>
            <span className="text-slate-600">بطاقة «بدر التمام» + بطاقة هدايا جرير 100 ريال</span>
          </div>
        </div>
        <p className="mt-3 text-xs leading-relaxed text-slate-400">
          الجوائز تُمنح بالترتيب المعلن — لا سحوبات ولا حظ. الجوائز الكبرى بتمويل شركاء الموسم.
        </p>
      </Card>

      {/* champions' card skins — earned, never bought */}
      <Link to="/cards" className="block">
        <Card className="card-hover">
          <div className="flex items-center justify-between gap-3">
            <div>
              <div className="font-bold text-slate-900">🏆 بطاقات الأبطال</div>
              <div className="mt-0.5 text-xs text-slate-500">تصاميم حصرية باسمك تُفتح بالمراكز والدوريات — لا تُشترى أبدًا</div>
            </div>
            <span className="shrink-0 text-brand-700">←</span>
          </div>
        </Card>
      </Link>

      <p className="text-center text-xs leading-relaxed text-slate-400">
        النقاط تُكتسب بالادّخار (حتى {lb.capPercent}% من دخلك)، والمهام وسؤال اليوم والاستبيانات — ولا تُنقَص عند الصرف.
      </p>
    </div>
  )
}
