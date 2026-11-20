import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { StudentCheckout } from '../../components/CheckoutPanels'
import { Badge, Button, Card, CardHeader, Empty, ErrorNote, Field, PageHeader, Spinner, Tabs, inputCls, statusTone } from '../../components/ui'
import { api } from '../../lib/api'
import { addDays, fmtDateTime, fmtDay, iso, titleCase } from '../../lib/format'
import type { Cleaning, Complaint, ComplaintCategory, ComplaintPriority, SlotAvailability } from '../../lib/types'

type Tab = 'cleaning' | 'report' | 'track' | 'checkout'

export default function RoomServices() {
  const [params, setParams] = useSearchParams()
  const tab = (params.get('tab') as Tab) || 'cleaning'
  return (
    <div>
      <PageHeader title="Room services" subtitle="Get your room cleaned, report a problem, and follow every request." />
      <Tabs<Tab> tabs={[{ id: 'cleaning', label: 'Schedule cleaning' }, { id: 'report', label: 'Report a problem' }, { id: 'track', label: 'Track requests' }, { id: 'checkout', label: 'Checkout' }]}
        value={tab} onChange={(t) => setParams({ tab: t })} />
      {tab === 'cleaning' && <CleaningForm />}
      {tab === 'report' && <ReportForm onDone={() => setParams({ tab: 'track' })} />}
      {tab === 'track' && <Tracking />}
      {tab === 'checkout' && <StudentCheckout />}
    </div>
  )
}

function CleaningForm() {
  const qc = useQueryClient()
  const today = new Date()
  const [date, setDate] = useState(iso(addDays(today, 1)))
  const [slot, setSlot] = useState('')
  const [notes, setNotes] = useState('')
  const slots = useQuery({ queryKey: ['cleaning', 'slots', date], queryFn: () => api<SlotAvailability[]>(`/api/cleaning/slots?date=${date}`) })
  const book = useMutation({
    mutationFn: () => api<Cleaning>('/api/cleaning', { body: { date, slot, instructions: notes } }),
    onSuccess: () => { setSlot(''); setNotes(''); ['cleaning', 'dashboard'].forEach((k) => qc.invalidateQueries({ queryKey: [k] })) },
  })
  return (
    <div className="grid gap-5 lg:grid-cols-[1fr_1fr]">
      <Card>
        <CardHeader title="Book a slot" subtitle="Slots already taken for your room are disabled." />
        <form className="space-y-4 p-5" onSubmit={(e) => { e.preventDefault(); book.mutate() }}>
          <Field label="Date">
            <input className={inputCls} type="date" min={iso(today)} max={iso(addDays(today, 14))} value={date} onChange={(e) => { setDate(e.target.value); setSlot('') }} required />
          </Field>
          <div>
            <p className="mb-1 text-sm font-medium">Time slot</p>
            {slots.isLoading ? <Spinner /> : slots.error ? <ErrorNote error={slots.error} /> : (
              <div className="grid grid-cols-2 gap-2" role="radiogroup" aria-label="Time slot">
                {slots.data!.map((s) => (
                  <button type="button" key={s.slot} role="radio" aria-checked={slot === s.slot} disabled={!s.available} onClick={() => setSlot(s.slot)}
                    className={`rounded-md border px-3 py-2.5 text-left text-sm transition-colors ${slot === s.slot ? 'border-forest bg-forest text-ivory' : 'border-line bg-paper hover:bg-forest-soft'} disabled:cursor-not-allowed disabled:bg-black/5 disabled:text-muted disabled:line-through`}>
                    {s.label}
                  </button>
                ))}
              </div>
            )}
          </div>
          <Field label="Special instructions (optional)">
            <textarea className={inputCls} rows={3} maxLength={300} value={notes} onChange={(e) => setNotes(e.target.value)} placeholder="e.g. Please do not move the books on my desk" />
          </Field>
          <ErrorNote error={book.error} />
          {book.isSuccess && <p className="rounded-md bg-forest-soft px-3 py-2 text-sm text-forest">Booked. You can follow it under Track requests.</p>}
          <Button type="submit" disabled={!slot} loading={book.isPending} className="w-full">Confirm booking</Button>
        </form>
      </Card>
      <MyCleaning />
    </div>
  )
}

function MyCleaning() {
  const qc = useQueryClient()
  const q = useQuery({ queryKey: ['cleaning', 'mine'], queryFn: () => api<Cleaning[]>('/api/cleaning/mine') })
  const cancel = useMutation({
    mutationFn: (id: number) => api(`/api/cleaning/${id}/cancel`, { method: 'POST' }),
    onSuccess: () => ['cleaning', 'dashboard'].forEach((k) => qc.invalidateQueries({ queryKey: [k] })),
  })
  return (
    <Card className="h-fit">
      <CardHeader title="Your cleaning bookings" />
      {q.isLoading ? <Spinner /> : (q.data ?? []).length === 0 ? <Empty title="No cleaning booked" /> : (
        <ul className="divide-y divide-line">
          {q.data!.map((c) => (
            <li key={c.id} className="flex items-start justify-between gap-3 px-5 py-3">
              <div>
                <p className="text-sm font-medium">{fmtDay(c.date)} · {c.slotLabel}</p>
                <p className="text-xs text-muted">{c.assignedToName ? `Assigned to ${c.assignedToName}` : 'Awaiting assignment'}{c.completionNotes ? ` · ${c.completionNotes}` : ''}</p>
              </div>
              <div className="flex flex-col items-end gap-1.5">
                <Badge tone={statusTone(c.status)}>{titleCase(c.status)}</Badge>
                {c.cancellable && <button className="text-xs text-clay underline" onClick={() => cancel.mutate(c.id)}>Cancel</button>}
              </div>
            </li>
          ))}
        </ul>
      )}
      {cancel.error && <div className="p-4"><ErrorNote error={cancel.error} /></div>}
    </Card>
  )
}

const categories: ComplaintCategory[] = ['ELECTRICAL', 'PLUMBING', 'FURNITURE', 'INTERNET', 'CLEANLINESS', 'OTHER']
const priorities: ComplaintPriority[] = ['LOW', 'MEDIUM', 'HIGH', 'URGENT']

function ReportForm({ onDone }: { onDone: () => void }) {
  const qc = useQueryClient()
  const [category, setCategory] = useState<ComplaintCategory>('ELECTRICAL')
  const [priority, setPriority] = useState<ComplaintPriority>('MEDIUM')
  const [description, setDescription] = useState('')
  const submit = useMutation({
    mutationFn: () => api('/api/maintenance', { body: { category, priority, description } }),
    onSuccess: () => { ['maintenance', 'dashboard'].forEach((k) => qc.invalidateQueries({ queryKey: [k] })); onDone() },
  })
  return (
    <Card className="max-w-xl">
      <CardHeader title="Report a problem" subtitle="It goes straight to the maintenance team for your hostel." />
      <form className="space-y-4 p-5" onSubmit={(e) => { e.preventDefault(); submit.mutate() }}>
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Category"><select className={inputCls} value={category} onChange={(e) => setCategory(e.target.value as ComplaintCategory)}>{categories.map((c) => <option key={c} value={c}>{titleCase(c)}</option>)}</select></Field>
          <Field label="Priority"><select className={inputCls} value={priority} onChange={(e) => setPriority(e.target.value as ComplaintPriority)}>{priorities.map((p) => <option key={p} value={p}>{titleCase(p)}</option>)}</select></Field>
        </div>
        <Field label="What is wrong?"><textarea className={inputCls} rows={4} maxLength={500} required value={description} onChange={(e) => setDescription(e.target.value)} placeholder="Describe the issue and where it is" /></Field>
        <ErrorNote error={submit.error} />
        <Button type="submit" loading={submit.isPending} disabled={!description.trim()}>Submit complaint</Button>
      </form>
    </Card>
  )
}

function Tracking() {
  const qc = useQueryClient()
  const q = useQuery({ queryKey: ['maintenance', 'mine'], queryFn: () => api<Complaint[]>('/api/maintenance/mine') })
  const close = useMutation({ mutationFn: (id: number) => api(`/api/maintenance/${id}/close`, { method: 'POST' }), onSuccess: () => ['maintenance', 'dashboard'].forEach((k) => qc.invalidateQueries({ queryKey: [k] })) })
  return (
    <div className="space-y-4">
      <MyCleaning />
      <Card>
        <CardHeader title="Your complaints" />
        {q.isLoading ? <Spinner /> : (q.data ?? []).length === 0 ? <Empty title="No complaints raised" /> : (
          <ul className="divide-y divide-line">
            {q.data!.map((c) => (
              <li key={c.id} className="px-5 py-4">
                <div className="flex flex-wrap items-center gap-2">
                  <Badge tone={statusTone(c.status)}>{titleCase(c.status)}</Badge>
                  <Badge tone={statusTone(c.priority)}>{titleCase(c.priority)}</Badge>
                  <span className="text-sm font-medium">{titleCase(c.category)}</span>
                  <span className="text-xs text-muted">· {fmtDateTime(c.createdAt)}</span>
                </div>
                <p className="mt-1.5 text-sm">{c.description}</p>
                <p className="mt-1 text-xs text-muted">{c.assignedToName ? `Handled by ${c.assignedToName}` : 'Waiting to be picked up'}</p>
                {c.resolutionNotes && <p className="mt-1 rounded-md bg-forest-soft px-3 py-2 text-[13px] text-forest">Resolution: {c.resolutionNotes}</p>}
                {c.status === 'RESOLVED' && <Button size="sm" variant="secondary" className="mt-2" onClick={() => close.mutate(c.id)}>Confirm fixed and close</Button>}
              </li>
            ))}
          </ul>
        )}
        {close.error && <div className="p-4"><ErrorNote error={close.error} /></div>}
      </Card>
    </div>
  )
}
