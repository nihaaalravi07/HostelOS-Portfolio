const inr = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2, minimumFractionDigits: 0 })
const inrExact = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', minimumFractionDigits: 2, maximumFractionDigits: 2 })

export const money = (n: number | undefined | null) => inr.format(n ?? 0)
export const moneyExact = (n: number | undefined | null) => inrExact.format(n ?? 0)

/** Local date at midnight from a "YYYY-MM-DD" string (avoids the UTC shift of new Date("YYYY-MM-DD")). */
export const parseDate = (s: string) => (s.length === 10 ? new Date(s + 'T00:00:00') : new Date(s))
export const iso = (d: Date) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
export const addDays = (d: Date, n: number) => { const c = new Date(d); c.setDate(c.getDate() + n); return c }

export const fmtDay = (s: string) => parseDate(s).toLocaleDateString('en-IN', { weekday: 'short', day: 'numeric', month: 'short' })
export const fmtShortDate = (s: string) => parseDate(s).toLocaleDateString('en-IN', { day: 'numeric', month: 'short' })
export const fmtDateTime = (s: string) => parseDate(s).toLocaleString('en-IN', { day: 'numeric', month: 'short', hour: 'numeric', minute: '2-digit' })
export const fmtTime = (t: string) => {
  const [h, m] = t.split(':').map(Number)
  return new Date(2000, 0, 1, h, m).toLocaleTimeString('en-IN', { hour: 'numeric', minute: '2-digit' })
}
export const titleCase = (s: string) => s.toLowerCase().replace(/_/g, ' ').replace(/^\w/, (c) => c.toUpperCase())
export const roleLabel = (r: string) => ({
  STUDENT: 'Student', WARDEN: 'Warden', ADMIN: 'Administrator', MESS_STAFF: 'Mess staff',
  HOUSEKEEPING_STAFF: 'Housekeeping', MAINTENANCE_STAFF: 'Maintenance',
} as Record<string, string>)[r] ?? r
