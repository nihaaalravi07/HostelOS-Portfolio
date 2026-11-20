import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { Bar, Card, CardHeader, ErrorNote, PageHeader, Spinner, Stat } from '../../components/ui'
import { api } from '../../lib/api'
import type { WardenDashboard as Data } from '../../lib/types'

export default function WardenDashboard() {
  const q = useQuery({ queryKey: ['dashboard', 'warden'], queryFn: () => api<Data>('/api/dashboard/warden') })
  if (q.isLoading) return <Spinner />
  if (q.error || !q.data) return <ErrorNote error={q.error} />
  const d = q.data
  return (
    <div className="space-y-6">
      <PageHeader title="Warden overview" subtitle="Occupancy and open hostel services." />
      <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
        <Stat label="Residents" value={d.totalResidents} />
        <Stat label="Occupied beds" value={`${d.occupancy.occupiedBeds} / ${d.occupancy.totalBeds}`} />
        <Stat label="Pending cleaning" value={d.pendingCleaning} />
        <Stat label="Open complaints" value={d.openComplaints} tone={d.openComplaints ? 'warn' : 'good'} />
      </div>
      <Card>
        <CardHeader title="Occupancy" />
        <div className="space-y-3 p-5"><Bar value={d.occupancy.occupiedBeds} max={d.occupancy.totalBeds} />
          <div className="flex gap-5 text-sm"><Link className="font-medium text-forest underline" to="/admin/residents">Residents & rooms</Link><Link className="font-medium text-forest underline" to="/admin/requests">Service requests</Link></div>
        </div>
      </Card>
    </div>
  )
}
