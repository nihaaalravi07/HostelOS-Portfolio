import { Navigate, Route, Routes } from 'react-router-dom'
import { homeFor, useAuth } from './auth'
import { Layout } from './components/Layout'
import { Spinner } from './components/ui'
import type { Role } from './lib/types'
import AdminDashboard from './pages/admin/AdminDashboard'
import FinanceReport from './pages/admin/FinanceReport'
import MessAnalytics from './pages/admin/MessAnalytics'
import Requests from './pages/admin/Requests'
import ResidentsRooms from './pages/admin/ResidentsRooms'
import WardenDashboard from './pages/admin/WardenDashboard'
import Login from './pages/Login'
import Account from './pages/student/Account'
import Dashboard from './pages/student/Dashboard'
import Meals from './pages/student/Meals'
import RoomServices from './pages/student/RoomServices'
import Feedback from './pages/staff/Feedback'
import HousekeepingJobs from './pages/staff/HousekeepingJobs'
import MaintenanceJobs from './pages/staff/MaintenanceJobs'
import MenuManager from './pages/staff/MenuManager'
import MessBoard from './pages/staff/MessBoard'

function Guard({ roles, children }: { roles: Role[]; children: React.ReactNode }) {
  const { user } = useAuth()
  if (!user) return <Navigate to="/login" replace />
  if (!roles.includes(user.role)) return <Navigate to={homeFor(user.role)} replace />
  return <>{children}</>
}

export default function App() {
  const { user, loading } = useAuth()
  if (loading) return <Spinner label="Restoring your session" />
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route element={user ? <Layout /> : <Navigate to="/login" replace />}>
        <Route index element={user && user.role !== 'STUDENT' ? <Navigate to={homeFor(user.role)} replace /> : <Guard roles={['STUDENT']}><Dashboard /></Guard>} />
        <Route path="meals" element={<Guard roles={['STUDENT']}><Meals /></Guard>} />
        <Route path="account" element={<Guard roles={['STUDENT']}><Account /></Guard>} />
        <Route path="services" element={<Guard roles={['STUDENT']}><RoomServices /></Guard>} />

        <Route path="mess" element={<Guard roles={['MESS_STAFF', 'ADMIN']}><MessBoard /></Guard>} />
        <Route path="mess/menu" element={<Guard roles={['MESS_STAFF', 'ADMIN']}><MenuManager /></Guard>} />
        <Route path="mess/feedback" element={<Guard roles={['MESS_STAFF', 'ADMIN']}><Feedback /></Guard>} />

        <Route path="admin" element={<Guard roles={['ADMIN']}><AdminDashboard /></Guard>} />
        <Route path="admin/mess" element={<Guard roles={['ADMIN']}><MessAnalytics /></Guard>} />
        <Route path="admin/finance" element={<Guard roles={['ADMIN']}><FinanceReport /></Guard>} />
        <Route path="admin/residents" element={<Guard roles={['ADMIN', 'WARDEN']}><ResidentsRooms /></Guard>} />
        <Route path="admin/requests" element={<Guard roles={['ADMIN', 'WARDEN']}><Requests /></Guard>} />
        <Route path="warden" element={<Guard roles={['WARDEN']}><WardenDashboard /></Guard>} />

        <Route path="housekeeping" element={<Guard roles={['HOUSEKEEPING_STAFF']}><HousekeepingJobs /></Guard>} />
        <Route path="maintenance" element={<Guard roles={['MAINTENANCE_STAFF']}><MaintenanceJobs /></Guard>} />
      </Route>
      <Route path="*" element={<Navigate to={user ? homeFor(user.role) : '/login'} replace />} />
    </Routes>
  )
}
