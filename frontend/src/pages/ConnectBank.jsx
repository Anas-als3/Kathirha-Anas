import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import api from '../lib/api'
import { sar } from '../lib/format'
import { Card, Loader, Badge, SectionTitle } from '../components/ui.jsx'
import { IconBank, IconCheck } from '../components/icons.jsx'

export default function ConnectBank() {
  const loc = useLocation()
  const navigate = useNavigate()
  const isCallback = loc.pathname.includes('/callback')

  const [status, setStatus] = useState(null)
  const [institutions, setInstitutions] = useState([])
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [result, setResult] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => { isCallback ? complete() : init() }, []) // eslint-disable-line

  async function init() {
    try {
      const { data } = await api.get('/bank/status')
      setStatus(data)
      if (data.realEnabled) {
        const ins = await api.get('/bank/institutions')
        setInstitutions(ins.data)
      }
    } catch (e) {
      setError(e.response?.data?.message || 'تعذّر تحميل حالة الربط البنكي')
    } finally { setLoading(false) }
  }

  async function startLink(id) {
    setBusy(true); setError('')
    try {
      const { data } = await api.post('/bank/link', { institutionId: id })
      localStorage.setItem('kathirha_requisition', data.id)
      window.location.href = data.link // leave to the bank's consent screen
    } catch (e) {
      setError(e.response?.data?.message || 'تعذّر إنشاء رابط الموافقة البنكية')
      setBusy(false)
    }
  }

  async function complete() {
    const reqId = localStorage.getItem('kathirha_requisition')
    if (!reqId) { setError('لا توجد موافقة بنكية قيد الانتظار.'); setLoading(false); return }
    try {
      const { data } = await api.post('/bank/complete', { requisitionId: reqId })
      setResult(data)
      localStorage.removeItem('kathirha_requisition')
    } catch (e) {
      setError(e.response?.data?.message || 'تعذّر الاستيراد — هل أكملت الموافقة على شاشة البنك؟')
    } finally { setLoading(false) }
  }

  async function mockImport() {
    setBusy(true); setError('')
    try {
      await api.post('/bank/import', { monthlyIncome: 9000, months: 3, preset: 'BALANCED' })
      navigate('/')
    } catch (e) {
      setError(e.response?.data?.message || 'تعذّر استيراد بيانات العيّنة')
    } finally { setBusy(false) }
  }

  if (loading) return <Loader label={isCallback ? 'نستورد معاملاتك…' : 'جارٍ التحميل…'} />

  // --- Callback view (after returning from the bank) ---
  if (isCallback) {
    return (
      <div className="space-y-4">
        {error
          ? <SectionTitle icon={IconBank}>ربط الحساب البنكي</SectionTitle>
          : <SectionTitle icon={IconCheck}>تم ربط حسابك</SectionTitle>}
        {error ? (
          <Card className="border-red-200 bg-red-50 text-red-700">{error}</Card>
        ) : (
          <Card>
            <div className="text-lg font-bold text-navy-800">✅ استوردنا {result?.imported} معاملة</div>
            <div className="mt-2 text-sm text-slate-600">
              دخلك الموثّق: <b>{sar(result?.baselineIncome)}</b> · تبدأ رحلتك في: <b>{result?.league || 'الدوري البرونزي'}</b>
            </div>
            <p className="mt-2 text-xs text-slate-500">
              نستخدم دخلك لأمرين فقط: سقف نقاط الادّخار (40%) وهدفك الذكي — ولا يظهر لأحد.
            </p>
            <button onClick={() => navigate('/')} className="btn-primary mt-4">إلى الرئيسية ←</button>
          </Card>
        )}
      </div>
    )
  }

  // --- Connect view ---
  return (
    <div className="space-y-4">
      <SectionTitle icon={IconBank}>اربط حسابك البنكي</SectionTitle>

      <Card>
        <div className="grid gap-3">
          <div className="rounded-xl bg-brand-50 p-3 text-sm">
            <div className="font-bold text-brand-800">🔒 وصول للقراءة فقط</div>
            <div className="mt-1 text-xs text-slate-600">نقرأ معاملاتك لنفهم إنفاقك — ولا يمكننا تحريك أموالك أبدًا.</div>
          </div>
          <div className="rounded-xl bg-brand-50 p-3 text-sm">
            <div className="font-bold text-brand-800">🌙 وفق مبادئ الشريعة</div>
            <div className="mt-1 text-xs text-slate-600">لا ربا، ولا ميسر، ولا غرر — بالتصميم.</div>
          </div>
          <div className="rounded-xl bg-brand-50 p-3 text-sm">
            <div className="font-bold text-brand-800">⚖️ عادلة بالتصميم</div>
            <div className="mt-1 text-xs text-slate-600">ترتيبك بنقاط تكسبها بجهدك — لا بحجم ثروتك.</div>
          </div>
        </div>
      </Card>

      {!status?.realEnabled ? (
        <Card>
          <Badge color="amber">بيئة العرض — بيانات عيّنة واقعية</Badge>
          <p className="mt-2 text-sm text-slate-600">
            في النسخة الكاملة يتم الربط عبر Open Banking (Tarabut · Neotek) بموافقتك.
            للعرض الآن حمّل بيانات عيّنة واقعية فورًا — وسيُكتشف راتبك تلقائيًا:
          </p>
          <button onClick={mockImport} disabled={busy} className="btn-primary mt-2">
            {busy ? 'نستورد…' : '⚡ اربط معاملاتي (بيانات عيّنة)'}
          </button>
        </Card>
      ) : (
        <Card>
          <p className="text-sm text-slate-600">
            اختر بنكك للموافقة على وصول <b>للقراءة فقط</b>. ستوافق على شاشة البنك ثم تعود هنا لاستيراد معاملاتك.
          </p>
          <div className="mt-3 grid gap-2">
            {institutions.map((b) => (
              <button key={b.id} onClick={() => startLink(b.id)} disabled={busy}
                className="flex items-center gap-3 rounded-xl border border-slate-200 p-3 text-start hover:border-brand-400">
                {b.logo ? <img src={b.logo} alt="" className="h-8 w-8 rounded" /> : <span className="text-xl">🏦</span>}
                <span className="text-sm font-semibold text-slate-700">{b.name}</span>
              </button>
            ))}
            {institutions.length === 0 && <div className="text-sm text-slate-400">لا توجد بنوك متاحة.</div>}
          </div>
        </Card>
      )}

      {error && <Card className="border-red-200 bg-red-50 text-red-700">{error}</Card>}
      <Link to="/" className="inline-block text-sm font-semibold text-brand-600">→ العودة للرئيسية</Link>
    </div>
  )
}
