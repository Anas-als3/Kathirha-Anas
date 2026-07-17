import { useEffect, useRef, useState } from 'react'
import api from '../lib/api'
import { useAuth } from '../lib/auth.jsx'
import { Card, Loader, SectionTitle } from '../components/ui.jsx'
import { IconChat } from '../components/icons.jsx'

const CAT_LABEL = {
  OTP: 'رمز التحقق', WELCOME: 'ترحيب', DAILY_QUESTION: 'سؤال اليوم',
  MISSION_NUDGE: 'مهمة', GOAL_RESCUE: 'إنقاذ الهدف', COUPON: 'قسيمة', GENERAL: '',
}

export default function WhatsApp() {
  const { user, login } = useAuth()
  const [messages, setMessages] = useState([])
  const [loading, setLoading] = useState(true)
  const [text, setText] = useState('')
  const [busy, setBusy] = useState(false)
  const [number, setNumber] = useState(user?.phone || '')
  const [savingNum, setSavingNum] = useState(false)
  const [sendingQ, setSendingQ] = useState(false)
  const [notice, setNotice] = useState('')
  const endRef = useRef(null)

  const load = () => api.get('/whatsapp').then(({ data }) => setMessages(data)).finally(() => setLoading(false))
  useEffect(() => { load() }, [])
  useEffect(() => { endRef.current?.scrollIntoView({ behavior: 'smooth' }) }, [messages])
  useEffect(() => { if (user?.phone) setNumber(user.phone) }, [user?.phone])

  const send = async (e) => {
    e.preventDefault()
    if (!text.trim()) return
    setBusy(true)
    try {
      const { data } = await api.post('/whatsapp/reply', { body: text.trim() })
      setMessages(data.messages)
      setText('')
    } catch {
      setNotice('تعذّر الإرسال — أعد المحاولة')
    } finally { setBusy(false) }
  }

  const saveNumber = async () => {
    setSavingNum(true); setNotice('')
    try {
      const { data } = await api.post('/auth/phone', { phone: number.trim() })
      login(data.token, data.user) // phone change re-issues the token
      setNotice('✅ تم حفظ الرقم. الآن انضم إلى Twilio sandbox من هذا الرقم، ثم أرسل سؤال اليوم.')
    } catch (err) {
      setNotice(err.response?.data?.message || 'تعذّر حفظ الرقم')
    } finally { setSavingNum(false) }
  }

  const sendDailyQuestion = async () => {
    setSendingQ(true); setNotice('')
    try {
      const { data } = await api.post('/whatsapp/send-daily-question')
      setMessages(data)
      setNotice('📚 تم إرسال سؤال اليوم (إلى WhatsApp إذا كان Twilio مفعّلًا، ويظهر أدناه أيضًا).')
    } catch (err) {
      setNotice(err.response?.data?.message || 'تعذّر الإرسال')
    } finally { setSendingQ(false) }
  }

  if (loading) return <Loader />

  return (
    <div className="space-y-4">
      <SectionTitle icon={IconChat}>WhatsApp</SectionTitle>

      <Card>
        <div className="text-sm font-semibold text-slate-500">WhatsApp مباشر (Twilio sandbox)</div>
        <p className="mt-1 text-xs text-slate-500">
          أدخل رقمك الحقيقي، وأرسل <code dir="ltr" className="inline-block">join &lt;your-sandbox-code&gt;</code> إلى رقم Twilio sandbox عبر
          WhatsApp، ثم أرسل لنفسك سؤال اليوم. الردود (A/B/C/D أو 1/2) تصل تلقائيًا.
        </p>
        <div className="mt-3 flex flex-col gap-2">
          <input className="input" dir="ltr" placeholder="+9665XXXXXXXX" value={number}
            onChange={(e) => setNumber(e.target.value)} />
          <button className="btn-ghost whitespace-nowrap" onClick={saveNumber} disabled={savingNum}>
            {savingNum ? 'جارٍ الحفظ…' : 'حفظ الرقم'}
          </button>
          <button className="btn-primary whitespace-nowrap" onClick={sendDailyQuestion} disabled={sendingQ}>
            {sendingQ ? 'جارٍ الإرسال…' : '📚 إرسال سؤال اليوم'}
          </button>
        </div>
        {notice && <div className="mt-2 text-sm text-brand-700">{notice}</div>}
      </Card>

      <p className="text-sm text-slate-500">
        كل تنبيه ترسله كثّرها يظهر في هذا المحاكي أيضًا. ردّ بـ <b>1</b> أو <b>2</b> على رسالة إنقاذ الهدف.
      </p>

      <div className="mx-auto max-w-md overflow-hidden rounded-3xl border-4 border-slate-800 bg-[#e5ddd5] shadow-xl">
        <div className="flex items-center gap-2 bg-[#075e54] px-4 py-3 text-white">
          <div className="flex h-9 w-9 items-center justify-center rounded-full bg-white/20 font-black">ك</div>
          <div>
            <div className="text-sm font-semibold">كثّرها</div>
            <div className="text-[11px] text-white/70">متصل</div>
          </div>
        </div>

        <div className="h-[400px] space-y-2 overflow-y-auto p-3">
          {messages.map((m) => {
            const mine = m.direction === 'INBOUND'
            return (
              <div key={m.id} className={`flex ${mine ? 'justify-end' : 'justify-start'}`}>
                <div className={`max-w-[80%] rounded-lg px-3 py-2 text-sm shadow ${mine ? 'bg-[#dcf8c6]' : 'bg-white'}`}>
                  {!mine && CAT_LABEL[m.category] && (
                    <div className="mb-0.5 text-[10px] font-bold uppercase tracking-wide text-brand-600">{CAT_LABEL[m.category]}</div>
                  )}
                  <div className="whitespace-pre-line text-slate-800">{m.body}</div>
                </div>
              </div>
            )
          })}
          {messages.length === 0 && <div className="pt-10 text-center text-sm text-slate-500">لا توجد رسائل بعد.</div>}
          <div ref={endRef} />
        </div>

        <form onSubmit={send} className="flex items-center gap-2 bg-[#f0f0f0] p-2">
          <input className="flex-1 rounded-full border-0 px-4 py-2 text-sm outline-none" placeholder="اكتب ردًا… (جرّب 1 أو 2)"
            value={text} onChange={(e) => setText(e.target.value)} />
          <button disabled={busy} className="flex h-10 w-10 items-center justify-center rounded-full bg-[#075e54] text-white">
            {/* Glyph points right; flip so it points in the RTL send direction */}
            <span className="inline-block scale-x-[-1]">➤</span>
          </button>
        </form>
      </div>
    </div>
  )
}
