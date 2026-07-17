import { useEffect, useState } from 'react'
import api from '../lib/api'
import { Card, Badge, Loader, SectionTitle } from '../components/ui.jsx'
import { IconBulb } from '../components/icons.jsx'

export default function Quiz() {
  const [q, setQ] = useState(null)
  const [loading, setLoading] = useState(true)
  const [result, setResult] = useState(null)
  const [busy, setBusy] = useState(false)
  const [loadError, setLoadError] = useState(false)
  const [submitError, setSubmitError] = useState('')

  const load = () => {
    setLoading(true)
    setLoadError(false)
    return api.get('/questions/today').then(({ data }) => {
      setQ(data)
      if (data.answered) {
        setResult({ correct: data.answeredCorrect, correctIndex: data.correctIndex, explanation: data.explanation })
      }
    }).catch(() => setLoadError(true)).finally(() => setLoading(false))
  }
  useEffect(() => { load() }, [])

  const answer = async (index) => {
    if (q.answered || busy) return
    setBusy(true)
    setSubmitError('')
    try {
      const { data } = await api.post('/questions/answer', { index })
      setResult(data)
      window.dispatchEvent(new Event('points-changed'))
      // Update locally instead of reloading, so the "+points earned" line stays visible.
      setQ((prev) => ({ ...prev, answered: true, answeredIndex: index }))
    } catch {
      setSubmitError('تعذّر إرسال إجابتك — أعد المحاولة')
    } finally { setBusy(false) }
  }

  if (loading) return <Loader />
  if (loadError) return (
    <Card className="text-center">
      <div className="font-bold text-slate-700">تعذّر تحميل سؤال اليوم — أعد المحاولة</div>
      <button onClick={load} className="btn-primary mt-3">أعد المحاولة</button>
    </Card>
  )
  if (!q) return <Card>لا يوجد سؤال اليوم.</Card>

  const answeredIndex = q.answeredIndex

  return (
    <div className="reveal space-y-6">
      <SectionTitle icon={IconBulb}>سؤال اليوم المالي</SectionTitle>
      <p className="-mt-4 text-sm text-slate-500">60 ثانية فقط. مخصص حسب دخلك وعاداتك. اكسب نقاطًا عند الإجابة الصحيحة.</p>

      <Card>
        <div className="flex items-center justify-between">
          <Badge color="amber">+{q.rewardPoints} نقطة</Badge>
          {q.answered && (result?.correct ? <Badge color="brand">إجابة صحيحة ✅</Badge> : <Badge color="red">إجابة خاطئة ❌</Badge>)}
        </div>
        <div className="mt-3 text-lg font-bold text-slate-900">{q.prompt}</div>

        <div className="mt-4 space-y-2">
          {q.options.map((opt, i) => {
            const isCorrect = result && i === result.correctIndex
            const isChosenWrong = result && i === answeredIndex && i !== result.correctIndex
            let cls = 'border-slate-200 hover:border-brand-400'
            if (isCorrect) cls = 'border-brand-500 bg-brand-50'
            else if (isChosenWrong) cls = 'border-red-400 bg-red-50'
            return (
              <button key={i} disabled={q.answered || busy} onClick={() => answer(i)}
                className={`flex w-full items-center gap-3 rounded-xl border p-3 text-start transition ${cls}`}>
                <span className="flex h-7 w-7 items-center justify-center rounded-full bg-slate-100 text-sm font-bold text-slate-600">
                  {String.fromCharCode(65 + i)}
                </span>
                <span className="text-sm font-medium text-slate-700">{opt}</span>
                {isCorrect && <span className="ms-auto">✅</span>}
                {isChosenWrong && <span className="ms-auto">❌</span>}
              </button>
            )
          })}
        </div>

        {submitError && (
          <div className="mt-4 rounded-xl bg-red-50 p-3 text-sm font-semibold text-red-600">{submitError}</div>
        )}

        {result && (
          <div className="mt-4 rounded-xl bg-slate-50 p-3 text-sm text-slate-600">
            💡 {result.explanation}
            {result.correct && result.pointsAwarded > 0 && (
              <span className="ms-1 font-semibold text-brand-600"> اكتسبت +{result.pointsAwarded} نقطة! 🎉</span>
            )}
          </div>
        )}
      </Card>
    </div>
  )
}
