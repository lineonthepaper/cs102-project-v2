import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider, useAuth } from './AuthContext'
import { useRole } from './role'
import Login from './Login'
import Home from './Home'
import Students from './Students'
import AddStudent from './AddStudent'
import Classes from './Classes'
import TeachingAssistants from './TeachingAssistants'
import Instructors from './Instructors'
import Attendance from './Attendance'
import SectionAttendance from './SectionAttendance'
import Dashboard from './Dashboard'
import './styles.css'
import Register from './Register'


function ProtectedRoute({
  children,
  allowedRoles,
}: {
  children: React.ReactNode
  allowedRoles?: string[]
}) {
  const { user, loading } = useAuth()
  const role = useRole()

  if (loading) return <div>Loading...</div>

  if (!user) return <Navigate to="/login" />

  if (allowedRoles && !allowedRoles.includes(role)) {
    return <Navigate to="/dashboard" />
  }

    return children
}


function PublicRoute({ children }: { children: React.ReactNode }) {
    const { user, loading } = useAuth()

  if (loading) return <div>Loading...</div>
  if (user) return <Navigate to="/home" />

    return children
}

function AppRoutes() {
  return (
    <Routes>
      <Route
        path="/login"
        element={
          <PublicRoute>
            <Login />
          </PublicRoute>
        }
      />

      <Route
        path="/home"
        element={
          <ProtectedRoute allowedRoles={['admin']}>
            <Home />
          </ProtectedRoute>
        }
      />
      <Route
        path="/students"
        element={
          <ProtectedRoute allowedRoles={['admin']}>
            <Students />
          </ProtectedRoute>
        }
      />
      <Route
        path="/student"
        element={
          <ProtectedRoute allowedRoles={['admin']}>
            <AddStudent />
          </ProtectedRoute>
        }
      />
      <Route
        path="/classes"
        element={
          <ProtectedRoute allowedRoles={['admin']}>
            <Classes />
          </ProtectedRoute>
        }
      />
      <Route
        path="/teaching-assistants"
        element={
          <ProtectedRoute allowedRoles={['admin']}>
            <TeachingAssistants />
          </ProtectedRoute>
        }
      />
      <Route
        path="/instructors"
        element={
          <ProtectedRoute allowedRoles={['admin']}>
            <Instructors />
          </ProtectedRoute>
        }
      />
      <Route
        path="/attendance/all"
        element={
          <ProtectedRoute allowedRoles={['admin']}>
            <Attendance />
          </ProtectedRoute>
        }
      />

      <Route
        path="/attendance/:sectionId/:sectionCode"
        element={
          <ProtectedRoute allowedRoles={['instructor', 'teaching assistant']}>
            <SectionAttendance />
          </ProtectedRoute>
        }
      />
      <Route
        path="/dashboard"
        element={
          <ProtectedRoute allowedRoles={['instructor', 'teaching assistant', 'admin']}>
            <Dashboard />
          </ProtectedRoute>
        }
      />

      <Route path="/" element={<Navigate to="/login" />} />
    </Routes>
  )
}

function App() {
    return (
        <AuthProvider>
            <Router>
                <AppRoutes />
            </Router>
        </AuthProvider>
    )
}

export default App
