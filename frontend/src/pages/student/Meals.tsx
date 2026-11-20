import { useQuery } from '@tanstack/react-query'
import { useMemo, useState } from 'react'
import { MealCard } from '../../components/MealCard'
import { Badge, Card, Empty, ErrorNote, PageHeader, Spinner, Tabs, statusTone } from '../../components/ui'
import { api } from '../../lib/api'
import { addDays, fmtDateTime, fmtDay, iso, money, moneyExact } from '../../lib/format'
import type { BookingHistoryItem, Meal } from '../../lib/types'
import { ReceiptIndianRupee } from 'lucide-react'

export default function Meals() {
  const [tab, setTab] = useState<'menu' | 'history'>('menu')
  return (
    <div>
      <PageHeader title="Meals" subtitle="Book only what you will eat. You are billed after the mess staff confirm you ate it." />
      <Tabs tabs={[{ id: 'menu', label: 'Menu & booking' }, { id: 'history', label: 'My meals' }]} value={tab} onChange={setTab} />
      {tab === 'menu' ? <Menu /> : <History />}
    </div>
  )
}

function Menu() {
  const today = new Date()
  const [offset, setOffset] = useState(0)
  const from = iso(today), to = iso(addDays(today, 6))
  const q = useQuery({ queryKey: ['meals', from, to], queryFn: () => api<Meal[]>(`/api/meals?from=${from}&to=${to}`) })
  const days = useMemo(() => Array.from({ length: 7 }, (_, i) => iso(addDays(today, i))), [from]) // eslint-disable-line react-hooks/exhaustive-deps
  const selected = days[offset]
  const meals = (q.data ?? []).filter((m) => m.date === selected)
  return (
    <div>
      <div className="-mx-1 mb-5 flex gap-2 overflow-x-auto px-1 pb-1" role="tablist" aria-label="Day">
        {days.map((d, i) => {
          const count = (q.data ?? []).filter((m) => m.date === d && m.myStatus === 'BOOKED').length
          return (
            <button key={d} role="tab" aria-selected={offset === i} onClick={() => setOffset(i)}
              className={`min-w-[84px] rounded-lg border px-3 py-2 text-left transition-colors ${offset === i ? 'border-forest bg-forest text-ivory' : 'border-line bg-paper hover:bg-forest-soft'}`}>
              <span className="block text-xs opacity-80">{i === 0 ? 'Today' : i === 1 ? 'Tomorrow' : fmtDay(d).split(',')[0]}</span>
              <span className="block text-sm font-semibold">{fmtDay(d).replace(/^\w+,? /, '')}</span>
              <span className="block text-[11px] opacity-80">{count ? `${count} booked` : ' '}</span>
            </button>
          )
        })}
      </div>
      {q.isLoading ? <Spinner /> : q.error ? <ErrorNote error={q.error} /> : meals.length === 0
        ? <Card><Empty title="No meals published for this day" /></Card>
        : <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">{meals.map((m) => <MealCard key={m.id} meal={m} />)}</div>}
    </div>
  )
}

function History() {
  const q = useQuery({ queryKey: ['history'], queryFn: () => api<BookingHistoryItem[]>('/api/meals/history?limit=60') })
  if (q.isLoading) return <Spinner />
  if (q.error) return <ErrorNote error={q.error} />
  const rows = q.data ?? []
  const eaten = rows.filter((r) => r.status === 'CONSUMED')
  const total = eaten.reduce((s, r) => s + (r.chargedAmount ?? 0), 0)
  return (
    <div className="space-y-4">
      <p className="text-sm text-muted">
        <ReceiptIndianRupee className="mr-1 inline h-4 w-4" /> Last {rows.length} bookings · {eaten.length} eaten and billed {moneyExact(total)}.
        Cancelled and missed meals cost nothing.
      </p>
      <Card>
        {rows.length === 0 ? <Empty title="No bookings yet" hint="Book your first meal from the menu." /> : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="border-b border-line text-xs uppercase tracking-wide text-muted">
                <tr><th className="px-5 py-3">Meal</th><th className="px-3 py-3">Items</th><th className="px-3 py-3">Status</th><th className="px-3 py-3 text-right">Charged</th><th className="px-5 py-3">Your rating</th></tr>
              </thead>
              <tbody className="divide-y divide-line">
                {rows.map((r) => (
                  <tr key={r.bookingId}>
                    <td className="whitespace-nowrap px-5 py-3"><span className="font-medium">{r.meal.categoryLabel}</span><br /><span className="text-xs text-muted">{fmtDay(r.meal.date)}</span></td>
                    <td className="max-w-[260px] truncate px-3 py-3 text-muted">{r.meal.menuItems.join(', ')}</td>
                    <td className="px-3 py-3"><Badge tone={statusTone(r.status)}>{r.status === 'CONSUMED' ? 'Eaten' : r.status.charAt(0) + r.status.slice(1).toLowerCase()}</Badge>
                      {r.consumedAt && <span className="ml-2 text-xs text-muted">{fmtDateTime(r.consumedAt)}</span>}</td>
                    <td className="tabular px-3 py-3 text-right">{r.chargedAmount != null ? money(r.chargedAmount) : '–'}</td>
                    <td className="px-5 py-3 text-muted">{r.meal.myFeedback ? `Taste ${r.meal.myFeedback.tasteRating}/5 · Hygiene ${r.meal.myFeedback.hygieneRating}/5` : r.status === 'CONSUMED' ? 'Not rated' : '–'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>
    </div>
  )
}
