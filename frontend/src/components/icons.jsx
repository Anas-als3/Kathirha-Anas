/**
 * Kathirha icon set — single stroke family (2px, round caps), currentColor.
 * One visual language everywhere; never emojis for structural icons.
 */
const base = { fill: 'none', stroke: 'currentColor', strokeWidth: 2, strokeLinecap: 'round', strokeLinejoin: 'round' }

function Icon({ size = 20, children, ...rest }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" aria-hidden="true" {...base} {...rest}>
      {children}
    </svg>
  )
}

export const IconHome = (p) => <Icon {...p}><path d="M3 11l9-8 9 8" /><path d="M5 10v10h14V10" /></Icon>
export const IconTarget = (p) => <Icon {...p}><circle cx="12" cy="12" r="8" /><circle cx="12" cy="12" r="4" /><circle cx="12" cy="12" r="0.5" /></Icon>
export const IconTrophy = (p) => <Icon {...p}><path d="M8 21h8" /><path d="M12 17v4" /><path d="M7 4h10v6a5 5 0 0 1-10 0z" /><path d="M7 6H4.5a2 2 0 0 0 .5 4h2" /><path d="M17 6h2.5a2 2 0 0 1-.5 4h-2" /></Icon>
export const IconPodium = (p) => <Icon {...p}><path d="M8 21V10" /><path d="M16 21V4" /><path d="M4 21h16" /></Icon>
export const IconGift = (p) => <Icon {...p}><rect x="3" y="8" width="18" height="4" rx="1" /><path d="M12 8v13" /><path d="M19 12v7a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2v-7" /><path d="M7.5 8a2.5 2.5 0 0 1 0-5C11 3 12 8 12 8s1-5 4.5-5a2.5 2.5 0 0 1 0 5" /></Icon>
export const IconBulb = (p) => <Icon {...p}><path d="M9 18h6" /><path d="M10 21h4" /><path d="M12 3a6 6 0 0 1 3.6 10.8c-.6.5-.6 1.2-.6 2.2h-6c0-1 0-1.7-.6-2.2A6 6 0 0 1 12 3z" /></Icon>
export const IconChat = (p) => <Icon {...p}><path d="M3 20l1.3-3.9A8 8 0 1 1 12 20a8 8 0 0 1-3.8-1z" /></Icon>
export const IconShield = (p) => <Icon {...p}><path d="M12 3l7 4v5c0 4.4-3 7.4-7 9-4-1.6-7-4.6-7-9V7z" /></Icon>
export const IconCoins = (p) => <Icon {...p}><ellipse cx="12" cy="6" rx="7" ry="3" /><path d="M5 6v6c0 1.7 3.1 3 7 3s7-1.3 7-3V6" /><path d="M5 12v6c0 1.7 3.1 3 7 3s7-1.3 7-3v-6" /></Icon>
export const IconFlame = (p) => <Icon {...p}><path d="M12 3s-6 5.2-6 10a6 6 0 0 0 12 0c0-4.8-6-10-6-10z" /><path d="M12 17a2.5 2.5 0 0 0 2.5-2.5c0-1.8-2.5-4-2.5-4s-2.5 2.2-2.5 4A2.5 2.5 0 0 0 12 17z" /></Icon>
export const IconSparkles = (p) => <Icon {...p}><path d="M12 4l1.7 4.3L18 10l-4.3 1.7L12 16l-1.7-4.3L6 10l4.3-1.7z" /><path d="M18.5 15.5l.7 1.8 1.8.7-1.8.7-.7 1.8-.7-1.8-1.8-.7 1.8-.7z" /></Icon>
export const IconCrown = (p) => <Icon {...p}><path d="M12 6l4 6 5-4-2 10H5L3 8l5 4z" /></Icon>
export const IconWallet = (p) => <Icon {...p}><rect x="3" y="6" width="18" height="13" rx="2" /><path d="M3 10h18" /><circle cx="16.5" cy="14.5" r="1" /></Icon>
export const IconTrendUp = (p) => <Icon {...p}><path d="M3 17l6-6 4 4 8-8" /><path d="M17 7h4v4" /></Icon>
export const IconLock = (p) => <Icon {...p}><rect x="5" y="11" width="14" height="9" rx="2" /><path d="M8 11V8a4 4 0 0 1 8 0v3" /></Icon>
export const IconScale = (p) => <Icon {...p}><path d="M12 3v18" /><path d="M8 21h8" /><path d="M4 7h16" /><path d="M6 7l-2.5 6a3.2 3.2 0 0 0 6.4 0L7.5 7" /><path d="M16.5 7L14 13a3.2 3.2 0 0 0 6.4 0L18 7" /></Icon>
export const IconMoon = (p) => <Icon {...p}><path d="M21 13A8 8 0 1 1 11 3a6.5 6.5 0 0 0 10 10z" /></Icon>
export const IconCheck = (p) => <Icon {...p}><path d="M5 12l5 5L20 7" /></Icon>
export const IconBank = (p) => <Icon {...p}><path d="M3 21h18" /><path d="M4 10h16" /><path d="M5 10l7-6 7 6" /><path d="M6 10v11" /><path d="M18 10v11" /><path d="M10 14v4" /><path d="M14 14v4" /></Icon>
export const IconBolt = (p) => <Icon {...p}><path d="M13 3L4 14h6l-1 7 9-11h-6z" /></Icon>
export const IconClock = (p) => <Icon {...p}><circle cx="12" cy="12" r="9" /><path d="M12 7v5l3 2" /></Icon>
export const IconStore = (p) => <Icon {...p}><path d="M4 9l1-4h14l1 4" /><path d="M4 9a2.6 2.6 0 0 0 5.3 0 2.6 2.6 0 0 0 5.4 0A2.6 2.6 0 0 0 20 9" /><path d="M5 11.5V20h14v-8.5" /><path d="M9 20v-5h6v5" /></Icon>
export const IconQuestion = (p) => <Icon {...p}><circle cx="12" cy="12" r="9" /><path d="M9.5 9a2.6 2.6 0 1 1 4.4 1.9c-.7.6-1.4 1-1.4 2.1v.3" /><path d="M12 17h.01" /></Icon>
export const IconMedal = (p) => <Icon {...p}><circle cx="12" cy="14" r="5" /><path d="M9 10L6.5 3h4L12 7l1.5-4h4L15 10" /><path d="M12 12.5l.9 1.8 2 .3-1.4 1.4.3 2-1.8-1-1.8 1 .3-2-1.4-1.4 2-.3z" /></Icon>
export const IconArrowBack = (p) => <Icon {...p}><path d="M5 12h14" /><path d="M15 6l6 6-6 6" /></Icon>
export const IconArrowGo = (p) => <Icon {...p}><path d="M19 12H5" /><path d="M9 6l-6 6 6 6" /></Icon>
export const IconPlus = (p) => <Icon {...p}><path d="M12 5v14" /><path d="M5 12h14" /></Icon>
export const IconLifebuoy = (p) => <Icon {...p}><circle cx="12" cy="12" r="9" /><circle cx="12" cy="12" r="4" /><path d="M5.6 5.6l3.5 3.5" /><path d="M14.9 14.9l3.5 3.5" /><path d="M14.9 9.1l3.5-3.5" /><path d="M5.6 18.4l3.5-3.5" /></Icon>
