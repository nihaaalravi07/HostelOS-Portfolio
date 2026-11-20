import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Badge, Button, Card, CardHeader, ErrorNote, Field, Modal, PageHeader, Spinner, Stat, inputCls } from '../../components/ui'
import { api } from '../../lib/api'
import { fmtDateTime, money, moneyExact } from '../../lib/format'
import type { FinanceReport as Report, FinanceSummary, ResidentSummary, StatementLine } from '../../lib/types'

interface ResidentStatement { resident: ResidentSummary; summary: FinanceSummary; lines: StatementLine[] }

export default function FinanceReport() {
  const qc = useQueryClient()
  const q = useQuery({ queryKey: ['finance', 'report'], queryFn: () => api<Report>('/api/finance/report') })
  const [selected, setSelected] = useState<number | null>(null)
  const rent = useMutation({
    mutationFn: () => api<{ month: string; posted: number }>('/api/finance/rent', { body: {} }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['finance'] }),
  })
  const r = q.data
  return (
    <div className="space-y-6">
      <PageHeader title="Financial report" subtitle="Resident balances from the append-only ledger."
        action={<Button variant="secondary" loading={rent.isPending} onClick={() => rent.mutate()}>Post this month's rent</Button>} />
      {rent.data && <p className="rounded-md bg-forest-soft px-3 py-2 text-sm text-forest">Rent for {rent.data.month}: {rent.data.posted} new charge(s) posted. Running it again never double-charges.</p>}
      <ErrorNote error={rent.error} />
      {q.isLoading ? <Spinner /> : q.error || !r ? <ErrorNote error={q.error} /> : (
        <>
          <div className="grid gap-3 sm:grid-cols-3">
            <Stat label="Total charged" value={money(r.totalCharges)} />
            <Stat label="Total received" value={money(r.totalPayments)} tone="good" />
            <Stat label="Outstanding" value={money(r.totalOutstanding)} tone="warn" hint="Sum of positive balances" />
          </div>
          <Card>
            <CardHeader title="Resident balances" subtitle="Highest dues first. Select a resident to see their statement." />
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead className="border-b border-line text-xs uppercase tracking-wide text-muted"><tr><th className="px-5 py-3">Resident</th><th className="px-3 py-3 text-right">Charges</th><th className="px-3 py-3 text-right">Payments</th><th className="px-3 py-3 text-right">Credits</th><th className="px-3 py-3 text-right">Outstanding</th><th className="px-5 py-3" /></tr></thead>
                <tbody className="divide-y divide-line">
                  {r.balances.map((b) => (
                    <tr key={b.residentId}>
                      <td className="px-5 py-3"><span className="font-medium">{b.fullName}</span><br /><span className="text-xs text-muted">{b.enrollmentNo}</span></td>
                      <td className="tabular px-3 py-3 text-right">{money(b.summary.totalCharges)}</td>
                      <td className="tabular px-3 py-3 text-right">{money(b.summary.totalPayments)}</td>
                      <td className="tabular px-3 py-3 text-right">{money(b.summary.totalCredits)}</td>
                      <td className={`tabular px-3 py-3 text-right font-semibold ${b.summary.outstanding > 0 ? 'text-clay' : 'text-forest'}`}>{money(b.summary.outstanding)}</td>
                      <td className="px-5 py-3 text-right"><Button size="sm" variant="secondary" onClick={() => setSelected(b.residentId)}>Statement</Button></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </Card>
        </>
      )}
      {selected !== null && <StatementModal residentId={selected} onClose={() => setSelected(null)} />}
    </div>
  )
}

function StatementModal({ residentId, onClose }: { residentId: number; onClose: () => void }) {
  const qc = useQueryClient()
  const q = useQuery({ queryKey: ['finance', 'resident', residentId], queryFn: () => api<ResidentStatement>(`/api/finance/residents/${residentId}/statement`) })
  const [amount, setAmount] = useState('')
  const refresh = () => qc.invalidateQueries({ queryKey: ['finance'] })
  const pay = useMutation({ mutationFn: () => api(`/api/finance/residents/${residentId}/payments`, { body: { amount: Number(amount), method: 'cash', note: 'Recorded by admin' } }), onSuccess: () => { setAmount(''); refresh() } })
  const reverse = useMutation({ mutationFn: (id: number) => api(`/api/finance/entries/${id}/reverse`, { body: { reason: 'Admin correction' } }), onSuccess: refresh })
  const reversed = new Set((q.data?.lines ?? []).filter((l) => l.sourceRef.startsWith('REVERSAL-')).map((l) => Number(l.sourceRef.replace('REVERSAL-', ''))))
  return (
    <Modal title={q.data ? `${q.data.resident.fullName} · statement` : 'Statement'} onClose={onClose}>
      {q.isLoading ? <Spinner /> : q.data && (
        <div className="space-y-4">
          <div className="grid grid-cols-2 gap-2 text-sm">
            <Badge tone="gray">Charges {moneyExact(q.data.summary.totalCharges)}</Badge><Badge tone="green">Paid {moneyExact(q.data.summary.totalPayments)}</Badge>
            <Badge tone="blue">Credits {moneyExact(q.data.summary.totalCredits)}</Badge><Badge tone={q.data.summary.outstanding > 0 ? 'red' : 'green'}>Due {moneyExact(q.data.summary.outstanding)}</Badge>
          </div>
          <form className="flex items-end gap-2" onSubmit={(e) => { e.preventDefault(); pay.mutate() }}>
            <Field label="Record a payment (₹)"><input className={inputCls} type="number" min={1} step="0.01" value={amount} onChange={(e) => setAmount(e.target.value)} required /></Field>
            <Button type="submit" loading={pay.isPending}>Record</Button>
          </form>
          <ErrorNote error={pay.error ?? reverse.error} />
          <ul className="max-h-72 divide-y divide-line overflow-auto rounded-md border border-line text-sm">
            {q.data.lines.slice(0, 40).map((l) => (
              <li key={l.id} className="flex items-center justify-between gap-3 px-3 py-2">
                <div className="min-w-0"><p className="truncate">{l.description}</p><p className="text-xs text-muted">{l.reference} · {fmtDateTime(l.createdAt)}</p></div>
                <div className="flex shrink-0 items-center gap-2">
                  <span className={`tabular font-medium ${l.signedAmount < 0 ? 'text-forest' : ''}`}>{l.signedAmount < 0 ? '−' : ''}{moneyExact(l.amount)}</span>
                  {!l.sourceRef.startsWith('REVERSAL-') && !reversed.has(l.id) && <button className="text-xs text-clay underline" onClick={() => confirm('Post a reversing entry? The original line stays on record.') && reverse.mutate(l.id)}>Reverse</button>}
                </div>
              </li>
            ))}
          </ul>
        </div>
      )}
    </Modal>
  )
}
