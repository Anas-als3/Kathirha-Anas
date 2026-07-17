import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import api from '../lib/api'
import { Card, Badge, Loader, SectionTitle } from '../components/ui.jsx'
import { IconGift, IconCoins } from '../components/icons.jsx'
import { SHOP_SKINS } from '../lib/skins.js'

export default function Shop() {
  const [items, setItems] = useState([])
  const [redemptions, setRedemptions] = useState([])
  const [rec, setRec] = useState(null)
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(null)
  const [coupon, setCoupon] = useState(null)
  const [loadError, setLoadError] = useState(false)

  const load = async () => {
    setLoadError(false)
    try {
      const [a, b, c] = await Promise.all([
        api.get('/shop'),
        api.get('/shop/redemptions'),
        api.get('/shop/recommendation').catch(() => ({ data: null })),
      ])
      setItems(a.data); setRedemptions(b.data); setRec(c.data)
    } catch {
      setLoadError(true)
    } finally { setLoading(false) }
  }
  useEffect(() => { load() }, [])

  const redeem = async (id) => {
    setBusy(id)
    try {
      const { data } = await api.post(`/shop/${id}/redeem`)
      setCoupon(data)
      window.dispatchEvent(new Event('points-changed'))
      await load()
    } catch (e) {
      setCoupon({ error: e.response?.data?.message || 'تعذّر الاستبدال' })
    } finally { setBusy(null) }
  }

  if (loading) return <Loader />
  if (loadError) return (
    <Card className="text-center">
      <div className="font-bold text-slate-700">تعذّر تحميل المتجر — أعد المحاولة</div>
      <button onClick={() => { setLoading(true); load() }} className="btn-primary mt-3">أعد المحاولة</button>
    </Card>
  )

  return (
    <div className="reveal space-y-6">
      <SectionTitle icon={IconGift}>متجر المكافآت</SectionTitle>

      {rec && rec.itemName && (
        <Card className="border-violet-200 bg-violet-50">
          <div className="text-sm font-semibold text-violet-800">🤖 يرشّح لك الذكاء الاصطناعي</div>
          <div className="mt-1 text-slate-700"><b>{rec.emoji} {rec.itemName}</b> — {rec.reason}</div>
        </Card>
      )}

      {coupon && (
        <Card className={coupon.error ? 'border-red-200 bg-red-50' : 'border-brand-200 bg-brand-50'}>
          {coupon.error ? <div className="text-sm text-red-600">{coupon.error}</div> : (
            <div>
              <div className="font-bold text-brand-800">🎉 حصلت على {coupon.emoji} {coupon.itemName}!</div>
              <div className="mt-1 text-sm text-slate-600">وصلتك القسيمة على <Link to="/whatsapp" className="font-bold text-emerald-700 underline">WhatsApp</Link>. الرمز:
                <span className="ms-1 rounded bg-white px-2 py-0.5 font-mono font-bold text-brand-700">{coupon.couponCode}</span>
              </div>
            </div>
          )}
        </Card>
      )}

      {/* card skins live in بطاقاتي — tease them here with real previews */}
      <Link to="/cards" className="block">
        <Card className="card-hover">
          <div className="flex items-center justify-between gap-3">
            <div>
              <div className="font-bold text-slate-900">🎨 تصاميم البطاقات</div>
              <div className="mt-0.5 text-xs text-slate-500">اصرف نقاطك على ستايل بطاقتك — وبطاقات أبطال حصرية للفائزين</div>
            </div>
            <div className="flex shrink-0 items-center">
              {Object.values(SHOP_SKINS).slice(0, 3).map((s, i) => (
                <img key={s.id} src={s.image} alt="" className="-ms-3 h-9 w-14 rounded-lg border border-white/60 object-cover shadow-md first:ms-0"
                  style={{ zIndex: 3 - i }} />
              ))}
              <span className="ms-2 text-brand-700">←</span>
            </div>
          </div>
        </Card>
      </Link>

      <div className="grid grid-cols-2 gap-3">
        {items.filter((i) => i.category !== 'SKIN').map((i) => (
          <Card key={i.id} className="flex flex-col">
            <div className="text-3xl">{i.emoji}</div>
            <div className="mt-1 font-bold text-slate-900">{i.name}</div>
            <div className="text-xs text-slate-500">{i.description}</div>
            <div className="mt-2 flex items-center gap-2">
              <Badge color={i.pointsType === 'SEASONAL' ? 'violet' : 'amber'}>
                {i.costPoints} {i.pointsType === 'SEASONAL' ? 'موسمية' : 'نقطة'}
              </Badge>
            </div>
            <button onClick={() => redeem(i.id)} disabled={!i.affordable || busy === i.id}
              className={`mt-3 ${i.affordable ? 'btn-primary' : 'btn-ghost'}`}>
              {busy === i.id ? 'جارٍ الاستبدال…' : i.affordable ? 'استبدل' : 'نقاطك لا تكفي'}
            </button>
          </Card>
        ))}
      </div>

      {redemptions.length > 0 && (
        <div>
          <SectionTitle icon={IconCoins}>مكافآتك</SectionTitle>
          <div className="space-y-2">
            {redemptions.map((r) => (
              <div key={r.id} className="flex items-center justify-between rounded-xl bg-white px-4 py-3 text-sm shadow-sm">
                <span className="font-medium text-slate-700">{r.emoji} {r.itemName}</span>
                <span className="rounded bg-slate-100 px-2 py-0.5 font-mono text-xs font-bold text-slate-600">{r.couponCode}</span>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}
