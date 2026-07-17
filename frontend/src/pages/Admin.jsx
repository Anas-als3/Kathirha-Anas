import { useEffect, useState } from 'react'
import api from '../lib/api'
import { pct } from '../lib/format'
import { Card, StatCard, Loader, SectionTitle } from '../components/ui.jsx'
import { IconShield, IconCheck, IconTrendUp, IconCoins } from '../components/icons.jsx'

export default function Admin() {
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [forbidden, setForbidden] = useState(false)

  useEffect(() => {
    api.get('/admin/insights')
      .then(({ data }) => setData(data))
      .catch((e) => { if (e.response?.status === 403) setForbidden(true) })
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <Loader />
  if (forbidden) return <Card>🛡️ هذه اللوحة لفريق البنك فقط. سجّل الدخول بحساب الإدارة لعرضها.</Card>
  if (!data) return <Card>تعذّر تحميل الرؤى.</Card>

  return (
    <div className="space-y-6">
      <SectionTitle icon={IconShield}>الإدارة · رؤى المنتج بالذكاء الاصطناعي</SectionTitle>

      <div className="grid grid-cols-2 gap-4">
        <StatCard icon={IconCoins} label="إجمالي المستخدمين" value={data.totalUsers} accent="navy" />
        <StatCard icon={IconCheck} label="موثّقون بنكيًا" value={data.verifiedUsers} accent="green" />
        <StatCard icon={IconTrendUp} label="متوسط نسبة الادّخار" value={pct(data.avgSavingsRatePercent)} accent="brand" />
      </div>

      <div className="grid gap-4">
        {data.insights.map((it, i) => (
          <Card key={i}>
            <div className="flex items-start justify-between gap-2">
              <div className="font-bold text-slate-900">{it.title}</div>
              <span className="badge bg-violet-100 text-violet-700">{it.metric}</span>
            </div>
            <p className="mt-1 text-sm text-slate-600">{it.detail}</p>
          </Card>
        ))}
      </div>
      <p className="text-center text-xs text-slate-400">أُنشئت بواسطة محرك الذكاء الاصطناعي في كثّرها من بيانات المستخدمين المباشرة.</p>
    </div>
  )
}
