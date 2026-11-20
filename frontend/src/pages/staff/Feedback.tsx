import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { Badge, Card, Empty, ErrorNote, PageHeader, Spinner, Stars } from '../../components/ui'
import { api } from '../../lib/api'
import { fmtDateTime, fmtShortDate } from '../../lib/format'
import type { FeedbackRow } from '../../lib/types'

export default function Feedback() {
  const q = useQuery({ queryKey: ['mess', 'feedback'], queryFn: () => api<FeedbackRow[]>('/api/mess/feedback?limit=100') })
  const [lowOnly, setLowOnly] = useState(false)
  const rows = (q.data ?? []).filter((r) => !lowOnly || r.tasteRating <= 2 || r.hygieneRating <= 2)
  return (
    <div>
      <PageHeader title="Food feedback" subtitle="Reviews come only from residents who ate the meal."
        action={<label className="flex items-center gap-2 text-sm"><input type="checkbox" checked={lowOnly} onChange={(e) => setLowOnly(e.target.checked)} /> Show low ratings only (≤ 2)</label>} />
      {q.isLoading ? <Spinner /> : q.error ? <ErrorNote error={q.error} /> : rows.length === 0 ? <Card><Empty title="No feedback to show" /></Card> : (
        <Card>
          <ul className="divide-y divide-line">
            {rows.map((r) => (
              <li key={r.id} className="px-5 py-4">
                <div className="flex flex-wrap items-center gap-x-4 gap-y-1">
                  <span className="text-sm font-semibold">{r.categoryLabel} · {fmtShortDate(r.mealDate)}</span>
                  <span className="inline-flex items-center gap-1.5 text-xs text-muted">Taste <Stars value={r.tasteRating} /></span>
                  <span className="inline-flex items-center gap-1.5 text-xs text-muted">Hygiene <Stars value={r.hygieneRating} /></span>
                  {(r.tasteRating <= 2 || r.hygieneRating <= 2) && <Badge tone="red">Needs attention</Badge>}
                </div>
                <p className="mt-0.5 text-[13px] text-muted">{r.menu}</p>
                {r.comment && <p className="mt-1.5 text-sm">“{r.comment}”</p>}
                <p className="mt-1 text-xs text-muted">{r.residentName} · {fmtDateTime(r.createdAt)}</p>
              </li>
            ))}
          </ul>
        </Card>
      )}
    </div>
  )
}
