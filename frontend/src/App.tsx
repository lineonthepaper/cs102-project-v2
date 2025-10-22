import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider, useAuth } from './AuthContext'
import Login from './Login'
import Home from './Home'
import Students from './Students'
import AddStudent from './AddStudent'
import Classes from './Classes'
import TeachingAssistants from './TeachingAssistants'
import Instructors from './Instructors'
import Attendance from './Attendance'
import './styles.css'

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { user, loading } = useAuth()

  if (loading) {
    return <div>Loading...</div>
  }

  if (!user) {
    return <Navigate to="/login" />
  }

  return children
}

function PublicRoute({ children }: { children: React.ReactNode }) {
  const { user, loading } = useAuth()

  if (loading) {
    return <div>Loading...</div>
  }

  if (user) {
    return <Navigate to="/home" />
  }

  return children
}

function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={
        <PublicRoute>
          <Login />
        </PublicRoute>
      } />
      <Route path="/home" element={
        <ProtectedRoute>
          <Home />
        </ProtectedRoute>
      } />
      <Route path="/students" element={
        <ProtectedRoute>
          <Students />
        </ProtectedRoute>
      } />
      <Route path="/student" element={
        <ProtectedRoute>
          <AddStudent />
        </ProtectedRoute>
      } />
      <Route path="/classes" element={
        <ProtectedRoute>
          <Classes />
        </ProtectedRoute>
      } />
      <Route path="/teaching-assistants" element={
        <ProtectedRoute>
          <TeachingAssistants />
        </ProtectedRoute>
      } />
      <Route path="/instructors" element={
        <ProtectedRoute>
          <Instructors />
        </ProtectedRoute>
      } />
      <Route path="/attendance" element={
        <ProtectedRoute>
          <Attendance />
        </ProtectedRoute>
      } />
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
