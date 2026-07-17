// Card-skin catalog. Winner skins unlock by real league/rank; shop skins are
// ShopItems (category SKIN) bought with wallet points — matched here by name.
// Card faces are the official Kathirha card art (served from /card-art).

// League order used for gating: bronze < silver < gold < diamond.
export function leagueTier(leagueLabel = '') {
  if (leagueLabel.includes('ماس')) return 4
  if (leagueLabel.includes('ذهب')) return 3
  if (leagueLabel.includes('فض')) return 2
  if (leagueLabel.includes('برونز')) return 1
  return 0
}

export const WINNER_SKINS = [
  {
    id: 'champion',
    name: 'تاج السيادة',
    desc: 'أعظم بطاقة في كثّرها — عامٌ كامل في القمة، وتاجٌ يُصاغ مرةً واحدة باسمك.',
    emoji: '👑',
    image: '/card-art/card-champion.png',
    unlocked: ({ rank }) => rank === 1,
    requirement: 'تصدّر دوري السنة',
  },
  {
    id: 'monthly',
    name: 'بدر التمام',
    desc: 'شهرٌ كامل في الصدارة — اكتمل البدر وسطع ضوؤه على الجميع.',
    emoji: '🌕',
    image: '/card-art/card-monthly.png',
    unlocked: ({ rank }) => rank === 1,
    requirement: 'تصدّر دوري الشهر',
  },
  {
    id: 'weekly',
    name: 'هلال السبق',
    desc: 'لمتصدّر الأسبوع وحده — هلالٌ رقيق يشهد أن السبق يبدأ صغيرًا.',
    emoji: '🌙',
    image: '/card-art/card-weekly.png',
    unlocked: ({ rank }) => rank === 1,
    requirement: 'تصدّر دوري الأسبوع',
  },
  {
    id: 'diamond',
    name: 'ماسة الخلود',
    desc: 'أندر الدرجات — صفاءٌ مقطوع بإتقان لا يخدشه إنفاق.',
    emoji: '💎',
    image: '/card-art/card-diamond.png',
    unlocked: ({ tier }) => tier >= 4,
    requirement: 'اوصل إلى الدوري الماسي',
  },
  {
    id: 'gold-league',
    name: 'شمس الصفوة',
    desc: 'لمن جعل القمّة عادته — ذهبٌ يشرق من الالتزام لا من الحظ.',
    emoji: '🏆',
    image: '/card-art/card-gold.png',
    unlocked: ({ tier }) => tier >= 3,
    requirement: 'اوصل إلى الدوري الذهبي',
  },
  {
    id: 'silver-league',
    name: 'مدار الفضة',
    desc: 'ثباتٌ في المدار — ادّخارك صار عادة تدور معك حيث كنت.',
    emoji: '🥈',
    image: '/card-art/card-silver.png',
    unlocked: ({ tier }) => tier >= 2,
    requirement: 'اوصل إلى الدوري الفضي',
  },
  {
    id: 'bronze-league',
    name: 'وسام العزم',
    desc: 'أول وسام في الرحلة — نحاسٌ مصقول يشهد أن كل بداية عزيمة.',
    emoji: '🥉',
    image: '/card-art/card-bronze.png',
    unlocked: ({ tier }) => tier >= 1,
    requirement: 'ادخل المنافسة واكسب أول نقاطك',
  },
]

// keyed by ShopItem name (shown in the shop) — keep in sync with DataSeeder
export const SHOP_SKINS = {
  'نبض البوليفارد': {
    id: 'neon',
    emoji: '🌃',
    cost: 600,
    image: '/card-art/card-neon.png',
    tagline: 'سهرة نيون من قلب الرياض — لمن يدّخر بإيقاع المدينة.',
  },
  'قوس العلا': {
    id: 'alula',
    emoji: '🏜️',
    cost: 800,
    image: '/card-art/card-alula.png',
    tagline: 'صخرُ العلا وقمرُها — تاريخٌ يطلّ من نافذة القوس.',
  },
  'لؤلؤ الخليج': {
    id: 'gulf',
    emoji: '🌊',
    cost: 800,
    image: '/card-art/card-gulf.png',
    tagline: 'من أعماق الخليج — صبرُ المحّار هو الذي يصنع اللؤلؤ.',
  },
  'أفق الرياض': {
    id: 'riyadh',
    emoji: '🌆',
    cost: 1000,
    image: '/card-art/card-riyadh.png',
    tagline: 'أفق العاصمة عند الغروب — طموحٌ بارتفاع الأبراج.',
  },
  'سعفة المجد': {
    id: 'palm',
    emoji: '🌴',
    cost: 1200,
    image: '/card-art/card-palm.png',
    tagline: 'خضرةُ النخل وكرمُه — عطاءٌ يثمر مع كل وديعة.',
  },
  'دار الذهب': {
    id: 'luxe',
    emoji: '✨',
    cost: 1500,
    image: '/card-art/card-luxe.png',
    tagline: 'زخرفةٌ ونقشٌ وذهبٌ خالص — فخامة الدور العريقة.',
  },
}

// season-exclusive card — earned at battle-pass level 7, gone when the season ends
export const SEASON_SKIN = {
  id: 'season-summer',
  name: 'بطاقة موسم الصيف',
  desc: 'حصرية صيف الادّخار — تُفتح بالمستوى 7، وتغيب بنهاية الموسم.',
  emoji: '🏖️',
  shine: true,
  requirement: 'اوصل إلى المستوى 7 في الباتل باس',
  style: {
    background: 'radial-gradient(circle at 100% 100%, #072530 0 30%, rgba(7,37,48,.98) 45%, rgba(7,37,48,0) 68%), radial-gradient(circle at 22% 28%, #FFD98A 0 5%, rgba(255,183,77,.45) 6% 15%, rgba(255,183,77,0) 36%), repeating-linear-gradient(-8deg, rgba(255,255,255,.06) 0 2px, transparent 2px 26px), linear-gradient(160deg, #0B4F5C 0%, #16808A 48%, #E8A54B 100%)',
    color: '#FFF7E6',
    border: '1px solid rgba(255,217,138,.6)',
  },
}

export const DEFAULT_SKIN = {
  id: 'default',
  name: 'كثّرها الكلاسيكية',
  emoji: '🪙',
  shine: false,
  style: {
    background: 'radial-gradient(circle at 100% 100%, #0C1A30 0 34%, rgba(12,26,48,.98) 54%, rgba(12,26,48,0) 75%), repeating-linear-gradient(135deg, rgba(221,132,99,.08) 0 1px, transparent 1px 16px), linear-gradient(150deg, #15243F 0%, #233C62 56%, #7B3E2A 100%)',
    color: '#FFFFFF',
  },
}
