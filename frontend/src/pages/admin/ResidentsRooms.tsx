import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useAuth } from '../../auth'
import { Badge, Bar, Button, Card, CardHeader, Empty, ErrorNote, Field, Modal, PageHeader, Spinner, Tabs, inputCls } from '../../components/ui'
import { api } from '../../lib/api'
import type { BedSuggestion, HostelView, Occupancy, ResidentRow } from '../../lib/types'

type Tab = 'rooms' | 'residents'

export default function ResidentsRooms() {
  const [tab, setTab] = useState<Tab>('rooms')
  const occ = useQuery({ queryKey: ['accommodation', 'occupancy'], queryFn: () => api<Occupancy>('/api/accommodation/occupancy') })
  return (
    <div>
      <PageHeader title="Residents & rooms" subtitle={occ.data ? `${occ.data.occupiedBeds} of ${occ.data.totalBeds} beds occupied · ${occ.data.availableBeds} available` : undefined} />
      <Tabs<Tab> tabs={[{ id: 'rooms', label: 'Rooms & beds' }, { id: 'residents', label: 'Residents' }]} value={tab} onChange={setTab} />
      {tab === 'rooms' ? <Rooms /> : <Residents />}
    </div>
  )
}

const refreshKeys = ['accommodation']

function Rooms() {
  const qc = useQueryClient()
  const q = useQuery({ queryKey: ['accommodation', 'overview'], queryFn: () => api<HostelView[]>('/api/accommodation/overview') })
  const [adding, setAdding] = useState(false)
  const [releasing, setReleasing] = useState<number | null>(null)
  const release = useMutation({
    mutationFn: (residentId: number) => api('/api/accommodation/release', { body: { residentId } }),
    onSuccess: () => { setReleasing(null); refreshKeys.forEach((k) => qc.invalidateQueries({ queryKey: [k] })) },
  })
  if (q.isLoading) return <Spinner />
  if (q.error) return <ErrorNote error={q.error} />
  return (
    <div className="space-y-8">
      <div className="flex justify-end"><Button variant="secondary" onClick={() => setAdding(true)}>Add a room</Button></div>
      <ErrorNote error={release.error} />
      {q.data!.map((h) => (
        <section key={h.id}>
          <div className="mb-2 flex items-end justify-between gap-4">
            <h2 className="text-xl font-medium text-forest-dark">{h.name}</h2>
            <div className="w-48"><p className="mb-1 text-right text-xs text-muted">{h.occupiedBeds}/{h.totalBeds} beds</p><Bar value={h.occupiedBeds} max={h.totalBeds} /></div>
          </div>
          <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
            {h.rooms.map((r) => (
              <Card key={r.id} className="p-3.5">
                <div className="mb-2 flex items-center justify-between"><span className="font-semibold">Room {r.roomNumber}</span><span className="text-xs text-muted">Floor {r.floorNo}</span></div>
                <ul className="space-y-1.5">
                  {r.beds.map((b) => (
                    <li key={b.id} className="flex items-center justify-between gap-2 rounded-md bg-ivory px-2.5 py-1.5 text-[13px]">
                      <span className="truncate">{b.residentName ?? <span className="text-muted">Bed {b.bedNumber} · free</span>}</span>
                      {b.residentId && (releasing === b.residentId
                        ? <span className="flex gap-1"><button className="text-xs text-clay underline" onClick={() => release.mutate(b.residentId!)}>Confirm</button><button className="text-xs text-muted underline" onClick={() => setReleasing(null)}>No</button></span>
                        : <button className="text-xs text-muted underline" onClick={() => setReleasing(b.residentId!)}>Release</button>)}
                    </li>
                  ))}
                </ul>
              </Card>
            ))}
          </div>
        </section>
      ))}
      {adding && <AddRoomModal hostels={q.data!} onClose={() => setAdding(false)} />}
    </div>
  )
}

function AddRoomModal({ hostels, onClose }: { hostels: HostelView[]; onClose: () => void }) {
  const qc = useQueryClient()
  const [hostelId, setHostelId] = useState(String(hostels[0]?.id ?? ''))
  const [floorNo, setFloorNo] = useState('1')
  const [roomNumber, setRoomNumber] = useState('')
  const [capacity, setCapacity] = useState('2')
  const save = useMutation({
    mutationFn: () => api('/api/accommodation/rooms', { body: { hostelId: Number(hostelId), floorNo: Number(floorNo), roomNumber, capacity: Number(capacity) } }),
    onSuccess: () => { refreshKeys.forEach((k) => qc.invalidateQueries({ queryKey: [k] })); onClose() },
  })
  return (
    <Modal title="Add a room" onClose={onClose}>
      <form className="space-y-4" onSubmit={(e) => { e.preventDefault(); save.mutate() }}>
        <Field label="Hostel"><select className={inputCls} value={hostelId} onChange={(e) => setHostelId(e.target.value)}>{hostels.map((h) => <option key={h.id} value={h.id}>{h.name}</option>)}</select></Field>
        <div className="grid grid-cols-3 gap-3">
          <Field label="Floor"><input className={inputCls} type="number" min={0} value={floorNo} onChange={(e) => setFloorNo(e.target.value)} required /></Field>
          <Field label="Room no."><input className={inputCls} value={roomNumber} onChange={(e) => setRoomNumber(e.target.value)} required /></Field>
          <Field label="Beds"><input className={inputCls} type="number" min={1} max={6} value={capacity} onChange={(e) => setCapacity(e.target.value)} required /></Field>
        </div>
        <ErrorNote error={save.error} />
        <div className="flex justify-end gap-2"><Button type="button" variant="ghost" onClick={onClose}>Cancel</Button><Button type="submit" loading={save.isPending}>Add room</Button></div>
      </form>
    </Modal>
  )
}

function Residents() {
  const { user } = useAuth()
  const q = useQuery({ queryKey: ['accommodation', 'residents'], queryFn: () => api<ResidentRow[]>('/api/accommodation/residents') })
  const [target, setTarget] = useState<{ row: ResidentRow; mode: 'allocate' | 'transfer' } | null>(null)
  const [filter, setFilter] = useState('')
  if (q.isLoading) return <Spinner />
  if (q.error) return <ErrorNote error={q.error} />
  const rows = q.data!.filter((r) => !filter || (r.resident.fullName + r.resident.enrollmentNo).toLowerCase().includes(filter.toLowerCase()))
  return (
    <div className="space-y-4">
      <input className={`${inputCls} max-w-sm`} placeholder="Search by name or enrolment no." aria-label="Search residents" value={filter} onChange={(e) => setFilter(e.target.value)} />
      <Card>
        <CardHeader title={`${rows.length} resident${rows.length === 1 ? '' : 's'}`} subtitle={user?.role === 'ADMIN' ? 'Admins and wardens can allocate and transfer beds.' : undefined} />
        {rows.length === 0 ? <Empty title="No residents match" /> : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="border-b border-line text-xs uppercase tracking-wide text-muted"><tr><th className="px-5 py-3">Resident</th><th className="px-3 py-3">Course</th><th className="px-3 py-3">Accommodation</th><th className="px-5 py-3" /></tr></thead>
              <tbody className="divide-y divide-line">
                {rows.map((r) => (
                  <tr key={r.resident.id}>
                    <td className="px-5 py-3"><span className="font-medium">{r.resident.fullName}</span><br /><span className="text-xs text-muted">{r.resident.enrollmentNo} · {r.resident.email}</span></td>
                    <td className="px-3 py-3 text-muted">{r.resident.course} · Year {r.resident.yearOfStudy}</td>
                    <td className="px-3 py-3">{r.accommodation ? <>{r.accommodation.hostelName}<br /><span className="text-xs text-muted">Room {r.accommodation.roomNumber} · Bed {r.accommodation.bedNumber}</span></> : <Badge tone="amber">Not allocated</Badge>}</td>
                    <td className="px-5 py-3 text-right">
                      {r.resident.status === 'ACTIVE' && (r.accommodation
                        ? <Button size="sm" variant="secondary" onClick={() => setTarget({ row: r, mode: 'transfer' })}>Transfer</Button>
                        : <Button size="sm" onClick={() => setTarget({ row: r, mode: 'allocate' })}>Allocate bed</Button>)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>
      {target && <AllocateModal row={target.row} mode={target.mode} onClose={() => setTarget(null)} />}
    </div>
  )
}

function AllocateModal({ row, mode, onClose }: { row: ResidentRow; mode: 'allocate' | 'transfer'; onClose: () => void }) {
  const qc = useQueryClient()
  const [strategy, setStrategy] = useState<'PACK' | 'SPREAD'>('PACK')
  const [bedId, setBedId] = useState('')
  const rec = useQuery({ queryKey: ['accommodation', 'recommend', row.resident.id, strategy], queryFn: () => api<BedSuggestion[]>(`/api/accommodation/recommend?residentId=${row.resident.id}&strategy=${strategy}`) })
  const save = useMutation({
    mutationFn: () => api(`/api/accommodation/${mode}`, { body: { residentId: row.resident.id, bedId: Number(bedId) } }),
    onSuccess: () => { refreshKeys.forEach((k) => qc.invalidateQueries({ queryKey: [k] })); onClose() },
  })
  return (
    <Modal title={`${mode === 'allocate' ? 'Allocate bed' : 'Transfer'} · ${row.resident.fullName}`} onClose={onClose}>
      <div className="space-y-4">
        <Tabs<'PACK' | 'SPREAD'> tabs={[{ id: 'PACK', label: 'Fill rooms first' }, { id: 'SPREAD', label: 'Roomiest first' }]} value={strategy} onChange={setStrategy} />
        {rec.isLoading ? <Spinner /> : rec.error ? <ErrorNote error={rec.error} /> : (
          <ul className="space-y-2" aria-label="Recommended beds">
            {rec.data!.map((s) => (
              <li key={s.bedId}>
                <button type="button" onClick={() => setBedId(String(s.bedId))} className={`w-full rounded-md border px-3 py-2 text-left text-sm transition-colors ${bedId === String(s.bedId) ? 'border-forest bg-forest-soft' : 'border-line hover:bg-forest-soft'}`}>
                  <span className="font-medium">{s.label}</span><span className="block text-xs text-muted">{s.reason}</span>
                </button>
              </li>
            ))}
          </ul>
        )}
        <ErrorNote error={save.error} />
        <div className="flex justify-end gap-2"><Button variant="ghost" onClick={onClose}>Cancel</Button><Button disabled={!bedId} loading={save.isPending} onClick={() => save.mutate()}>{mode === 'allocate' ? 'Allocate' : 'Transfer'}</Button></div>
      </div>
    </Modal>
  )
}
