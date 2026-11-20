import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Badge, Button, Card, Empty, ErrorNote, Field, Modal, PageHeader, Spinner, inputCls, statusTone } from '../../components/ui'
import { api } from '../../lib/api'
import { fmtDay, titleCase } from '../../lib/format'
import type { Cleaning } from '../../lib/types'

export default function HousekeepingJobs() {
  const qc = useQueryClient()
  const q = useQuery({ queryKey: ['cleaning', 'board'], queryFn: () => api<Cleaning[]>('/api/cleaning/board') })
  const [completing, setCompleting] = useState<Cleaning | null>(null)
  const refresh = () => ['cleaning', 'dashboard'].forEach((k) => qc.invalidateQueries({ queryKey: [k] }))
  const claim = useMutation({ mutationFn: (id: number) => api(`/api/cleaning/${id}/claim`, { method: 'POST' }), onSuccess: refresh })
  const start = useMutation({ mutationFn: (id: number) => api(`/api/cleaning/${id}/status`, { method: 'PATCH', body: { status: 'IN_PROGRESS' } }), onSuccess: refresh })

  const byDate = new Map<string, Cleaning[]>()
  ;(q.data ?? []).forEach((c) => byDate.set(c.date, [...(byDate.get(c.date) ?? []), c]))
  return (
    <div className="space-y-6">
      <PageHeader title="Cleaning jobs" subtitle="Your schedule for the next 7 days, plus unassigned jobs you can pick up." />
      <ErrorNote error={claim.error ?? start.error} />
      {q.isLoading ? <Spinner /> : q.error ? <ErrorNote error={q.error} /> : byDate.size === 0 ? <Card><Empty title="No jobs in this window" /></Card> :
        [...byDate.entries()].map(([date, jobs]) => (
          <section key={date}>
            <h2 className="mb-2 text-lg font-medium text-forest-dark">{fmtDay(date)}</h2>
            <Card>
              <ul className="divide-y divide-line">
                {jobs.map((c) => (
                  <li key={c.id} className="flex flex-wrap items-center justify-between gap-3 px-5 py-3.5">
                    <div className="min-w-0">
                      <p className="text-sm font-semibold">{c.roomLabel} <span className="font-normal text-muted">· {c.slotLabel}</span></p>
                      <p className="text-[13px] text-muted">{c.residentName} ({c.enrollmentNo}){c.instructions ? ` · “${c.instructions}”` : ''}</p>
                      {c.completionNotes && <p className="text-[13px] text-forest">Notes: {c.completionNotes}</p>}
                    </div>
                    <div className="flex items-center gap-2">
                      <Badge tone={statusTone(c.status)}>{titleCase(c.status)}</Badge>
                      {c.status === 'SCHEDULED' && <Button size="sm" onClick={() => claim.mutate(c.id)}>Take this job</Button>}
                      {c.status === 'ASSIGNED' && <Button size="sm" onClick={() => start.mutate(c.id)}>Start</Button>}
                      {c.status === 'IN_PROGRESS' && <Button size="sm" onClick={() => setCompleting(c)}>Mark completed</Button>}
                    </div>
                  </li>
                ))}
              </ul>
            </Card>
          </section>
        ))}
      {completing && <CompleteModal job={completing} onClose={() => setCompleting(null)} onDone={refresh} />}
    </div>
  )
}

function CompleteModal({ job, onClose, onDone }: { job: Cleaning; onClose: () => void; onDone: () => void }) {
  const [notes, setNotes] = useState('')
  const save = useMutation({
    mutationFn: () => api(`/api/cleaning/${job.id}/status`, { method: 'PATCH', body: { status: 'COMPLETED', notes } }),
    onSuccess: () => { onDone(); onClose() },
  })
  return (
    <Modal title={`Complete cleaning · ${job.roomLabel}`} onClose={onClose}>
      <form className="space-y-4" onSubmit={(e) => { e.preventDefault(); save.mutate() }}>
        <Field label="Completion notes (optional)"><textarea className={inputCls} rows={3} maxLength={300} value={notes} onChange={(e) => setNotes(e.target.value)} placeholder="Anything the resident should know?" /></Field>
        <ErrorNote error={save.error} />
        <div className="flex justify-end gap-2"><Button type="button" variant="ghost" onClick={onClose}>Cancel</Button><Button type="submit" loading={save.isPending}>Mark completed</Button></div>
      </form>
    </Modal>
  )
}
