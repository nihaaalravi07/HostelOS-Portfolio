import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { CategoryRatingChart, ParticipationChart, RevenueChart } from '../../components/charts'
import { Bar, Card, CardHeader, ErrorNote, PageHeader, Spinner, Stat } from '../../components/ui'
import { api } from '../../lib/api'
import { money } from '../../lib/format'
import type { AdminDashboard as Data } from '../../lib/types'

export default function AdminDashboard() {
  const q = useQuery({ queryKey: ['dashboard', 'admin'], queryFn: () => api<Data>('/api/dashboard/admin'), refetchInterval: 20_000 })
  if (q.isLoading) return <Spinner />
  if (q.error || !q.data) return <ErrorNote error={q.error} />
  const d = q.data, m = d.mess
  const accuracy = m.totals.expected ? Math.round((m.totals.consumed / m.totals.expected) * 100) : 0
  return (
    <div className="space-y-6">
      <PageHeader title="Hostel overview" subtitle="Live figures from bookings, confirmed meals, the ledger and service requests." />

      <section aria-label="Today in the mess">
        <h2 className="mb-2 text-lg font-medium text-forest-dark">Today in the mess</h2>
        <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
          <Stat label="Expected plates" value={d.todayExpected} hint="Booked and not cancelled" />
          <Stat label="Confirmed eaten" value={d.todayConsumed} tone="good" />
          <Stat label="Pending confirmation" value={d.todayPending} tone={d.todayPending > 0 ? 'warn' : undefined} hint={<Link className="underline" to="/mess">Open mess board</Link>} />
          <Stat label="Revenue today" value={money(d.todayRevenue)} hint={`${money(d.monthRevenue)} this month`} />
        </div>
      </section>

      <section aria-label="Hostel">
        <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
          <Stat label="Residents" value={d.totalResidents} />
          <Stat label="Occupied beds" value={`${d.occupancy.occupiedBeds} / ${d.occupancy.totalBeds}`} hint={`${d.occupancy.availableBeds} available`} />
          <Stat label="Outstanding balances" value={money(d.outstandingBalances)} tone="warn" hint={<Link className="underline" to="/admin/finance">Financial report</Link>} />
          <Stat label="Avg food quality / hygiene" value={`${d.avgTaste?.toFixed(1) ?? '–'} / ${d.avgHygiene?.toFixed(1) ?? '–'}`} hint="Last 14 days, out of 5" />
          <Stat label="Pending cleaning" value={d.pendingCleaning} hint={<Link className="underline" to="/admin/requests">Manage requests</Link>} />
          <Stat label="Open complaints" value={d.openComplaints} tone={d.openComplaints > 0 ? 'warn' : 'good'} />
          <Stat label="Demand accuracy" value={`${accuracy}%`} hint="Booked meals actually eaten (14 days)" />
          <Stat label="Cancelled in advance" value={m.totals.cancelled} hint="Waste avoided" />
        </div>
      </section>

      <div className="grid gap-5 lg:grid-cols-2">
        <Card><CardHeader title="Expected versus eaten" subtitle="Last 14 days: what residents booked against what they ate" /><div className="p-4"><ParticipationChart trend={m.trend} /></div></Card>
        <Card><CardHeader title="Meal revenue" subtitle="Billed per day, from confirmed consumption only" /><div className="p-4"><RevenueChart trend={m.trend} /></div></Card>
        <Card><CardHeader title="Ratings by meal" subtitle="Average taste and hygiene, last 14 days" /><div className="p-4"><CategoryRatingChart categories={m.categories} /></div></Card>
        <Card>
          <CardHeader title="Occupancy" subtitle="Beds in use" />
          <div className="space-y-4 p-5">
            <div className="flex items-baseline justify-between"><span className="tabular text-4xl font-semibold text-forest">{Math.round((d.occupancy.occupiedBeds / Math.max(1, d.occupancy.totalBeds)) * 100)}%</span><span className="text-sm text-muted">{d.occupancy.occupiedBeds} of {d.occupancy.totalBeds} beds</span></div>
            <Bar value={d.occupancy.occupiedBeds} max={d.occupancy.totalBeds} />
            <Link to="/admin/residents" className="inline-block text-sm font-medium text-forest underline">Manage rooms and residents</Link>
          </div>
        </Card>
      </div>
    </div>
  )
}
