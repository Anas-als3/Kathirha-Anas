import { useEffect, useState } from 'react'
import api from '../lib/api'
import { sar } from '../lib/format'
import { Card, Badge, Loader, SectionTitle, Empty } from '../components/ui.jsx'
import { IconTarget, IconCheck } from '../components/icons.jsx'

const DIFF_COLOR = { EASY: 'green', MEDIUM: 'amber', HARD: 'red' }
// Arabic labels for raw enum values coming from the API (do not translate the enum constants themselves)
const DIFF_LABEL = { EASY: 'سهلة', MEDIUM: 'متوسطة', HARD: 'صعبة' }
const TYPE_LABEL = {
  DAILY: 'يومية', WEEKLY: 'أسبوعية', MONTHLY: 'شهرية', PAYDAY: 'يوم الراتب', EMERGENCY: 'طوارئ',
  SURVEY: 'استبيان', LEARN: 'تعلّم', SOCIAL: 'مع الأصدقاء',
}
const TYPE_COLOR = {
  DAILY: 'brand', WEEKLY: 'brand', MONTHLY: 'brand', PAYDAY: 'gold', EMERGENCY: 'blue',
  SURVEY: 'violet', LEARN: 'blue', SOCIAL: 'green',
}
const CTA_LABEL = {
  SURVEY: 'أكملت الاستبيان', LEARN: 'أنجزتها', SOCIAL: 'أرسلت الدعوة',
}

export default function Missions() {
  const [missions, setMissions] = useState([])
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(null)
  const [toast, setToast] = useState(null)

  const load = () => api.get('/missions').then(({ data }) => setMissions(data)).finally(() => setLoading(false))
  useEffect(() => { load() }, [])

  const complete = async (id) => {
    setBusy(id)
    try {
      const { data } = await api.post(`/missions/${id}/complete`)
      setToast(`✅ +${data.pointsAwarded} نقطة! ${data.streakMessage}`)
      window.dispatchEvent(new Event('points-changed'))
      await load()
    } catch (e) {
      setToast(e.response?.data?.message || 'تعذّر إكمال المهمة')
    } finally { setBusy(null); setTimeout(() => setToast(null), 6000) }
  }

  const generate = async () => {
    setBusy('gen')
    try { await api.post('/missions/generate'); await load() }
    catch { setToast('تعذّر إنشاء المهام — أعد المحاولة'); setTimeout(() => setToast(null), 6000) }
    finally { setBusy(null) }
  }

  if (loading) return <Loader />
  const active = missions.filter((m) => m.status === 'ACTIVE')
  const done = missions.filter((m) => m.status === 'COMPLETED')

  return (
    <div className="reveal space-y-6">
      {toast && <div className="rounded-xl bg-brand-600 px-4 py-3 text-sm font-semibold text-white shadow-lg">{toast}</div>}

      <SectionTitle action={<button onClick={generate} disabled={busy === 'gen'} className="btn-ghost text-sm">
        {busy === 'gen' ? 'جارٍ الإنشاء…' : '🤖 أنشئ المزيد'}</button>}>
        مهام الذكاء الاصطناعي 🎯
      </SectionTitle>
      <p className="-mt-4 text-sm text-slate-500">مصمّمة لك من إنفاقك الفعلي، والمهام الأصعب تمنحك نقاطًا أكثر.</p>

      {active.length === 0 ? (
        <Empty title="لا توجد مهام نشطة">اضغط «أنشئ المزيد» للحصول على مهام جديدة من الذكاء الاصطناعي.</Empty>
      ) : (
        <div className="grid grid-cols-2 gap-3">
          {active.map((m) => (
            <Card key={m.id} className="flex flex-col justify-between">
              <div>
                <div className="flex items-start justify-between gap-2">
                  <div className="font-bold text-slate-900">{m.title}</div>
                  <Badge color={DIFF_COLOR[m.difficulty]}>{DIFF_LABEL[m.difficulty] || m.difficulty}</Badge>
                </div>
                <p className="mt-1 text-sm text-slate-600">{m.description}</p>
                <div className="mt-3 flex flex-wrap gap-2 text-xs">
                  <Badge color={TYPE_COLOR[m.type] || 'slate'}>{TYPE_LABEL[m.type] || m.type}</Badge>
                  {m.targetAmount > 0 && <Badge color="slate"><span className="num">{sar(m.targetAmount)}</span></Badge>}
                  <Badge color="gold"><span className="num">+{m.rewardPoints}</span> نقطة</Badge>
                </div>
              </div>
              <button onClick={() => complete(m.id)} disabled={busy === m.id} className="btn-primary mt-4">
                {busy === m.id ? 'جارٍ الحفظ…' : (CTA_LABEL[m.type] || 'ادّخرت المبلغ')}
              </button>
            </Card>
          ))}
        </div>
      )}

      {done.length > 0 && (
        <div>
          <SectionTitle icon={IconCheck}>مهام مكتملة</SectionTitle>
          <div className="space-y-2">
            {done.map((m) => (
              <div key={m.id} className="flex items-center justify-between rounded-xl bg-white px-4 py-3 text-sm shadow-sm">
                <span className="font-medium text-slate-500 line-through">{m.title}</span>
                <Badge color="brand">+{m.rewardPoints}</Badge>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}
