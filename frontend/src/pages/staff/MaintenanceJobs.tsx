import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Badge, Button, Card, Empty, ErrorNote, Field, Modal, PageHeader, Spinner, inputCls, statusTone } from '../../components/ui'
import { api } from '../../lib/api'
import { fmtDateTime, titleCase } from '../../lib/format'
import type { Complaint } from '../../lib/types'

export default function MaintenanceJobs() {
  const qc = useQueryClient()
  const q = useQuery({ queryKey: ['maintenance', 'board'], queryFn: () => api<Complaint[]>('/api/maintenance/board') })
  const [resolving, setResolving] = useState<Complaint | null>(null)
  const refresh = () => ['maintenance', 'dashboard'].forEach((k) => qc.invalidateQueries({ queryKey: [k] }))
  const claim = useMutation({ mutationFn: (id: number) => api(`/api/maintenance/${id}/claim`, { method: 'POST' }), onSuccess: refresh })
  const start = useMutation({ mutationFn: (id: number) => api(`/api/maintenance/${id}/status`, { method: 'PATCH', body: { status: 'IN_PROGRESS' } }), onSuccess: refresh })
  return (
    <div className="space-y-6">
      <PageHeader title="Complaints" subtitle="Unassigned complaints you can pick up, and the ones you are handling. Most urgent first." />
      <ErrorNote error={claim.error ?? start.error} />
      {q.isLoading ? <Spinner /> : q.error ? <ErrorNote error={q.error} /> : (q.data ?? []).length === 0 ? <Card><Empty title="Nothing to do right now" /></Card> : (
        <Card>
          <ul className="divide-y divide-line">
            {q.data!.map((c) => (
              <li key={c.id} className="px-5 py-4">
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div className="min-w-0">
                    <div className="flex flex-wrap items-center gap-2">
                      <Badge tone={statusTone(c.priority)}>{titleCase(c.priority)}</Badge>
                      <Badge tone={statusTone(c.status)}>{titleCase(c.status)}</Badge>
                      <span className="text-sm font-semibold">{titleCase(c.category)}</span>
                      <span className="text-xs text-muted">· {c.roomLabel}</span>
                    </div>
                    <p className="mt-1.5 text-sm">{c.description}</p>
                    <p className="mt-1 text-xs text-muted">{c.residentName} · {fmtDateTime(c.createdAt)}</p>
                    {c.resolutionNotes && <p className="mt-1.5 text-[13px] text-forest">Resolution: {c.resolutionNotes}</p>}
                  </div>
                  <div className="flex gap-2">
                    {c.status === 'OPEN' && <Button size="sm" onClick={() => claim.mutate(c.id)}>Take this</Button>}
                    {c.status === 'ASSIGNED' && <Button size="sm" onClick={() => start.mutate(c.id)}>Start work</Button>}
                    {c.status === 'IN_PROGRESS' && <Button size="sm" onClick={() => setResolving(c)}>Record resolution</Button>}
                  </div>
                </div>
              </li>
            ))}
          </ul>
        </Card>
      )}
      {resolving && <ResolveModal c={resolving} onClose={() => setResolving(null)} onDone={refresh} />}
    </div>
  )
}

function ResolveModal({ c, onClose, onDone }: { c: Complaint; onClose: () => void; onDone: () => void }) {
  const [notes, setNotes] = useState('')
  const save = useMutation({
    mutationFn: () => api(`/api/maintenance/${c.id}/status`, { method: 'PATCH', body: { status: 'RESOLVED', notes } }),
    onSuccess: () => { onDone(); onClose() },
  })
  return (
    <Modal title={`Resolve · ${titleCase(c.category)} at ${c.roomLabel}`} onClose={onClose}>
      <form className="space-y-4" onSubmit={(e) => { e.preventDefault(); save.mutate() }}>
        <Field label="What was done?"><textarea className={inputCls} rows={4} maxLength={500} required value={notes} onChange={(e) => setNotes(e.target.value)} /></Field>
        <ErrorNote error={save.error} />
        <div className="flex justify-end gap-2"><Button type="button" variant="ghost" onClick={onClose}>Cancel</Button><Button type="submit" loading={save.isPending} disabled={!notes.trim()}>Mark resolved</Button></div>
      </form>
    </Modal>
  )
}
