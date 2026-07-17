import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import api from '../lib/api'
import { sar, pct, num } from '../lib/format'
import { Card, StatCard, Progress, Badge, AiChip, ScoreRing, Loader } from '../components/ui.jsx'
import {
  IconBank, IconCoins, IconTrendUp, IconWallet, IconTarget, IconTrophy,
  IconMedal, IconCheck, IconSparkles, IconBolt, IconPodium, IconCrown,
} from '../components/icons.jsx'

export default function Dashboard() {
  const [d, setD] = useState(null)
  const [loading, setLoading] = useState(true)

  const load = () => {
    setLoading(true)
    api.get('/dashboard').then(({ data }) => setD(data)).catch(() => {}).finally(() => setLoading(false))
  }
  useEffect(() => { load() }, [])

  if (loading) return <Loader />
  if (!d) return (
    <Card className="text-center">
      <div className="font-bold text-slate-700">تعذّر تحميل لوحتك.</div>
      <button onClick={load} className="btn-primary mt-3">أعد المحاولة</button>
    </Card>
  )

  const firstName = (d.user?.displayName || 'يا مدّخر').split(' ')[0]
  const capPct = Math.min(100, (Number(d.savingsRatePercent) / 40) * 100)

  return (
    <div className="reveal space-y-6">
      {/* HERO — the deck's navy signature card */}
      <section className="hero-navy">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <h1 className="text-2xl font-extrabold">أهلًا {firstName} 👋</h1>
            <div className="mt-2 flex flex-wrap items-center gap-2 text-xs">
              {d.user?.bankVerified && (
                <Link to="/bank/connect">
                  <span className="badge border border-white/15 bg-white/5 text-emerald-200"><IconCheck size={13} /> حساب موثّق</span>
                </Link>
              )}
              {d.leagueLabel && (
                <span className="badge border border-gold-400/40 bg-gold-400/10 text-gold-400"><IconMedal size={13} /> {d.leagueLabel}</span>
              )}
              <span className="badge border border-white/15 bg-white/5 text-white/75"><IconSparkles size={13} /> {d.aiProvider === 'openai' ? 'ذكاء اصطناعي · OpenAI' : 'ذكاء اصطناعي · محرّك ذكي'}</span>
            </div>
          </div>
          <Link to="/leaderboard" className="group rounded-2xl border border-white/15 bg-black/15 px-4 py-2.5 text-center shadow-[inset_0_1px_0_rgba(255,255,255,.08)] transition-colors hover:bg-black/25">
            <div className="num text-2xl font-extrabold text-gold-400">#{d.leaderboardRank}</div>
            <div className="text-[11px] font-bold text-white/60">من {d.leaderboardPlayers} في دوريك</div>
          </Link>
        </div>

        <div className="hero-panel mt-5">
          <div className="flex flex-wrap items-center justify-between gap-2 text-sm">
            <span className="font-bold text-white/85">
              ادّخار هذا الشهر: <b className="num text-white">{sar(d.monthlySaved)}</b> · <b className="num text-gold-400">{pct(d.savingsRatePercent)}</b> من دخلك
            </span>
            <span className="num text-xs font-extrabold text-gold-400">السقف: 40%</span>
          </div>
          <div className="mt-2.5"><Progress value={capPct} dark /></div>
          <div className="mt-2 text-[11px] font-semibold text-white/55">
            الادّخار يمنح نقاطًا حتى 40% من دخلك — ونقاطك تُكتسب ولا تُنقَص عند الصرف.
          </div>
        </div>
      </section>

      {/* Quick actions */}
      <div className="grid grid-cols-3 gap-2.5">
        <Link to="/quiz" className="tile-grad bg-gradient-to-br from-brand-500 to-brand-700 shadow-lg shadow-brand-600/30">
          <IconBolt size={19} />
          <span className="text-[11px] font-extrabold">سؤال اليوم</span>
        </Link>
        <Link to="/goals" className="tile-grad bg-gradient-to-br from-gold-400 to-gold-700 shadow-lg shadow-gold-500/30">
          <IconTrophy size={19} />
          <span className="text-[11px] font-extrabold">أهدافي</span>
        </Link>
        <Link to="/cards" className="tile-grad bg-gradient-to-br from-navy-700 to-navy-900 shadow-lg shadow-navy-800/30">
          <IconCrown size={19} />
          <span className="text-[11px] font-extrabold">بطاقاتي</span>
        </Link>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-2 gap-4">
        <StatCard icon={IconBank} label="دخلك الشهري" value={sar(d.monthlyIncome)} accent="navy" />
        <StatCard icon={IconWallet} label="ادّخارك الشهري" value={sar(d.monthlySaved)} accent="green" />
        <StatCard icon={IconTrendUp} label="نسبة الادّخار" value={pct(d.savingsRatePercent)} sub="تمنح نقاطًا حتى 40% من دخلك" accent="brand" />
        <StatCard icon={IconCoins} label="نقاطك" value={num(d.normalPoints)} sub={`${num(d.seasonalPoints)} نقطة موسمية`} accent="gold" />
      </div>

      {/* Health + personality */}
      <div className="grid gap-4">
        <Card>
          <div className="text-sm font-bold text-slate-500">مؤشر صحة الادّخار</div>
          <div className="mt-3 flex flex-col items-center gap-2 text-center">
            <ScoreRing score={d.healthScore?.score ?? 0} />
            <div className="text-lg font-extrabold text-navy-800">{d.healthScore?.grade}</div>
            <p className="text-xs leading-relaxed text-slate-500">{d.healthScore?.summary}</p>
          </div>
          <div className="mt-4 space-y-2.5">
            {d.healthScore?.factors?.map((f) => (
              <div key={f.name}>
                <div className="mb-1 flex justify-between text-xs font-semibold text-slate-500">
                  <span>{f.name} <span className="text-slate-300">· {f.weightPercent}%</span></span>
                  <span className="num font-extrabold text-navy-700">{f.score}</span>
                </div>
                <Progress value={f.score} coral />
              </div>
            ))}
          </div>
        </Card>

        <Card>
          <div className="flex items-center justify-between">
            <div className="text-sm font-bold text-slate-500">شخصيتك في الإنفاق</div>
            <AiChip source={d.aiProvider} />
          </div>
          <div className="mt-2 text-2xl font-extrabold text-navy-800">{d.personality?.label}</div>
          <p className="mt-1 leading-relaxed text-slate-600">{d.personality?.reason}</p>

          <div className="mt-4 flex items-start gap-3 rounded-xl border border-brand-100 bg-brand-50/70 p-3.5 text-sm text-brand-800">
            <span className="mt-0.5 shrink-0 text-brand-500"><IconBolt size={17} /></span>
            <span><b>نصيحة ادّخار:</b> {d.savingInsight}</span>
          </div>

          {d.anomalies?.length > 0 && (
            <div className="mt-3 flex items-start gap-3 rounded-xl border border-gold-400/30 bg-gold-100/60 p-3.5 text-sm text-gold-700">
              <span className="mt-0.5 shrink-0"><IconTrendUp size={17} /></span>
              <span><b>ملاحظة:</b> {d.anomalies[0].message}</span>
            </div>
          )}
        </Card>
      </div>

      {/* Spending breakdown + cashback */}
      <div className="grid gap-4">
        <Card>
          <div className="text-sm font-bold text-slate-500">أين تذهب أموالك؟ (شهريًا)</div>
          <div className="mt-4 space-y-3.5">
            {d.breakdown?.slice(0, 7).map((c) => (
              <div key={c.category}>
                <div className="mb-1 flex items-center justify-between text-sm">
                  <span className="font-bold text-navy-700">{c.emoji} {c.label}</span>
                  <span className="num text-xs font-semibold text-slate-500">{sar(c.amount)} · {pct(c.sharePercent, 0)}</span>
                </div>
                <Progress value={c.sharePercent} />
              </div>
            ))}
          </div>
        </Card>

        <Card className="card-hover">
          <div className="text-sm font-bold text-slate-500">بطاقة الاسترداد المقترحة</div>
          <div className="mt-3 text-3xl">{d.cashback?.emoji}</div>
          <div className="mt-1 text-lg font-extrabold text-navy-800">{d.cashback?.cardName}</div>
          <div className="num mt-1 text-sm font-extrabold text-brand-600">
            نحو {sar(d.cashback?.estimatedMonthlySaving)} شهريًا
          </div>
          <p className="mt-2 text-xs leading-relaxed text-slate-500">{d.cashback?.reason}</p>
        </Card>
      </div>

      {/* Missions + goal */}
      <div className="grid gap-4">
        <Card>
          <div className="mb-3 flex items-center justify-between">
            <div className="flex items-center gap-2 text-sm font-bold text-slate-500"><IconTarget size={16} /> مهامك النشطة</div>
            <Link to="/missions" className="text-sm font-extrabold text-brand-600 hover:text-brand-700">عرض الكل ←</Link>
          </div>
          <div className="space-y-2">
            {d.activeMissions?.slice(0, 3).map((m) => (
              <div key={m.id} className="flex items-center justify-between gap-3 rounded-xl border border-[#F0E9DD] bg-[#FBF8F3] px-3.5 py-2.5">
                <div className="min-w-0 text-sm">
                  <div className="truncate font-bold text-navy-800">{m.title}</div>
                </div>
                <Badge color="gold"><span className="num">+{m.rewardPoints}</span></Badge>
              </div>
            ))}
            {!d.activeMissions?.length && <div className="text-sm text-slate-400">لا مهام نشطة الآن.</div>}
          </div>
        </Card>

        <Card>
          <div className="mb-3 flex items-center justify-between">
            <div className="flex items-center gap-2 text-sm font-bold text-slate-500"><IconTrophy size={16} /> هدفك الأول</div>
            <Link to="/goals" className="text-sm font-extrabold text-brand-600 hover:text-brand-700">إدارة الأهداف ←</Link>
          </div>
          {d.topGoal ? (
            <div>
              <div className="flex items-center justify-between">
                <div className="font-extrabold text-navy-800">{d.topGoal.name}</div>
                {d.topGoal.status === 'BEHIND'
                  ? <Badge color="red">متأخر</Badge>
                  : <Badge color="green">على المسار</Badge>}
              </div>
              <div className="num mt-2 text-sm font-semibold text-slate-500">
                {sar(d.topGoal.currentAmount)} من {sar(d.topGoal.targetAmount)}
              </div>
              <div className="mt-2"><Progress value={d.topGoal.progressPercent} coral /></div>
              <div className="num mt-2 text-xs text-slate-500">خطتك: {sar(d.topGoal.monthlySaving)} شهريًا · {sar(d.topGoal.weeklySaving)} أسبوعيًا</div>
            </div>
          ) : (
            <div className="text-sm text-slate-400">لا أهداف بعد. <Link to="/goals" className="font-extrabold text-brand-600">أنشئ هدفك الأول ←</Link></div>
          )}
        </Card>
      </div>
    </div>
  )
}
