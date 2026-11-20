import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Badge, Button, Card, CardHeader, Empty, ErrorNote, Field, PageHeader, Spinner, Stat, inputCls } from '../../components/ui'
import { api } from '../../lib/api'
import { fmtDateTime, money, moneyExact } from '../../lib/format'
import type { FinanceSummary, StatementLine } from '../../lib/types'

const typeLabel: Record<string, string> = {
  CHARGE: 'Charge', PAYMENT: 'Payment', REFUND: 'Refund', ADJUSTMENT_DEBIT: 'Adjustment', ADJUSTMENT_CREDIT: 'Adjustment',
}

export default function Account() {
  const qc = useQueryClient()
  const summary = useQuery({ queryKey: ['account', 'summary'], queryFn: () => api<FinanceSummary>('/api/finance/me/summary') })
  const statement = useQuery({ queryKey: ['account', 'statement'], queryFn: () => api<StatementLine[]>('/api/finance/me/statement') })
  const [amount, setAmount] = useState('')
  const pay = useMutation({
    mutationFn: () => api('/api/finance/me/payments', { body: { amount: Number(amount), method: 'UPI', note: 'Paid from HostelOS' } }),
    onSuccess: () => { setAmount(''); ['account', 'dashboard'].forEach((k) => qc.invalidateQueries({ queryKey: [k] })) },
  })
  const s = summary.data
  return (
    <div className="space-y-6">
      <PageHeader title="Account statement" subtitle="Every meal, rent charge and payment in one auditable ledger." />
      {summary.isLoading ? <Spinner /> : s && (
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
          <Stat label="Total charges" value={money(s.totalCharges)} hint="Meals eaten, rent, services" />
          <Stat label="Payments made" value={money(s.totalPayments)} />
          <Stat label="Credits & refunds" value={money(s.totalCredits)} />
          <Stat label="Outstanding" value={money(s.outstanding)} tone={s.outstanding > 0 ? 'warn' : 'good'} hint={s.outstanding > 0 ? 'Amount due' : 'All settled'} />
        </div>
      )}

      <div className="grid gap-5 lg:grid-cols-[1fr_320px]">
        <Card>
          <CardHeader title="Transactions" subtitle="Newest first. Entries are never edited: corrections appear as new lines." />
          {statement.isLoading ? <Spinner /> : statement.error ? <div className="p-4"><ErrorNote error={statement.error} /></div> : (statement.data ?? []).length === 0 ? <Empty title="No transactions yet" /> : (
            <div className="max-h-[640px] overflow-auto">
              <table className="w-full text-left text-sm">
                <thead className="sticky top-0 border-b border-line bg-paper text-xs uppercase tracking-wide text-muted">
                  <tr><th className="px-5 py-3">Date</th><th className="px-3 py-3">Description</th><th className="px-3 py-3 text-right">Amount</th><th className="px-5 py-3 text-right">Balance</th></tr>
                </thead>
                <tbody className="divide-y divide-line">
                  {statement.data!.map((l) => (
                    <tr key={l.id}>
                      <td className="whitespace-nowrap px-5 py-3 text-muted">{fmtDateTime(l.createdAt)}<br /><span className="text-[11px]">{l.reference}</span></td>
                      <td className="px-3 py-3"><Badge tone={l.signedAmount > 0 ? 'amber' : 'green'}>{typeLabel[l.type]}</Badge><span className="ml-2">{l.description}</span></td>
                      <td className={`tabular whitespace-nowrap px-3 py-3 text-right font-medium ${l.signedAmount > 0 ? 'text-ink' : 'text-forest'}`}>{l.signedAmount > 0 ? '' : '−'}{moneyExact(l.amount)}</td>
                      <td className="tabular whitespace-nowrap px-5 py-3 text-right text-muted">{moneyExact(l.runningBalance)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </Card>

        <Card className="h-fit">
          <CardHeader title="Make a payment" subtitle="Simulated. No real money moves in this prototype." />
          <form className="space-y-3 p-5" onSubmit={(e) => { e.preventDefault(); pay.mutate() }}>
            <Field label="Amount (₹)">
              <input className={inputCls} type="number" min={1} step="0.01" value={amount} onChange={(e) => setAmount(e.target.value)} placeholder={s && s.outstanding > 0 ? String(s.outstanding) : '500'} required />
            </Field>
            {s && s.outstanding > 0 && <button type="button" className="text-xs font-medium text-forest underline" onClick={() => setAmount(String(s.outstanding))}>Pay full outstanding ({money(s.outstanding)})</button>}
            <ErrorNote error={pay.error} />
            <Button type="submit" className="w-full" loading={pay.isPending}>Record simulated UPI payment</Button>
          </form>
        </Card>
      </div>
    </div>
  )
}
