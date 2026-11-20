import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { CheckoutQueue } from '../../components/CheckoutPanels'
import { Badge, Button, Card, Empty, ErrorNote, PageHeader, Spinner, Tabs, inputCls, statusTone } from '../../components/ui'
import { api } from '../../lib/api'
import { addDays, fmtDateTime, fmtDay, iso, titleCase } from '../../lib/format'
import type { Cleaning, Complaint, Staff } from '../../lib/types'

type Tab = 'cleaning' | 'maintenance' | 'checkout'

export default function Requests() {
  const [tab, setTab] = useState<Tab>('maintenance')
  return (
    <div>
      <PageHeader title="Service requests" subtitle="Monitor cleaning, maintenance and checkouts across the hostel and assign work to staff." />
      <Tabs<Tab> tabs={[{ id: 'maintenance', label: 'Maintenance complaints' }, { id: 'cleaning', label: 'Cleaning schedule' }, { id: 'checkout', label: 'Checkouts' }]} value={tab} onChange={setTab} />
      {tab === 'maintenance' ? <MaintenanceTab /> : tab === 'cleaning' ? <CleaningTab /> : <CheckoutQueue />}
    </div>
  )
}

function AssignSelect({ staff, current, onAssign, busy }: { staff: Staff[]; current?: number; onAssign: (id: number) => void; busy?: boolean }) {
  return (
    <select aria-label="Assign to" className={`${inputCls} !w-auto !py-1.5 text-[13px]`} disabled={busy} value={current ?? ''} onChange={(e) => e.target.value && onAssign(Number(e.target.value))}>
      <option value="">Assign to…</option>
      {staff.map((s) => <option key={s.id} value={s.id}>{s.fullName}</option>)}
    </select>
  )
}

function MaintenanceTab() {
  const qc = useQueryClient()
  const q = useQuery({ queryKey: ['maintenance', 'board'], queryFn: () => api<Complaint[]>('/api/maintenance/board') })
  const staff = useQuery({ queryKey: ['staff', 'MAINTENANCE_STAFF'], queryFn: () => api<Staff[]>('/api/users/staff?role=MAINTENANCE_STAFF') })
  const [status, setStatus] = useState<'active' | 'all'>('active')
  const refresh = () => ['maintenance', 'dashboard'].forEach((k) => qc.invalidateQueries({ queryKey: [k] }))
  const assign = useMutation({ mutationFn: (v: { id: number; staffId: number }) => api(`/api/maintenance/${v.id}/assign`, { body: { staffId: v.staffId } }), onSuccess: refresh })
  const close = useMutation({ mutationFn: (id: number) => api(`/api/maintenance/${id}/status`, { method: 'PATCH', body: { status: 'CLOSED' } }), onSuccess: refresh })
  if (q.isLoading) return <Spinner />
  const rows = (q.data ?? []).filter((c) => status === 'all' || !['CLOSED'].includes(c.status))
  return (
    <div className="space-y-3">
      <ErrorNote error={assign.error ?? close.error ?? q.error} />
      <label className="flex items-center gap-2 text-sm"><input type="checkbox" checked={status === 'all'} onChange={(e) => setStatus(e.target.checked ? 'all' : 'active')} /> Include closed complaints</label>
      <Card>
        {rows.length === 0 ? <Empty title="No complaints" /> : (
          <ul className="divide-y divide-line">
            {rows.map((c) => (
              <li key={c.id} className="flex flex-wrap items-start justify-between gap-3 px-5 py-4">
                <div className="min-w-0 max-w-xl">
                  <div className="flex flex-wrap items-center gap-2"><Badge tone={statusTone(c.priority)}>{titleCase(c.priority)}</Badge><Badge tone={statusTone(c.status)}>{titleCase(c.status)}</Badge><span className="text-sm font-semibold">{titleCase(c.category)}</span><span className="text-xs text-muted">· {c.roomLabel}</span></div>
                  <p className="mt-1.5 text-sm">{c.description}</p>
                  <p className="mt-1 text-xs text-muted">{c.residentName} · {fmtDateTime(c.createdAt)}{c.assignedToName ? ` · ${c.assignedToName}` : ''}</p>
                  {c.resolutionNotes && <p className="mt-1 text-[13px] text-forest">Resolution: {c.resolutionNotes}</p>}
                </div>
                <div className="flex items-center gap-2">
                  {(c.status === 'OPEN' || c.status === 'ASSIGNED') && <AssignSelect staff={staff.data ?? []} current={c.assignedToId} busy={assign.isPending} onAssign={(staffId) => assign.mutate({ id: c.id, staffId })} />}
                  {c.status === 'RESOLVED' && <Button size="sm" variant="secondary" onClick={() => close.mutate(c.id)}>Close</Button>}
                </div>
              </li>
            ))}
          </ul>
        )}
      </Card>
    </div>
  )
}

function CleaningTab() {
  const qc = useQueryClient()
  const from = iso(addDays(new Date(), -3)), to = iso(addDays(new Date(), 14))
  const q = useQuery({ queryKey: ['cleaning', 'board', 'all'], queryFn: () => api<Cleaning[]>(`/api/cleaning/board?from=${from}&to=${to}`) })
  const staff = useQuery({ queryKey: ['staff', 'HOUSEKEEPING_STAFF'], queryFn: () => api<Staff[]>('/api/users/staff?role=HOUSEKEEPING_STAFF') })
  const assign = useMutation({ mutationFn: (v: { id: number; staffId: number }) => api(`/api/cleaning/${v.id}/assign`, { body: { staffId: v.staffId } }), onSuccess: () => ['cleaning', 'dashboard'].forEach((k) => qc.invalidateQueries({ queryKey: [k] })) })
  if (q.isLoading) return <Spinner />
  return (
    <div className="space-y-3">
      <ErrorNote error={assign.error ?? q.error} />
      <Card>
        {(q.data ?? []).length === 0 ? <Empty title="No cleaning requests in this window" /> : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="border-b border-line text-xs uppercase tracking-wide text-muted"><tr><th className="px-5 py-3">When</th><th className="px-3 py-3">Room</th><th className="px-3 py-3">Resident</th><th className="px-3 py-3">Status</th><th className="px-5 py-3">Staff</th></tr></thead>
              <tbody className="divide-y divide-line">
                {q.data!.map((c) => (
                  <tr key={c.id}>
                    <td className="whitespace-nowrap px-5 py-3">{fmtDay(c.date)}<br /><span className="text-xs text-muted">{c.slotLabel}</span></td>
                    <td className="px-3 py-3">{c.roomLabel}{c.instructions && <span className="block max-w-[220px] truncate text-xs text-muted">“{c.instructions}”</span>}</td>
                    <td className="px-3 py-3 text-muted">{c.residentName}</td>
                    <td className="px-3 py-3"><Badge tone={statusTone(c.status)}>{titleCase(c.status)}</Badge></td>
                    <td className="px-5 py-3">{['SCHEDULED', 'ASSIGNED'].includes(c.status) ? <AssignSelect staff={staff.data ?? []} current={c.assignedToId} busy={assign.isPending} onAssign={(staffId) => assign.mutate({ id: c.id, staffId })} /> : <span className="text-muted">{c.assignedToName ?? '–'}</span>}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>
    </div>
  )
}
