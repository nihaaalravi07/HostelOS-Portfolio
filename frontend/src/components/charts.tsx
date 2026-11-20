import { Bar, BarChart, CartesianGrid, Legend, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { fmtShortDate } from '../lib/format'
import type { Analytics } from '../lib/types'

export const C = { forest: '#2E4A3A', gold: '#C9A24B', clay: '#B4532A', slate: '#3C5A7A', grid: '#E4DECF', muted: '#6C6F66' }
const axis = { fontSize: 12, fill: C.muted }
const tip = { contentStyle: { borderRadius: 8, border: `1px solid ${C.grid}`, fontSize: 12, background: '#FFFEFA' } }

/** Expected (booked) versus actually eaten, per day. */
export function ParticipationChart({ trend }: { trend: Analytics['trend'] }) {
  const data = trend.map((t) => ({ ...t, label: fmtShortDate(t.date) }))
  return (
    <div className="h-64 w-full" role="img" aria-label="Expected versus confirmed meals per day">
      <ResponsiveContainer>
        <LineChart data={data} margin={{ top: 8, right: 8, bottom: 0, left: -16 }}>
          <CartesianGrid stroke={C.grid} vertical={false} />
          <XAxis dataKey="label" tick={axis} tickLine={false} axisLine={false} interval="preserveStartEnd" />
          <YAxis tick={axis} tickLine={false} axisLine={false} allowDecimals={false} />
          <Tooltip {...tip} />
          <Legend iconType="plainline" wrapperStyle={{ fontSize: 12 }} />
          <Line type="monotone" dataKey="expected" name="Expected (booked)" stroke={C.gold} strokeWidth={2} strokeDasharray="5 4" dot={false} />
          <Line type="monotone" dataKey="consumed" name="Confirmed eaten" stroke={C.forest} strokeWidth={2.5} dot={{ r: 2.5 }} />
        </LineChart>
      </ResponsiveContainer>
    </div>
  )
}

export function RevenueChart({ trend }: { trend: Analytics['trend'] }) {
  const data = trend.map((t) => ({ ...t, label: fmtShortDate(t.date) }))
  return (
    <div className="h-64 w-full" role="img" aria-label="Meal revenue per day">
      <ResponsiveContainer>
        <BarChart data={data} margin={{ top: 8, right: 8, bottom: 0, left: -8 }}>
          <CartesianGrid stroke={C.grid} vertical={false} />
          <XAxis dataKey="label" tick={axis} tickLine={false} axisLine={false} interval="preserveStartEnd" />
          <YAxis tick={axis} tickLine={false} axisLine={false} tickFormatter={(v) => `₹${v / 1000}k`} />
          <Tooltip {...tip} formatter={(v) => [`₹${Number(v).toLocaleString('en-IN')}`, 'Revenue']} />
          <Bar dataKey="revenue" fill={C.forest} radius={[3, 3, 0, 0]} />
        </BarChart>
      </ResponsiveContainer>
    </div>
  )
}

export function CategoryRatingChart({ categories }: { categories: Analytics['categories'] }) {
  const data = categories.map((c) => ({ name: c.label, Taste: c.avgTaste ?? 0, Hygiene: c.avgHygiene ?? 0 }))
  return (
    <div className="h-64 w-full" role="img" aria-label="Average taste and hygiene rating by meal">
      <ResponsiveContainer>
        <BarChart data={data} margin={{ top: 8, right: 8, bottom: 0, left: -24 }}>
          <CartesianGrid stroke={C.grid} vertical={false} />
          <XAxis dataKey="name" tick={axis} tickLine={false} axisLine={false} />
          <YAxis domain={[0, 5]} tick={axis} tickLine={false} axisLine={false} />
          <Tooltip {...tip} />
          <Legend iconType="square" wrapperStyle={{ fontSize: 12 }} />
          <Bar dataKey="Taste" fill={C.forest} radius={[3, 3, 0, 0]} />
          <Bar dataKey="Hygiene" fill={C.gold} radius={[3, 3, 0, 0]} />
        </BarChart>
      </ResponsiveContainer>
    </div>
  )
}

export function RatingDistributionChart({ dist }: { dist: Analytics['ratingDistribution'] }) {
  const data = dist.map((d) => ({ name: `${d.rating}★`, Taste: d.taste, Hygiene: d.hygiene }))
  return (
    <div className="h-64 w-full" role="img" aria-label="Distribution of ratings">
      <ResponsiveContainer>
        <BarChart data={data} margin={{ top: 8, right: 8, bottom: 0, left: -24 }}>
          <CartesianGrid stroke={C.grid} vertical={false} />
          <XAxis dataKey="name" tick={axis} tickLine={false} axisLine={false} />
          <YAxis tick={axis} tickLine={false} axisLine={false} allowDecimals={false} />
          <Tooltip {...tip} />
          <Legend iconType="square" wrapperStyle={{ fontSize: 12 }} />
          <Bar dataKey="Taste" fill={C.forest} radius={[3, 3, 0, 0]} />
          <Bar dataKey="Hygiene" fill={C.gold} radius={[3, 3, 0, 0]} />
        </BarChart>
      </ResponsiveContainer>
    </div>
  )
}
