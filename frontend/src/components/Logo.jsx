/**
 * The Kathirha mark — coin stack + sprout + growth arrow (same artwork as the deck).
 * variant="coral" for light backgrounds, variant="cream" for dark/brand backgrounds.
 */
export default function Logo({ size = 36, variant = 'coral' }) {
  const c = variant === 'cream'
    ? { b: '#EAD8C8', m: '#FCEFE6', t: '#F4E2D2', st: '#F4E2D2', l1: '#FCEFE6', l2: '#F4E2D2' }
    : { b: '#C66848', m: '#E89A7B', t: '#DD8463', st: '#DD8463', l1: '#E89A7B', l2: '#DD8463' }
  return (
    <svg width={size} height={size} viewBox="0 0 100 100" style={{ display: 'block' }} aria-label="كثّرها">
      <path d="M58 82 C82 76 88 48 79 27" stroke={c.t} strokeWidth="6.5" fill="none" strokeLinecap="round" />
      <path d="M79 27 L70.5 29 L80 35.5 Z" fill={c.t} />
      <ellipse cx="45" cy="78" rx="23" ry="6.4" fill={c.b} />
      <ellipse cx="45" cy="68.5" rx="23" ry="6.4" fill={c.m} />
      <ellipse cx="45" cy="59" rx="23" ry="6.4" fill={c.t} />
      <ellipse cx="45" cy="49.5" rx="23" ry="6.4" fill={c.m} />
      <path d="M45 44 V35" stroke={c.st} strokeWidth="6" strokeLinecap="round" />
      <path d="M45 40 C35 40 27 35 25 24.5 C38.5 23.5 45 30.5 45 40 Z" fill={c.l1} />
      <path d="M45 38 C55 38 63 33 65 23 C51.5 22 45 29.5 45 38 Z" fill={c.l2} />
    </svg>
  )
}
