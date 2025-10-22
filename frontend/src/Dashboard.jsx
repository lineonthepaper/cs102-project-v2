import { useAuth } from './AuthContext'
import { useNavigate } from 'react-router-dom'
import { useState, useEffect } from 'react'
import { supabase } from './supabase'

function Dashboard() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [userRole, setUserRole] = useState('Loading...')

  useEffect(() => {
    const fetchUserRole = async () => {
      if (user?.email) {
        try {
          const { data, error } = await supabase
            .from('users')
            .select('is_student, is_instructor, is_ta')
            .eq('email', user.email)
            .maybeSingle()

          if (data && !error) {
            // Priority: Instructor > Teaching Assistant > Student
            if (data.is_instructor) setUserRole('Instructor')
            else if (data.is_ta) setUserRole('Teaching Assistant')
            else if (data.is_student) setUserRole('Student')
            else setUserRole('User') // No role assigned
          } else if (!data) {
            // User not in database - treat as Admin for authenticated system users
            setUserRole('Admin')
          } else {
            setUserRole('User')
          }
        } catch (err) {
          console.error('Error fetching user role:', err)
          setUserRole('User')
        }
      }
    }

    fetchUserRole()
  }, [user?.email])

  const handleLogout = async () => {
    await logout()
    navigate('/login')
  }

  return (
    <div className="container">
      <div className="header">
        <div className="header-content">
          <div className="header-title">
            <h1>Smart Attendance System</h1>
            <span className="role-text">{userRole}</span>
          </div>
          <div className="header-actions">
            <button onClick={handleLogout} className="btn btn-secondary-small">
              Logout
            </button>
          </div>
        </div>
      </div>

      <div className="welcome-section">
        <h2>Dashboard</h2>
        <p>Manage your attendance system and track student participation efficiently.</p>

        </div>

      <div className="dashboard-grid">
        <div className="card">
          <h3>Students</h3>
          <p>Manage student records, enrollments, and face recognition data</p>
          <button onClick={() => navigate('/students')} className="btn btn-secondary">
            Manage Students
          </button>
        </div>

        <div className="card">
          <h3>Teaching Assistants</h3>
          <p>Manage Teaching Assistant status and section assignments</p>
          <button onClick={() => navigate('/teaching-assistants')} className="btn btn-secondary">
            Manage Teaching Assistants
          </button>
        </div>

        <div className="card">
          <h3>Instructors</h3>
          <p>Manage instructor accounts and teaching assignments</p>
          <button onClick={() => navigate('/instructors')} className="btn btn-secondary">
            Manage Instructors
          </button>
        </div>

        <div className="card">
          <h3>Classes</h3>
          <p>Create and manage course sections and schedules</p>
          <button onClick={() => navigate('/classes')} className="btn btn-secondary">
            Manage Classes
          </button>
        </div>

        <div className="card">
          <h3>Attendance</h3>
          <p>Start new attendance sessions and track participation</p>
          <button className="btn btn-secondary">Start Session</button>
        </div>

        <div className="card">
          <h3>Reports</h3>
          <p>View attendance analytics and export reports</p>
          <button className="btn btn-secondary">View Reports</button>
        </div>

        <div className="card">
          <h3>Settings</h3>
          <p>Configure system settings and preferences</p>
          <button className="btn btn-secondary">Settings</button>
        </div>
      </div>
    </div>
  )
}

export default Dashboard
