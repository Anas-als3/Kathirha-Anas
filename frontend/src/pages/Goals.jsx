import { useEffect, useState } from 'react'
import api from '../lib/api'
import { sar, dateShort } from '../lib/format'
import { Card, Badge, Progress, Loader, SectionTitle } from '../components/ui.jsx'
import { IconTrophy } from '../components/icons.jsx'

export default function Goals() {
  const [goals, setGoals] = useState([])
  const [loading, setLoading] = useState(true)
  const [form, setForm] = useState({ name: '', targetAmount: '', targetDate: '' })
  const [creating, setCreating] = useState(false)
  const [createError, setCreateError] = useState('')
  const [goalError, setGoalError] = useState(null) // { goalId, message }
  const [rescue, setRescue] = useState(null) // { goalId, data }
  const [busy, setBusy] = useState(false)

  const load = () => api.get('/goals').then(({ data }) => setGoals(data)).finally(() => setLoading(false))
  useEffect(() => { load() }, [])

  const create = async (e) => {
    e.preventDefault()
    setCreating(true)
    setCreateError('')
    try {
      await api.post('/goals', {
        name: form.name,
        targetAmount: Number(form.targetAmount),
        targetDate: form.targetDate,
      })
      setForm({ name: '', targetAmount: '', targetDate: '' })
      await load()
    } catch (e) {
      setCreateError(e.response?.data?.message || 'تعذّر إنشاء الهدف — تحقّق من البيانات وأعد المحاولة')
    } finally { setCreating(false) }
  }

  const openRescue = async (id) => {
    setBusy(true)
    try {
      const { data } = await api.post(`/goals/${id}/rescue`)
      setRescue({ goalId: id, data })
    } finally { setBusy(false) }
  }

  const applyRescue = async (option) => {
    setBusy(true)
    try {
      await api.post(`/goals/${rescue.goalId}/rescue/apply`, { option })
      setRescue(null)
      await load()
    } finally { setBusy(false) }
  }

  const contribute = async (id) => {
    const input = prompt('كم تودّ إضافته لهذا الهدف؟ (ريال)', '500')
    if (input === null) return // cancelled
    const amount = Number(input)
    if (!Number.isFinite(amount) || amount <= 0) return
    setGoalError(null)
    try {
      await api.post(`/goals/${id}/contribute`, { amount })
      await load()
    } catch (e) {
      setGoalError({ goalId: id, message: e.response?.data?.message || 'تعذّر إضافة المدّخرات — أعد المحاولة' })
    }
  }

  if (loading) return <Loader />

  return (
    <div className="reveal space-y-6">
      <SectionTitle icon={IconTrophy}>أهدافي الادّخارية</SectionTitle>

      <Card>
        <div className="text-sm font-semibold text-slate-500">أنشئ هدفًا — والذكاء الاصطناعي يُعدّ لك خطة ادّخار واضحة</div>
        <form onSubmit={create} className="mt-3 grid grid-cols-2 gap-3">
          <input className="input col-span-2" placeholder="مثال: لابتوب جديد" required
            value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
          <input className="input" type="number" placeholder="المبلغ المستهدف (ريال)" required min="1"
            value={form.targetAmount} onChange={(e) => setForm({ ...form, targetAmount: e.target.value })} />
          <input className="input" type="date" required
            value={form.targetDate} onChange={(e) => setForm({ ...form, targetDate: e.target.value })} />
          <button className="btn-primary col-span-2" disabled={creating}>
            {creating ? 'جارٍ إعداد الخطة…' : '✨ أنشئ الهدف بخطة ذكية'}
          </button>
        </form>
        {createError && <div className="mt-2 text-sm font-semibold text-red-600">{createError}</div>}
      </Card>

      <div className="grid gap-4">
        {goals.map((g) => (
          <Card key={g.id}>
            <div className="flex items-start justify-between">
              <div className="font-bold text-slate-900">{g.name}</div>
              {g.status === 'BEHIND' ? <Badge color="red">متأخر</Badge>
                : g.status === 'COMPLETED' ? <Badge color="brand">مكتمل 🎉</Badge>
                : <Badge color="brand">على المسار</Badge>}
            </div>
            <div className="mt-2 text-sm text-slate-500">
              {sar(g.currentAmount)} من {sar(g.targetAmount)} · بحلول {dateShort(g.targetDate)}
            </div>
            <div className="mt-1.5"><Progress value={g.progressPercent} /></div>

            <div className="mt-3 grid grid-cols-3 gap-2 text-center text-xs">
              <Plan label="شهريًا" value={sar(g.monthlySaving)} />
              <Plan label="أسبوعيًا" value={sar(g.weeklySaving)} />
              <Plan label="المخاطرة" value={g.riskLevel} />
            </div>
            {g.strategy && <p className="mt-3 rounded-lg bg-slate-50 p-2.5 text-xs text-slate-500">{g.strategy}</p>}

            {goalError?.goalId === g.id && (
              <div className="mt-2 text-sm font-semibold text-red-600">{goalError.message}</div>
            )}
            <div className="mt-3 flex gap-2">
              <button onClick={() => contribute(g.id)} className="btn-ghost flex-1 text-sm">＋ أضف مدّخرات</button>
              {g.status === 'BEHIND' && (
                <button onClick={() => openRescue(g.id)} disabled={busy} className="btn-primary flex-1 text-sm">
                  🛟 أنقذ هدفك
                </button>
              )}
            </div>
          </Card>
        ))}
        {goals.length === 0 && <div className="text-sm text-slate-400">لا توجد أهداف بعد — أنشئ هدفك الأول من الأعلى.</div>}
      </div>

      {rescue && (
        <div className="fixed inset-0 z-30 flex items-center justify-center bg-black/40 p-4" onClick={() => setRescue(null)}>
          <div className="w-full max-w-md rounded-2xl bg-white p-6 shadow-xl" onClick={(e) => e.stopPropagation()}>
            <div className="text-lg font-bold text-slate-900">إنقاذ الهدف 🛟</div>
            <p className="mt-2 text-sm text-slate-600">{rescue.data.message}</p>
            <div className="mt-4 space-y-3">
              <button onClick={() => applyRescue('EXTEND')} disabled={busy}
                className="w-full rounded-xl border border-slate-200 p-3 text-start hover:border-brand-400">
                <div className="font-semibold text-slate-800">1️⃣ {rescue.data.extend.label}</div>
                <div className="text-sm text-slate-500">{rescue.data.extend.detail}</div>
              </button>
              <button onClick={() => applyRescue('INCREASE')} disabled={busy}
                className="w-full rounded-xl border border-slate-200 p-3 text-start hover:border-brand-400">
                <div className="font-semibold text-slate-800">2️⃣ {rescue.data.increase.label}</div>
                <div className="text-sm text-slate-500">{rescue.data.increase.detail}</div>
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

function Plan({ label, value }) {
  return (
    <div className="rounded-lg bg-slate-50 p-2">
      <div className="text-slate-400">{label}</div>
      <div className="font-bold text-slate-800">{value}</div>
    </div>
  )
}
