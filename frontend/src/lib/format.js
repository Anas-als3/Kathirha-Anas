export const sar = (v, decimals = 0) => {
  const n = Number(v ?? 0)
  return new Intl.NumberFormat('en-US', { maximumFractionDigits: decimals }).format(n) + ' ريال'
}

export const pct = (v, d = 1) => `${Number(v ?? 0).toFixed(d)}%`

export const num = (v) => new Intl.NumberFormat('en-US').format(Number(v ?? 0))

export const dateShort = (s) => {
  if (!s) return ''
  try {
    // Arabic month names, Latin digits (matches the rest of the UI)
    return new Date(s).toLocaleDateString('ar-u-nu-latn', { day: 'numeric', month: 'short', year: 'numeric' })
  } catch {
    return s
  }
}
