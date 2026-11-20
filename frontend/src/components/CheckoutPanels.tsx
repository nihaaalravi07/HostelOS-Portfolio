import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { api } from '../lib/api'
import { addDays, fmtShortDate, iso, money, titleCase } from '../lib/format'
import type { Checkout } from '../lib/types'
import { Badge, Button, Card, CardHeader, Empty, ErrorNote, Field, Spinner, inputCls } from './ui'

const tone = (s: Checkout['status']) => (s === 'CLEARED' ? 'green' : s === 'REJECTED' ? 'red' : 'amber')

/** Student side: request checkout and follow its status. */
export function StudentCheckout() {
  const qc = useQueryClient()
  const q = useQuery({ queryKey: ['checkout', 'mine'], queryFn: () => api<Checkout[]>('/api/checkout/mine') })
  const [date, setDate] = useState(iso(addDays(new Date(), 7)))
  const submit = useMutation({
    mutationFn: () => api('/api/checkout', { body: { plannedDate: date } }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['checkout'] }),
  })
  return (
    <div className="grid gap-5 lg:grid-cols-2">
      <Card className="h-fit">
        <CardHeader title="Request checkout" subtitle="Before the warden can clear you: no dues, no open complaints, no pending cleaning." />
        <form className="space-y-4 p-5" onSubmit={(e) => { e.preventDefault(); submit.mutate() }}>
          <Field label="Planned checkout date"><input className={inputCls} type="date" min={iso(new Date())} value={date} onChange={(e) => setDate(e.target.value)} required /></Field>
          <ErrorNote error={submit.error} />
          <Button type="submit" loading={submit.isPending}>Submit request</Button>
        </form>
      </Card>
      <Card className="h-fit">
        <CardHeader title="Your requests" />
        {q.isLoading ? <Spinner /> : (q.data ?? []).length === 0 ? <Empty title="No checkout requests" /> : (
          <ul className="divide-y divide-line">
            {q.data!.map((c) => (
              <li key={c.id} className="px-5 py-3">
                <div className="flex items-center justify-between"><span className="text-sm font-medium">Leaving {fmtShortDate(c.plannedDate)}</span><Badge tone={tone(c.status)}>{titleCase(c.status)}</Badge></div>
                {c.clearance && !c.clearance.canClear && <p className="mt-1 text-[13px] text-clay">Still to resolve: {c.clearance.blockers.join('; ')}</p>}
                {c.clearance?.canClear && <p className="mt-1 text-[13px] text-forest">All dues and requests are settled. Waiting for the warden.</p>}
                {c.reviewNote && <p className="mt-1 text-[13px] text-muted">Note: {c.reviewNote}</p>}
              </li>
            ))}
          </ul>
        )}
      </Card>
    </div>
  )
}

/** Warden/admin side: review checkout requests with live clearance checks. */
export function CheckoutQueue() {
  const qc = useQueryClient()
  const q = useQuery({ queryKey: ['checkout', 'all'], queryFn: () => api<Checkout[]>('/api/checkout') })
  const refresh = () => ['checkout', 'accommodation', 'dashboard', 'finance'].forEach((k) => qc.invalidateQueries({ queryKey: [k] }))
  const clear = useMutation({ mutationFn: (id: number) => api(`/api/checkout/${id}/clear`, { body: {} }), onSuccess: refresh })
  const reject = useMutation({ mutationFn: (id: number) => api(`/api/checkout/${id}/reject`, { body: { note: 'Rejected by warden' } }), onSuccess: refresh })
  if (q.isLoading) return <Spinner />
  return (
    <div className="space-y-3">
      <ErrorNote error={clear.error ?? reject.error ?? q.error} />
      <Card>
        {(q.data ?? []).length === 0 ? <Empty title="No checkout requests" /> : (
          <ul className="divide-y divide-line">
            {q.data!.map((c) => (
              <li key={c.id} className="flex flex-wrap items-start justify-between gap-3 px-5 py-4">
                <div className="min-w-0">
                  <div className="flex flex-wrap items-center gap-2"><span className="text-sm font-semibold">{c.residentName}</span><span className="text-xs text-muted">{c.enrollmentNo} · {c.room}</span><Badge tone={tone(c.status)}>{titleCase(c.status)}</Badge></div>
                  <p className="mt-1 text-[13px] text-muted">Leaving {fmtShortDate(c.plannedDate)}, requested {fmtShortDate(c.requestedOn)}</p>
                  {c.clearance && (
                    <p className={`mt-1 text-[13px] ${c.clearance.canClear ? 'text-forest' : 'text-clay'}`}>
                      {c.clearance.canClear ? 'Ready to clear: no dues, no open requests.' : `Blocked: ${c.clearance.blockers.join('; ')}`}
                    </p>
                  )}
                  {c.status !== 'PENDING' && c.duesAtReview != null && <p className="mt-1 text-[13px] text-muted">Dues at review: {money(c.duesAtReview)}{c.reviewNote ? ` · ${c.reviewNote}` : ''}</p>}
                </div>
                {c.status === 'PENDING' && (
                  <div className="flex gap-2">
                    <Button size="sm" variant="secondary" onClick={() => reject.mutate(c.id)}>Reject</Button>
                    <Button size="sm" disabled={!c.clearance?.canClear} loading={clear.isPending && clear.variables === c.id} onClick={() => clear.mutate(c.id)}>Clear and release bed</Button>
                  </div>
                )}
              </li>
            ))}
          </ul>
        )}
      </Card>
    </div>
  )
}
