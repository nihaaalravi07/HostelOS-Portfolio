import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Pencil } from 'lucide-react'
import { useState } from 'react'
import { Badge, Button, Card, ErrorNote, Field, Modal, PageHeader, Spinner, inputCls } from '../../components/ui'
import { api } from '../../lib/api'
import { addDays, fmtDateTime, fmtDay, iso, money } from '../../lib/format'
import type { Meal } from '../../lib/types'

export default function MenuManager() {
  const from = iso(new Date()), to = iso(addDays(new Date(), 6))
  const q = useQuery({ queryKey: ['mess', 'menu', from], queryFn: () => api<Meal[]>(`/api/mess/menu?from=${from}&to=${to}`) })
  const [editing, setEditing] = useState<Meal | null>(null)
  return (
    <div>
      <PageHeader title="Menu & prices" subtitle="Prices are stored per meal. Changing one never alters what residents were already billed." />
      {q.isLoading ? <Spinner /> : q.error ? <ErrorNote error={q.error} /> : (
        <Card>
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="border-b border-line text-xs uppercase tracking-wide text-muted"><tr><th className="px-5 py-3">Meal</th><th className="px-3 py-3">Menu</th><th className="px-3 py-3 text-right">Price</th><th className="px-3 py-3">Booking cutoff</th><th className="px-3 py-3">State</th><th className="px-5 py-3" /></tr></thead>
              <tbody className="divide-y divide-line">
                {q.data!.map((m) => (
                  <tr key={m.id}>
                    <td className="whitespace-nowrap px-5 py-3"><span className="font-medium">{m.categoryLabel}</span><br /><span className="text-xs text-muted">{fmtDay(m.date)}</span></td>
                    <td className="max-w-[320px] px-3 py-3 text-muted">{m.menuItems.join(', ')}</td>
                    <td className="tabular px-3 py-3 text-right font-medium">{money(m.price)}</td>
                    <td className="whitespace-nowrap px-3 py-3 text-muted">{fmtDateTime(m.cutoffTime)}</td>
                    <td className="px-3 py-3"><Badge tone={!m.available ? 'red' : m.bookingOpen ? 'green' : 'gray'}>{!m.available ? 'Withdrawn' : m.bookingOpen ? 'Booking open' : 'Closed'}</Badge></td>
                    <td className="px-5 py-3 text-right"><Button size="sm" variant="secondary" onClick={() => setEditing(m)}><Pencil className="h-3.5 w-3.5" /> Edit</Button></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      )}
      {editing && <EditModal meal={editing} onClose={() => setEditing(null)} />}
    </div>
  )
}

function EditModal({ meal, onClose }: { meal: Meal; onClose: () => void }) {
  const qc = useQueryClient()
  const [items, setItems] = useState(meal.menuItems.join(', '))
  const [price, setPrice] = useState(String(meal.price))
  const [cutoff, setCutoff] = useState(meal.cutoffTime.slice(0, 16))
  const [available, setAvailable] = useState(meal.available)
  const save = useMutation({
    mutationFn: () => api(`/api/mess/meals/${meal.id}`, { method: 'PUT', body: { menuItems: items, price: Number(price), cutoffTime: cutoff.length === 16 ? cutoff + ':00' : cutoff, available } }),
    onSuccess: () => { ['mess', 'meals', 'dashboard'].forEach((k) => qc.invalidateQueries({ queryKey: [k] })); onClose() },
  })
  return (
    <Modal title={`${meal.categoryLabel} · ${fmtDay(meal.date)}`} onClose={onClose}>
      <form className="space-y-4" onSubmit={(e) => { e.preventDefault(); save.mutate() }}>
        <Field label="Menu items" hint="Separate dishes with commas."><textarea className={inputCls} rows={3} value={items} onChange={(e) => setItems(e.target.value)} required /></Field>
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Price per plate (₹)"><input className={inputCls} type="number" min={0} step="0.5" value={price} onChange={(e) => setPrice(e.target.value)} required /></Field>
          <Field label="Booking cutoff"><input className={inputCls} type="datetime-local" value={cutoff} onChange={(e) => setCutoff(e.target.value)} required /></Field>
        </div>
        <label className="flex items-center gap-2 text-sm"><input type="checkbox" checked={available} onChange={(e) => setAvailable(e.target.checked)} /> Available for booking</label>
        <ErrorNote error={save.error} />
        <div className="flex justify-end gap-2"><Button type="button" variant="ghost" onClick={onClose}>Cancel</Button><Button type="submit" loading={save.isPending}>Save changes</Button></div>
      </form>
    </Modal>
  )
}
