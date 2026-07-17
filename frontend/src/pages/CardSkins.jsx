import { useEffect, useMemo, useState } from 'react'
import api from '../lib/api'
import { useAuth } from '../lib/auth.jsx'
import { Card, Badge, Loader, SectionTitle } from '../components/ui.jsx'
import { IconCrown, IconGift } from '../components/icons.jsx'
import { WINNER_SKINS, SHOP_SKINS, SEASON_SKIN, DEFAULT_SKIN, leagueTier } from '../lib/skins.js'

const SELECTED_KEY = 'kathirha_skin'

/** One collectible card — the official PNG art (name printed on it), or a CSS face as fallback. */
function SkinCard({ skin, subtitle, ownerName, locked, lockText, selected, onClick, small }) {
  const ring = selected ? 'ring-2 ring-brand-500 ring-offset-2 ring-offset-[#f7f1e8]' : ''
  const lockOverlay = locked && (
    <div className="absolute inset-0 flex flex-col items-center justify-center gap-1 bg-navy-900/50 p-2 text-center backdrop-blur-[1.5px]">
      <span className="text-xl">🔒</span>
      {lockText && <span className="text-[11px] font-bold leading-snug text-white">{lockText}</span>}
    </div>
  )

  if (skin.image) {
    return (
      <button type="button" onClick={onClick} disabled={locked && !onClick}
        className={`group block w-full text-start transition-transform duration-150 ${locked ? '' : 'hover:-translate-y-0.5'}`}>
        <div className={`relative overflow-hidden rounded-2xl shadow-lg ${locked ? 'skin-locked' : ''} ${ring}`}>
          <img src={skin.image} alt={skin.name} className="block w-full" />
          {lockOverlay}
        </div>
      </button>
    )
  }

  return (
    <button type="button" onClick={onClick} disabled={locked && !onClick}
      className={`group block w-full text-start transition-transform duration-150 ${locked ? '' : 'hover:-translate-y-0.5'}`}>
      <div className={`skin-card ${skin.shine && !locked ? 'skin-shine' : ''} ${locked ? 'skin-locked' : ''} ${ring}`}
        style={{ background: skin.style.background, color: skin.style.color, border: skin.style.border }}>
        <div className="flex h-full flex-col justify-between">
          <div className="flex items-start justify-between">
            <span className={`font-extrabold ${small ? 'text-sm' : 'text-base'}`}>كثّرها</span>
            <span className={small ? 'text-lg' : 'text-2xl'}>{skin.emoji}</span>
          </div>
          <div className={`${small ? 'h-4 w-6 rounded' : 'h-6 w-9 rounded-md'} border border-white/40 bg-white/25`} />
          <div>
            <div className={`font-extrabold ${small ? 'text-[13px]' : 'text-lg'}`}>{ownerName}</div>
            {subtitle && <div className={`opacity-85 ${small ? 'text-[10px]' : 'text-xs'}`}>{subtitle}</div>}
          </div>
        </div>
        {lockOverlay}
      </div>
    </button>
  )
}

export default function CardSkins() {
  const { user } = useAuth()
  const [lb, setLb] = useState(null)
  const [pass, setPass] = useState(null)
  const [items, setItems] = useState([])
  const [redemptions, setRedemptions] = useState([])
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(null)
  const [toast, setToast] = useState(null)
  const [selectedId, setSelectedId] = useState(() => localStorage.getItem(SELECTED_KEY) || 'default')

  const load = async () => {
    try {
      const [a, b, c, d] = await Promise.all([
        api.get('/leaderboard').catch(() => ({ data: null })),
        api.get('/shop'),
        api.get('/shop/redemptions'),
        api.get('/seasons/pass').catch(() => ({ data: null })),
      ])
      setLb(a.data); setItems(b.data); setRedemptions(c.data); setPass(d.data)
    } finally { setLoading(false) }
  }
  useEffect(() => { load() }, [])

  // dev/design review: /cards?preview=1 shows every skin unlocked (visual only)
  const preview = new URLSearchParams(window.location.search).get('preview') === '1'

  const name = user?.displayName || 'مدّخر كثّرها'
  const tier = leagueTier(lb?.leagueLabel)
  const rank = lb?.viewerRank || 0
  const gate = { tier, rank }
  const subtitle = lb ? `${lb.leagueLabel} · المركز ${rank} من ${lb.totalPlayers}` : ''

  const shopSkinItems = useMemo(() => {
    const real = items.filter((i) => i.category === 'SKIN')
    if (real.length > 0 || !preview) return real
    // preview fallback before the shop is reseeded: render from the local catalog
    return Object.entries(SHOP_SKINS).map(([n, meta], idx) => ({
      id: `preview-${idx}`, name: n, description: '', costPoints: meta.cost, affordable: false, category: 'SKIN',
    }))
  }, [items, preview])
  const owned = useMemo(() => new Set(redemptions.map((r) => r.itemName)), [redemptions])

  const allSkins = useMemo(() => {
    const map = { default: DEFAULT_SKIN, [SEASON_SKIN.id]: SEASON_SKIN }
    WINNER_SKINS.forEach((s) => { map[s.id] = s })
    Object.entries(SHOP_SKINS).forEach(([n, s]) => { map[s.id] = { ...s, name: n } })
    return map
  }, [])

  const seasonUnlocked = preview || (pass && pass.level >= pass.maxLevel)
  const selected = allSkins[selectedId] || DEFAULT_SKIN

  const choose = (id) => {
    setSelectedId(id)
    localStorage.setItem(SELECTED_KEY, id)
  }

  const buy = async (item) => {
    setBusy(item.id)
    try {
      await api.post(`/shop/${item.id}/redeem`)
      window.dispatchEvent(new Event('points-changed'))
      setToast({ ok: true, text: `🎨 فُتح «${item.name}» — اضغط عليه لاعتماده` })
      await load()
    } catch (e) {
      setToast({ ok: false, text: e.response?.data?.message || 'تعذّر فتح التصميم' })
    } finally { setBusy(null) }
  }

  if (loading) return <Loader />

  return (
    <div className="reveal space-y-6">
      <SectionTitle icon={IconCrown}
        action={preview ? <Badge color="violet">وضع المعاينة — كل التصاميم مفتوحة</Badge> : null}>
        بطاقاتي
      </SectionTitle>

      {/* live preview — the chosen skin with the user's name on it */}
      <div className="mx-auto max-w-sm">
        <SkinCard skin={selected} ownerName={name} subtitle={subtitle} />
        <div className="mt-2 text-center text-xs text-slate-500">بطاقتك الحالية: <b>{selected.name}</b></div>
      </div>

      {toast && (
        <Card className={toast.ok ? 'border-brand-200 bg-brand-50' : 'border-red-200 bg-red-50'}>
          <div className={`text-sm font-bold ${toast.ok ? 'text-brand-800' : 'text-red-600'}`}>{toast.text}</div>
        </Card>
      )}

      {/* winner skins — earned by rank & league, never sold */}
      <div>
        <SectionTitle icon={IconCrown}>بطاقات الأبطال</SectionTitle>
        <div className="mb-2 text-xs text-slate-500">تُفتح بالإنجاز فقط — لا تُشترى بالمال ولا بالنقاط</div>
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
          {WINNER_SKINS.map((s) => {
            const un = preview || s.unlocked(gate)
            return (
              <SkinCard key={s.id} small skin={s} ownerName={un ? name : s.name} subtitle={s.desc}
                locked={!un} lockText={s.requirement} selected={selectedId === s.id}
                onClick={un ? () => choose(s.id) : undefined} />
            )
          })}
        </div>
      </div>

      {/* season-exclusive card — scarcity: earned via the battle pass, gone with the season */}
      <div>
        <SectionTitle icon={IconGift}
          action={<Badge color="violet">تغيب بنهاية الموسم</Badge>}>
          حصرية الموسم
        </SectionTitle>
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
          <div className="space-y-2">
            <SkinCard small skin={SEASON_SKIN} ownerName={seasonUnlocked ? name : SEASON_SKIN.name}
              subtitle={SEASON_SKIN.desc} locked={!seasonUnlocked} lockText={SEASON_SKIN.requirement}
              selected={selectedId === SEASON_SKIN.id}
              onClick={seasonUnlocked ? () => choose(SEASON_SKIN.id) : undefined} />
            {pass && !seasonUnlocked && (
              <div className="text-xs font-bold text-slate-500">تقدّمك: {pass.seasonalPoints} / {pass.grandPrizeThreshold} نقطة موسمية</div>
            )}
          </div>
        </div>
      </div>

      {/* shop skins — bought with wallet points through the real redemption flow */}
      <div>
        <SectionTitle icon={IconGift}>تصاميم بالنقاط</SectionTitle>
        <div className="mb-2 text-xs text-slate-500">تُفتح بنقاط محفظتك — وترتيبك لا يتأثر بالصرف</div>
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
          {shopSkinItems.map((item) => {
            const meta = SHOP_SKINS[item.name]
            if (!meta) return null
            const isOwned = preview || owned.has(item.name)
            return (
              <div key={item.id} className="space-y-2">
                <SkinCard small skin={meta} ownerName={isOwned ? name : item.name}
                  subtitle={item.description} locked={!isOwned}
                  lockText={isOwned ? '' : `${item.costPoints} نقطة`}
                  selected={selectedId === meta.id}
                  onClick={isOwned ? () => choose(meta.id) : undefined} />
                {isOwned ? (
                  <Badge color="green">مِلكك ✓ {selectedId === meta.id ? '· مُعتمدة' : '· اضغطها لاعتمادها'}</Badge>
                ) : (
                  <button onClick={() => buy(item)} disabled={!item.affordable || busy === item.id}
                    className={`w-full text-sm ${item.affordable ? 'btn-primary' : 'btn-ghost'}`}>
                    {busy === item.id ? 'جارٍ الفتح…' : item.affordable ? `افتحه بـ ${item.costPoints} نقطة` : 'نقاطك لا تكفي'}
                  </button>
                )}
              </div>
            )
          })}
          {shopSkinItems.length === 0 && (
            <Card className="col-span-full text-center text-sm text-slate-500">
              تصاميم النقاط تظهر هنا بعد تحديث بيانات المتجر
            </Card>
          )}
        </div>
      </div>
    </div>
  )
}
