import { useQuery } from '@tanstack/react-query'
import { BedDouble, CalendarClock, Megaphone, Receipt, Sparkles, UtensilsCrossed, Wrench } from 'lucide-react'
import { Link } from 'react-router-dom'
import { MealCard } from '../../components/MealCard'
import { Badge, Card, CardHeader, Empty, ErrorNote, Spinner, Stat, statusTone } from '../../components/ui'
import { api } from '../../lib/api'
import { fmtDay, fmtShortDate, money, titleCase } from '../../lib/format'
import type { StudentDashboard } from '../../lib/types'

export default function Dashboard() {
  const q = useQuery({ queryKey: ['dashboard', 'student'], queryFn: () => api<StudentDashboard>('/api/dashboard/student') })
  if (q.isLoading) return <Spinner />
  if (q.error || !q.data) return <ErrorNote error={q.error} />
  const d = q.data
  const hour = new Date().getHours()
  const greeting = hour < 12 ? 'Good morning' : hour < 17 ? 'Good afternoon' : 'Good evening'
  const a = d.accommodation

  return (
    <div className="space-y-8">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="text-sm text-muted">{d.profile.course} · Year {d.profile.yearOfStudy} · {d.profile.enrollmentNo}</p>
          <h1 className="mt-1 text-[34px] font-medium leading-tight text-forest-dark">{greeting}, {d.profile.fullName.split(' ')[0]}.</h1>
        </div>
        <div className="flex flex-wrap gap-2">
          <QuickAction to="/meals" icon={UtensilsCrossed} label="Book a meal" primary />
          <QuickAction to="/services" icon={Sparkles} label="Schedule cleaning" />
          <QuickAction to="/services?tab=report" icon={Wrench} label="Report a problem" />
          <QuickAction to="/account" icon={Receipt} label="Account statement" />
        </div>
      </div>

      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <Stat label="Mess spend this month" value={money(d.messSpendThisMonth)} hint="Only meals you actually ate" />
        <Stat label="Meals eaten this month" value={d.mealsConsumedThisMonth} hint={`${d.upcomingBookedMeals} booked ahead`} />
        <Stat label="Outstanding balance" value={money(d.account.outstanding)} tone={d.account.outstanding > 0 ? 'warn' : 'good'} hint={<Link className="underline" to="/account">View statement</Link>} />
        <Stat label="Open complaints" value={d.openComplaints} hint={<Link className="underline" to="/services?tab=track">Track requests</Link>} />
      </div>

      <div className="grid gap-5 lg:grid-cols-3">
        <Card>
          <CardHeader title="Your room" />
          <div className="space-y-3 p-5">
            {a ? (
              <>
                <div className="flex items-center gap-3">
                  <div className="rounded-lg bg-forest-soft p-2.5 text-forest"><BedDouble className="h-5 w-5" /></div>
                  <div>
                    <p className="font-semibold">{a.hostelName}</p>
                    <p className="text-sm text-muted">Room {a.roomNumber} · Floor {a.floorNo} · Bed {a.bedNumber} of {a.roomCapacity}</p>
                  </div>
                </div>
                <p className="text-[13px] text-muted">Roommate{a.roommates.length === 1 ? '' : 's'}: {a.roommates.join(', ') || 'None yet'}</p>
                <p className="text-[13px] text-muted">Allocated since {fmtShortDate(a.since)}</p>
              </>
            ) : <Empty title="No bed allocated yet" hint="The warden will allocate your room." />}
          </div>
        </Card>

        <Card>
          <CardHeader title="Upcoming cleaning" />
          <div className="p-5">
            {d.nextCleaning ? (
              <div className="flex items-start gap-3">
                <div className="rounded-lg bg-forest-soft p-2.5 text-forest"><CalendarClock className="h-5 w-5" /></div>
                <div>
                  <p className="font-semibold">{fmtDay(d.nextCleaning.date)}</p>
                  <p className="text-sm text-muted">{d.nextCleaning.slotLabel}</p>
                  <div className="mt-2"><Badge tone={statusTone(d.nextCleaning.status)}>{titleCase(d.nextCleaning.status)}</Badge></div>
                </div>
              </div>
            ) : <Empty title="Nothing scheduled" hint="Pick a slot that suits you." />}
            <Link to="/services" className="mt-4 inline-block text-sm font-medium text-forest underline">Schedule room cleaning</Link>
          </div>
        </Card>

        <Card>
          <CardHeader title="Announcements" />
          <ul className="divide-y divide-line">
            {d.announcements.slice(0, 3).map((n) => (
              <li key={n.id} className="flex gap-3 px-5 py-3">
                <Megaphone className="mt-0.5 h-4 w-4 shrink-0 text-gold" />
                <div>
                  <p className="text-sm font-medium">{n.title}</p>
                  <p className="mt-0.5 line-clamp-2 text-[13px] text-muted">{n.body}</p>
                </div>
              </li>
            ))}
          </ul>
        </Card>
      </div>

      <section>
        <div className="mb-3 flex items-end justify-between">
          <h2 className="text-2xl font-medium text-forest-dark">Today's mess menu</h2>
          <Link to="/meals" className="text-sm font-medium text-forest underline">See the week ahead</Link>
        </div>
        {d.todaysMenu.length === 0 ? <Card><Empty title="No menu published for today" /></Card> : (
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">{d.todaysMenu.map((m) => <MealCard key={m.id} meal={m} />)}</div>
        )}
      </section>
    </div>
  )
}

function QuickAction({ to, icon: Icon, label, primary }: { to: string; icon: typeof Receipt; label: string; primary?: boolean }) {
  return (
    <Link to={to} className={`inline-flex items-center gap-2 rounded-md px-3.5 py-2 text-sm font-medium transition-colors ${primary ? 'bg-forest text-ivory hover:bg-forest-dark' : 'border border-line bg-paper hover:bg-forest-soft'}`}>
      <Icon className="h-4 w-4" /> {label}
    </Link>
  )
}
