import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Check, ChevronLeft, ChevronRight, Search } from 'lucide-react'
import { useState } from 'react'
import { Badge, Button, Card, Empty, ErrorNote, PageHeader, Spinner, Stat, inputCls, statusTone } from '../../components/ui'
import { api } from '../../lib/api'
import { addDays, fmtDay, fmtTime, iso, money, parseDate } from '../../lib/format'
import type { BookingRow, DayBoard, MealBoard } from '../../lib/types'

export default function MessBoard() {
  const [date, setDate] = useState(iso(new Date()))
  const [search, setSearch] = useState('')
  const q = useQuery({ queryKey: ['mess', 'board', date], queryFn: () => api<DayBoard>(`/api/mess/board?date=${date}`), refetchInterval: 15_000 })
  const shift = (n: number) => setDate(iso(addDays(parseDate(date), n)))
  const t = q.data?.totals
  return (
    <div className="space-y-6">
      <PageHeader title="Mess operations" subtitle="Confirm that residents ate. Confirmation is what bills them: booking alone never does."
        action={
          <div className="flex items-center gap-1.5">
            <Button variant="secondary" size="sm" aria-label="Previous day" onClick={() => shift(-1)}><ChevronLeft className="h-4 w-4" /></Button>
            <span className="min-w-[110px] text-center text-sm font-medium">{fmtDay(date)}</span>
            <Button variant="secondary" size="sm" aria-label="Next day" onClick={() => shift(1)}><ChevronRight className="h-4 w-4" /></Button>
          </div>} />
      {t && (
        <div className="grid grid-cols-2 gap-3 lg:grid-cols-5">
          <Stat label="Expected" value={t.expected} hint="Booked, not cancelled" />
          <Stat label="Confirmed eaten" value={t.consumed} tone="good" />
          <Stat label="Pending confirmation" value={t.pending} tone={t.pending > 0 ? 'warn' : undefined} />
          <Stat label="Cancelled" value={t.cancelled} />
          <Stat label="Meal revenue" value={money(t.revenue)} hint="From confirmed meals" />
        </div>
      )}
      <div className="relative max-w-sm">
        <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted" />
        <input className={`${inputCls} pl-9`} placeholder="Find a resident or enrolment no." aria-label="Search residents" value={search} onChange={(e) => setSearch(e.target.value)} />
      </div>
      {q.isLoading ? <Spinner /> : q.error ? <ErrorNote error={q.error} /> : q.data!.meals.length === 0
        ? <Card><Empty title="No meals scheduled for this day" /></Card>
        : q.data!.meals.map((m) => <MealSection key={m.meal.id} board={m} search={search} date={date} />)}
    </div>
  )
}

function MealSection({ board, search, date }: { board: MealBoard; search: string; date: string }) {
  const qc = useQueryClient()
  const [error, setError] = useState<Error | null>(null)
  const refresh = () => ['mess', 'dashboard', 'analytics'].forEach((k) => qc.invalidateQueries({ queryKey: [k] }))
  const confirm = useMutation({
    mutationFn: (id: number) => api<BookingRow>(`/api/mess/bookings/${id}/confirm`, { method: 'POST' }),
    onSuccess: () => { setError(null); refresh() }, onError: (e) => { setError(e as Error); refresh() },
  })
  const closeNoShows = useMutation({
    mutationFn: () => api(`/api/mess/meals/${board.meal.id}/close-no-shows`, { method: 'POST' }),
    onSuccess: () => { setError(null); refresh() }, onError: (e) => setError(e as Error),
  })
  const { meal, counts } = board
  const rows = board.bookings.filter((b) => !search || (b.residentName + b.enrollmentNo).toLowerCase().includes(search.toLowerCase()))
  const future = date > iso(new Date())
  return (
    <Card>
      <div className="flex flex-wrap items-start justify-between gap-3 border-b border-line px-5 py-4">
        <div>
          <h2 className="text-xl font-medium text-forest-dark">{meal.categoryLabel} <span className="text-sm font-normal text-muted">· {fmtTime(meal.startTime)}</span></h2>
          <p className="mt-0.5 text-[13px] text-muted">{meal.menuItems.join(', ')} · {money(meal.price)} per plate</p>
        </div>
        <div className="flex flex-wrap items-center gap-2 text-xs">
          <Badge tone="blue">{counts.expected} expected</Badge><Badge tone="green">{counts.consumed} eaten</Badge>
          {counts.pending > 0 && <Badge tone="amber">{counts.pending} pending</Badge>}
          <Badge>{money(counts.revenue)}</Badge>
          {counts.pending > 0 && !future && <Button size="sm" variant="secondary" loading={closeNoShows.isPending} onClick={() => closeNoShows.mutate()}>Close no-shows</Button>}
        </div>
      </div>
      {error && <div className="px-5 pt-3"><ErrorNote error={error} /></div>}
      {rows.length === 0 ? <Empty title="No bookings" hint={board.bookings.length ? 'No match for your search.' : undefined} /> : (
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="border-b border-line text-xs uppercase tracking-wide text-muted"><tr><th className="px-5 py-2.5">Resident</th><th className="px-3 py-2.5">Enrolment</th><th className="px-3 py-2.5">Status</th><th className="px-5 py-2.5 text-right">Action</th></tr></thead>
            <tbody className="divide-y divide-line">
              {rows.map((b) => (
                <tr key={b.bookingId}>
                  <td className="px-5 py-2.5 font-medium">{b.residentName}</td>
                  <td className="px-3 py-2.5 text-muted">{b.enrollmentNo}</td>
                  <td className="px-3 py-2.5"><Badge tone={statusTone(b.status)}>{b.status === 'CONSUMED' ? `Eaten · ${money(b.chargedAmount)}` : b.status.charAt(0) + b.status.slice(1).toLowerCase()}</Badge></td>
                  <td className="px-5 py-2.5 text-right">
                    {b.status === 'BOOKED' && <Button size="sm" disabled={future} loading={confirm.isPending && confirm.variables === b.bookingId} onClick={() => confirm.mutate(b.bookingId)}><Check className="h-3.5 w-3.5" /> Confirm eaten</Button>}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </Card>
  )
}
