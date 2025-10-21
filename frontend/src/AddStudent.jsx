import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { supabase } from './supabase'

function AddStudent() {
  const navigate = useNavigate()
  const [formData, setFormData] = useState({
    email: '',
    password: '',
    firstName: '',
    lastName: '',
    isTA: false
  })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  const handleSubmit = async (e) => {
    e.preventDefault()
    setLoading(true)
    setError('')
    setSuccess('')

    try {
      // Hash password
      const { data: authData, error: authError } = await supabase.auth.signUp({
        email: formData.email,
        password: formData.password,
        options: {
          data: {
            first_name: formData.firstName,
            last_name: formData.lastName,
            is_student: true,
            is_instructor: false,
            is_ta: formData.isTA
          }
        }
      })

      if (authError) throw authError

      // Create user record
      const { error: userError } = await supabase
        .from('users')
        .upsert({
          email: formData.email,
          first_name: formData.firstName,
          last_name: formData.lastName,
          is_student: true,
          is_instructor: false,
          is_ta: formData.isTA
        })
        .eq('email', formData.email)

      if (userError) throw userError

      setSuccess('Student added successfully!')
      setTimeout(() => navigate('/students'), 2000)
    } catch (error) {
      console.error('Error adding student:', error)
      setError(error.message || 'Failed to add student')
    } finally {
      setLoading(false)
    }
  }

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target
    setFormData(prev => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : (name === 'userType' ? value === 'ta' : value)
    }))
  }

  return (
    <div className="add-student-container">
      <h1 className="page-title">Add New Student</h1>

      <div className="card">
        <h3>Student Information</h3>

        <form onSubmit={handleSubmit}>
          {error && (
            <div className="error">
              {error}
            </div>
          )}

          {success && (
            <div className="success">
              {success}
            </div>
          )}

          <div className="form-group">
            <label className="form-label">Email Address</label>
            <input
              type="email"
              name="email"
              value={formData.email}
              onChange={handleChange}
              className="form-input"
              placeholder="student@smu.edu.sg"
              required
            />
          </div>

          <div className="form-group">
            <label className="form-label">Password</label>
            <input
              type="password"
              name="password"
              value={formData.password}
              onChange={handleChange}
              className="form-input"
              placeholder="Enter password"
              required
            />
          </div>

          <div className="form-group">
            <label className="form-label">First Name</label>
            <input
              type="text"
              name="firstName"
              value={formData.firstName}
              onChange={handleChange}
              className="form-input"
              placeholder="John"
              required
            />
          </div>

          <div className="form-group">
            <label className="form-label">Last Name</label>
            <input
              type="text"
              name="lastName"
              value={formData.lastName}
              onChange={handleChange}
              className="form-input"
              placeholder="Doe"
              required
            />
          </div>

          <div className="form-group">
            <label className="form-label">User Type</label>
            <div className="radio-group">
              <label className="radio-option">
                <input
                  type="radio"
                  name="userType"
                  value="student"
                  checked={!formData.isTA}
                  onChange={handleChange}
                />
                <span>Student (No login access)</span>
              </label>
              <label className="radio-option">
                <input
                  type="radio"
                  name="userType"
                  value="ta"
                  checked={formData.isTA}
                  onChange={handleChange}
                />
                <span>Teaching Assistant (Has login access)</span>
              </label>
            </div>
            <small className="form-help">
              Note: Only Teaching Assistants can log in to the system.
            </small>
          </div>

          <div className="form-actions">
            <button type="submit" className="btn btn-primary" disabled={loading}>
              {loading ? 'Adding...' : 'Add Student'}
            </button>
            <button
              type="button"
              onClick={() => navigate('/students')}
              className="btn btn-secondary"
              disabled={loading}
            >
              Cancel
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

export default AddStudent