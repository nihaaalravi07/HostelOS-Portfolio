import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { CategoryRatingChart, ParticipationChart, RatingDistributionChart, RevenueChart } from '../../components/charts'
import { Card, CardHeader, Empty, ErrorNote, PageHeader, Spinner, Stars, Stat, Tabs } from '../../components/ui'
import { api } from '../../lib/api'
import { fmtShortDate, money } from '../../lib/format'
import type { Analytics, MealRank } from '../../lib/types'

export default function MessAnalytics() {
  const [days, setDays] = useState<'7' | '14' | '30'>('14')
  const q = useQuery({ queryKey: ['analytics', days], queryFn: () => api<Analytics>(`/api/mess/analytics?days=${days}`) })
  const a = q.data
  return (
    <div className="space-y-6">
      <PageHeader title="Mess analytics" subtitle="Demand, revenue and food accountability."
        action={<Tabs<'7' | '14' | '30'> tabs={[{ id: '7', label: '7 days' }, { id: '14', label: '14 days' }, { id: '30', label: '30 days' }]} value={days} onChange={setDays} />} />
      {q.isLoading ? <Spinner /> : q.error || !a ? <ErrorNote error={q.error} /> : (
        <>
          <div className="grid grid-cols-2 gap-3 lg:grid-cols-5">
            <Stat label="Expected" value={a.totals.expected} hint="Booked, not cancelled" />
            <Stat label="Eaten" value={a.totals.consumed} tone="good" />
            <Stat label="Missed" value={a.totals.missed} tone={a.totals.missed ? 'warn' : undefined} hint="Booked, never ate" />
            <Stat label="Revenue" value={money(a.totals.revenue)} />
            <Stat label="Reviews" value={a.totals.reviews} hint={`Taste ${a.totals.avgTaste ?? '–'} · Hygiene ${a.totals.avgHygiene ?? '–'}`} />
          </div>
          <div className="grid gap-5 lg:grid-cols-2">
            <Card><CardHeader title="Expected versus eaten" /><div className="p-4"><ParticipationChart trend={a.trend} /></div></Card>
            <Card><CardHeader title="Revenue per day" /><div className="p-4"><RevenueChart trend={a.trend} /></div></Card>
            <Card><CardHeader title="Rating distribution" subtitle="How residents scored taste and hygiene" /><div className="p-4"><RatingDistributionChart dist={a.ratingDistribution} /></div></Card>
            <Card><CardHeader title="Average rating by meal" /><div className="p-4"><CategoryRatingChart categories={a.categories} /></div></Card>
          </div>
          <Card>
            <CardHeader title="Performance by meal" />
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead className="border-b border-line text-xs uppercase tracking-wide text-muted"><tr><th className="px-5 py-3">Meal</th><th className="px-3 py-3 text-right">Expected</th><th className="px-3 py-3 text-right">Eaten</th><th className="px-3 py-3 text-right">Revenue</th><th className="px-3 py-3">Taste</th><th className="px-3 py-3">Hygiene</th><th className="px-5 py-3 text-right">Reviews</th></tr></thead>
                <tbody className="divide-y divide-line">
                  {a.categories.map((c) => (
                    <tr key={c.category}><td className="px-5 py-3 font-medium">{c.label}</td><td className="tabular px-3 py-3 text-right">{c.expected}</td><td className="tabular px-3 py-3 text-right">{c.consumed}</td><td className="tabular px-3 py-3 text-right">{money(c.revenue)}</td><td className="px-3 py-3"><Stars value={c.avgTaste} /></td><td className="px-3 py-3"><Stars value={c.avgHygiene} /></td><td className="tabular px-5 py-3 text-right">{c.reviews}</td></tr>
                  ))}
                </tbody>
              </table>
            </div>
          </Card>
          <div className="grid gap-5 lg:grid-cols-2">
            <RankCard title="Most popular meals" subtitle="By plates eaten" rows={a.popularMeals} />
            <RankCard title="Lowest rated meals" subtitle="At least 2 reviews, by taste" rows={a.lowestRatedMeals} low />
          </div>
        </>
      )}
    </div>
  )
}

function RankCard({ title, subtitle, rows, low }: { title: string; subtitle: string; rows: MealRank[]; low?: boolean }) {
  return (
    <Card>
      <CardHeader title={title} subtitle={subtitle} />
      {rows.length === 0 ? <Empty title="Not enough data yet" /> : (
        <ul className="divide-y divide-line">
          {rows.map((r) => (
            <li key={r.mealId} className="flex items-center justify-between gap-3 px-5 py-3">
              <div className="min-w-0"><p className="truncate text-sm font-medium">{r.categoryLabel} · {fmtShortDate(r.date)}</p><p className="truncate text-xs text-muted">{r.menu}</p></div>
              <div className="shrink-0 text-right text-sm">{low ? <Stars value={r.avgTaste} /> : <span className="tabular font-semibold">{r.consumed} plates</span>}<p className="text-xs text-muted">{r.reviews} reviews</p></div>
            </li>
          ))}
        </ul>
      )}
    </Card>
  )
}
