import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../lib/api'
import { useAuth } from '../lib/auth.jsx'
import Logo from '../components/Logo.jsx'
import { IconScale, IconSparkles, IconMoon, IconChat, IconBolt } from '../components/icons.jsx'

const FEATURES = [
  { icon: IconScale, title: 'لوحة صدارة عادلة', desc: 'تنافس بنقاط تكسبها بجهدك — والادّخار يمنحها حتى 40% من دخلك، لا حجم ثروتك.' },
  { icon: IconSparkles, title: 'مدرّب ذكي على بياناتك', desc: 'مهام وأسئلة مالية مخصصة من إنفاقك الفعلي.' },
  { icon: IconMoon, title: 'وفق مبادئ الشريعة', desc: 'لا ربا، ولا ميسر، ولا غرر — بالتصميم.' },
  { icon: IconChat, title: 'يصلك على WhatsApp', desc: 'التنبيهات والأسئلة وأكواد المكافآت.' },
]

export default function Login() {
  const { login, token } = useAuth()
  const navigate = useNavigate()
  const [phone, setPhone] = useState('+966500000001')
  const [password, setPassword] = useState('demo1234')
  const [loading, setLoading] = useState(false)
  const [demoLoading, setDemoLoading] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => { if (token) navigate('/') }, [token, navigate])

  const doLogin = async (e) => {
    e.preventDefault()
    setError(''); setLoading(true)
    try {
      const { data } = await api.post('/auth/login', { phone, password })
      login(data.token, data.user)
      navigate('/')
    } catch (err) {
      setError(err.response?.data?.message || 'تعذّر تسجيل الدخول')
    } finally { setLoading(false) }
  }

  const instantDemo = async () => {
    setError(''); setDemoLoading(true)
    try {
      const { data } = await api.post('/demo/seed')
      login(data.token, data.user)
      navigate('/')
    } catch (err) {
      setError(err.response?.data?.message || 'تعذّر بدء العرض التجريبي')
    } finally { setDemoLoading(false) }
  }

  return (
    <div className="grid min-h-screen lg:grid-cols-2">
      {/* Brand panel */}
      <div className="relative hidden flex-col justify-between overflow-hidden p-12 text-white lg:flex"
        style={{ background: 'linear-gradient(160deg, #122642 0%, #1E3A5F 55%, #A34728 160%)' }}>
        <div aria-hidden className="pointer-events-none absolute inset-0"
          style={{ background: 'radial-gradient(560px 420px at 18% -10%, rgba(221,132,99,.28), transparent 68%), radial-gradient(480px 380px at 105% 100%, rgba(201,162,75,.25), transparent 66%)' }} />
        <div className="relative flex items-center gap-3">
          <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-white/12 shadow-lg shadow-black/20">
            <Logo size={44} variant="cream" />
          </div>
          <div>
            <div className="text-2xl font-extrabold">كثّرها</div>
            <div className="text-sm font-semibold text-white/60">نمِّ مدّخراتك… واربح بها</div>
          </div>
        </div>

        <div className="relative">
          <h1 className="max-w-md text-4xl font-extrabold leading-[1.3]">
            حوّلنا الادّخار إلى <span className="text-gold-400">لعبة</span> تستحق الفوز.
          </h1>
          <div className="mt-8 space-y-4">
            {FEATURES.map((f) => (
              <div key={f.title} className="flex items-start gap-3.5">
                <span className="mt-0.5 flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-white/10 text-gold-400">
                  <f.icon size={19} />
                </span>
                <div>
                  <div className="font-extrabold">{f.title}</div>
                  <div className="text-sm leading-relaxed text-white/60">{f.desc}</div>
                </div>
              </div>
            ))}
          </div>
        </div>

        <div className="relative text-sm font-semibold text-white/40">صُنع للسعودية · مهمة الادّخار في رؤية 2030</div>
      </div>

      {/* Auth panel */}
      <div className="flex items-center justify-center p-6">
        <div className="reveal w-full max-w-sm">
          <div className="mb-8 lg:hidden">
            <div className="flex items-center gap-2.5">
              <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-gradient-to-br from-brand-50 to-gold-100">
                <Logo size={38} />
              </div>
              <div>
                <div className="text-xl font-extrabold text-navy-800">كثّرها</div>
                <div className="-mt-0.5 text-sm font-bold text-brand-600">نمِّ مدّخراتك… واربح بها</div>
              </div>
            </div>
          </div>

          <h2 className="text-2xl font-extrabold text-navy-800">أهلًا بعودتك</h2>
          <p className="mt-1 text-sm text-slate-500">سجّل الدخول، أو انتقل مباشرة إلى العرض التجريبي للتحكيم.</p>

          <button onClick={instantDemo} disabled={demoLoading}
            className="btn-primary mt-6 w-full py-3 text-base">
            <IconBolt size={18} />
            {demoLoading ? 'نجهّز عرضك التجريبي…' : 'عرض تجريبي فوري (بنقرة واحدة)'}
          </button>
          <div className="my-6 flex items-center gap-3 text-xs font-semibold text-slate-400">
            <div className="h-px flex-1 bg-[#E4DACB]" /> أو سجّل الدخول <div className="h-px flex-1 bg-[#E4DACB]" />
          </div>

          <form onSubmit={doLogin} className="space-y-3.5">
            <div>
              <label htmlFor="phone" className="text-sm font-bold text-navy-700">رقم الجوال</label>
              <input id="phone" type="tel" autoComplete="tel" dir="ltr" className="input num mt-1.5 text-start"
                value={phone} onChange={(e) => setPhone(e.target.value)} />
            </div>
            <div>
              <label htmlFor="password" className="text-sm font-bold text-navy-700">كلمة المرور</label>
              <input id="password" type="password" autoComplete="current-password" className="input mt-1.5"
                value={password} onChange={(e) => setPassword(e.target.value)} />
            </div>
            {error && <div role="alert" className="rounded-xl border border-red-100 bg-red-50 px-3.5 py-2.5 text-sm font-semibold text-red-600">{error}</div>}
            <button type="submit" disabled={loading} className="btn-ghost w-full">
              {loading ? 'جارٍ تسجيل الدخول…' : 'تسجيل الدخول'}
            </button>
          </form>

          <div className="mt-8 rounded-2xl border border-[#EDE4D7] bg-white/60 p-4 text-xs text-slate-500">
            <div className="font-extrabold text-navy-700">حسابات تجريبية</div>
            <div className="num mt-1.5">مستخدم · <code dir="ltr">+966500000001</code> / <code>demo1234</code> (أنس)</div>
            <div className="num mt-0.5">الإدارة · <code>admin</code> / <code>admin1234</code></div>
          </div>
        </div>
      </div>
    </div>
  )
}
