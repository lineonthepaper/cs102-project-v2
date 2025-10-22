import { useAuth } from './AuthContext'
import { useNavigate } from 'react-router-dom'
import { useState, useEffect } from 'react'
import { supabase } from './supabase'

const API_BASE_URL = (import.meta.env?.VITE_API_BASE_URL ?? '').replace(/\/+$/, '')

function TeachingAssistants() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [loading, setLoading] = useState(true)

  // Teaching Assistants State
  const [users, setUsers] = useState([])
  const [courses, setCourses] = useState([])
  const [taAssignments, setTaAssignments] = useState([])
  const [showTAModal, setShowTAModal] = useState(false)
  const [showAddTAModal, setShowAddTAModal] = useState(false)
  const [selectedUser, setSelectedUser] = useState(null)
  const [selectedSections, setSelectedSections] = useState([])
  const [sectionList, setSectionList] = useState([])
  const [newTAEmail, setNewTAEmail] = useState('')
  const [newTAPassword, setNewTAPassword] = useState('')
  const [taCandidate, setTaCandidate] = useState(null)
  const [taEmailError, setTaEmailError] = useState('')
  const [checkingTAEmail, setCheckingTAEmail] = useState(false)
  const [searchTerm, setSearchTerm] = useState('')
  const [taCourseFilter, setTaCourseFilter] = useState('all')
  const [taType, setTaType] = useState('student') // 'student' or 'instructor'
  const [modalKey, setModalKey] = useState(0)

  useEffect(() => {
    fetchInitialData()
  }, [])

  const fetchInitialData = async () => {
    try {
      await Promise.all([
        fetchUsers(),
        fetchCourses(),
        fetchTaAssignments(),
        fetchSections()
      ])
    } catch (error) {
      console.error('Error fetching initial data:', error)
    } finally {
      setLoading(false)
    }
  }

  // Teaching Assistants Functions
  const fetchUsers = async () => {
    const { data, error } = await supabase
      .from('users')
      .select('*')
      .or('is_student.eq.true,is_instructor.eq.true')

    if (error) throw error
    setUsers(data || [])
  }

  const fetchCourses = async () => {
    const { data, error } = await supabase
      .from('courses')
      .select('*')

    if (error) throw error
    setCourses(data || [])
  }

  const fetchTaAssignments = async () => {
    const { data, error } = await supabase
      .from('ta_assignments')
      .select(`
        *,
        courses(*),
        sections(*),
        users(*)
      `)

    if (error) throw error
    setTaAssignments(data || [])
  }

  const fetchSections = async () => {
    const { data, error } = await supabase
      .from('sections')
      .select(`
        *,
        courses(*)
      `)

    if (error) throw error
    setSectionList(data || [])
  }

  const openTAModal = (user) => {
    setSelectedUser(user)

    // Get current TA assignments for this user
    const userAssignments = taAssignments.filter(ta => ta.user_id === user.id)
    const assignedSectionIds = userAssignments
      .map(ta => Number(ta.section_id))
      .filter((value) => !Number.isNaN(value))
    setSelectedSections(assignedSectionIds)

    setShowTAModal(true)
    setTaCourseFilter('all')
    setSearchTerm('')
  }

  const handleTAEmailBlur = async () => {
    const email = newTAEmail.trim()

    if (!email) {
      setTaCandidate(null)
      setTaEmailError('')
      return
    }

    setCheckingTAEmail(true)
    setTaEmailError('')

    try {
      const { data: existingUser, error } = await supabase
        .from('users')
        .select('*')
        .eq('email', email)
        .maybeSingle()

      if (error && error.code !== 'PGRST116') {
        throw error
      }

      if (!existingUser) {
        setTaCandidate(null)
        if (taType === 'instructor') {
          setTaEmailError('No instructor found with this email address.')
        } else {
          setTaEmailError('No student found with this email address.')
        }
      } else {
        // Validate the user type matches selection
        if (taType === 'instructor' && !existingUser.is_instructor) {
          setTaCandidate(null)
          setTaEmailError('This user is not an instructor. Please select "Student" or use a different email.')
          return
        }
        if (taType === 'student' && !existingUser.is_student) {
          setTaCandidate(null)
          setTaEmailError('This user is not a student. Please select "Instructor" or use a different email.')
          return
        }
        
        setTaCandidate(existingUser)
        setTaEmailError('')
      }
    } catch (error) {
      console.error('Error checking TA email:', error)
      setTaEmailError('Failed to look up user. Please try again.')
      setTaCandidate(null)
    } finally {
      setCheckingTAEmail(false)
    }
  }

  const addTA = async () => {
    const email = newTAEmail.trim()
    const password = newTAPassword.trim()

    if (!email) {
      alert('Please enter an email address')
      return
    }

    if (taType === 'student' && !password) {
      alert('Please provide a password for the student TA account')
      return
    }

    try {
      let existingUser = taCandidate

      if (!existingUser || existingUser.email.toLowerCase() !== email.toLowerCase()) {
        const { data, error: fetchError } = await supabase
          .from('users')
          .select('*')
          .eq('email', email)
          .maybeSingle()

        if (fetchError && fetchError.code !== 'PGRST116') {
          throw fetchError
        }

        existingUser = data || null
        setTaCandidate(existingUser)
      }

      if (!existingUser) {
        alert(`${taType === 'instructor' ? 'Instructor' : 'Student'} with this email not found`)
        return
      }

      if (existingUser.is_ta) {
        alert('This user is already a Teaching Assistant')
        return
      }

      // Validate user type
      if (taType === 'instructor' && !existingUser.is_instructor) {
        alert('This user is not an instructor. Please check the email or select Student.')
        return
      }
      if (taType === 'student' && !existingUser.is_student) {
        alert('This user is not a student. Please check the email or select Instructor.')
        return
      }

      let authId = existingUser.auth_id

      // For students without auth_id, create auth account
      if (taType === 'student' && !authId) {
        const { data: currentSessionData, error: currentSessionError } = await supabase.auth.getSession()
        if (currentSessionError) {
          console.error('Failed to capture current admin session before TA sign up:', currentSessionError)
        }
        const adminSessionTokens = currentSessionData?.session
          ? {
              access_token: currentSessionData.session.access_token,
              refresh_token: currentSessionData.session.refresh_token
            }
          : null

        const { data: signUpData, error: signUpError } = await supabase.auth.signUp({
          email,
          password
        })

        if (signUpError) {
          console.error('Supabase auth signUp error creating TA user:', signUpError)
          throw new Error(signUpError?.message || 'Failed to create authenticated user')
        }

        authId = signUpData?.user?.id

        if (!authId) {
          throw new Error('Auth sign up did not return a user id')
        }

        if (signUpData?.session && adminSessionTokens) {
          const { error: restoreError } = await supabase.auth.setSession(adminSessionTokens)
          if (restoreError) {
            console.warn('Created TA account but failed to restore original session. Please sign in again.', restoreError)
          }
        }
      }

      // For instructors, they should already have auth_id
      if (taType === 'instructor' && !authId) {
        alert('This instructor does not have login credentials. Please contact system administrator.')
        return
      }

      // Update user to be a TA
      const { error: updateError } = await supabase
        .from('users')
        .update({
          is_ta: true,
          enabled: true,
          auth_id: authId || existingUser.auth_id
        })
        .eq('id', existingUser.id)

      if (updateError) throw updateError

      // Refresh data
      await fetchUsers()
      await fetchTaAssignments()

      // Reset and close modal
      closeAddTAModal()
      alert(`Teaching Assistant account created successfully for ${existingUser.first_name} ${existingUser.last_name}.`)

    } catch (error) {
      console.error('Error adding TA:', error)
      alert(`Failed to add Teaching Assistant: ${error.message || 'Unknown error'}`)
    }
  }

  const removeTA = async (user) => {
    if (!confirm(`Are you sure you want to remove ${user.first_name} ${user.last_name} as a Teaching Assistant? They will remain as a student in the system.`)) {
      return
    }

    try {
      // Get the user's auth_id before removing TA status
      const { data: userData, error: userFetchError } = await supabase
        .from('users')
        .select('auth_id')
        .eq('id', user.id)
        .single()

      if (userFetchError) throw userFetchError

      // Remove all TA assignments
      await supabase
        .from('ta_assignments')
        .delete()
        .eq('user_id', user.id)

      // Update user to remove TA status but keep them as student
      const { error } = await supabase
        .from('users')
        .update({
          is_ta: false,
          enabled: false, // Disable login temporarily
          auth_id: null // Remove from authenticated users
        })
        .eq('id', user.id)

      if (error) throw error

      // If user had auth_id, remove them from Supabase Auth
      if (userData?.auth_id) {
        const authId = userData.auth_id
        console.log('Attempting to remove user from Supabase Auth via backend. Auth ID:', authId)

        try {
          const response = await fetch(`${API_BASE_URL}/api/admin/teaching-assistants/${authId}`, {
            method: 'DELETE'
          })

          if (!response.ok) {
            const errorText = await response.text()
            console.warn('Backend failed to remove user from Supabase Auth:', response.status, errorText)
            alert('Warning: Could not remove user from Supabase Auth system. The TA account has been disabled in the app but remains in Supabase Auth.')
          } else {
            console.log('User successfully removed from Supabase Auth via backend endpoint.')
          }
        } catch (apiError) {
          console.error('Backend API call to remove user from Supabase Auth failed:', apiError)
          alert('Warning: Could not contact backend to remove user from Supabase Auth. The account has been disabled locally but remains in Supabase Auth.')
        }
      } else {
        console.log('User does not have auth_id, no auth deletion needed')
      }

      // Refresh data
      await fetchUsers()
      await fetchTaAssignments()

      alert(`${user.first_name} ${user.last_name} has been removed as a Teaching Assistant. Their account has been disabled from login but remains as a student in the system.`)

    } catch (error) {
      console.error('Error removing TA:', error)
      alert('Failed to remove Teaching Assistant: ' + error.message)
    }
  }

  const handleSectionToggle = (sectionId) => {
    const normalizedId = Number(sectionId)
    setSelectedSections(prev => {
      if (prev.includes(normalizedId)) {
        return prev.filter(id => id !== normalizedId)
      } else {
        return [...prev, normalizedId]
      }
    })
  }

  const getFilteredSections = () => {
    return sectionList.filter(section => {
      if (taCourseFilter !== 'all' && Number(section.course_id) !== Number(taCourseFilter)) {
        return false
      }

      if (!searchTerm) return true

      const lower = searchTerm.toLowerCase()
      return (
        section.section_code.toLowerCase().includes(lower) ||
        section.courses?.code?.toLowerCase().includes(lower) ||
        section.courses?.title?.toLowerCase().includes(lower) ||
        section.schedule?.toLowerCase().includes(lower)
      )
    })
  }

  const closeTAModal = () => {
    setShowTAModal(false)
    setSelectedUser(null)
    setSelectedSections([])
    setSearchTerm('')
    setTaCourseFilter('all')
  }

  const openAddTAModal = () => {
    // Clear all fields before opening
    setNewTAEmail('')
    setNewTAPassword('')
    setTaCandidate(null)
    setTaEmailError('')
    setCheckingTAEmail(false)
    setTaType('student')
    setModalKey(prev => prev + 1) // Force modal to remount with fresh state
    setShowAddTAModal(true)
  }

  const closeAddTAModal = () => {
    setShowAddTAModal(false)
    setNewTAEmail('')
    setNewTAPassword('')
    setTaCandidate(null)
    setTaEmailError('')
    setCheckingTAEmail(false)
    setTaCourseFilter('all')
    setTaType('student')
  }

  const saveTAAssignments = async () => {
    try {
      if (!selectedUser) return

      // Delete existing assignments for this user
      await supabase
        .from('ta_assignments')
        .delete()
        .eq('user_id', selectedUser.id)

      // Insert new assignments
      if (selectedSections.length > 0) {
        const assignmentsToInsert = selectedSections.map(sectionId => ({
          user_id: selectedUser.id,
          section_id: sectionId
        }))

        const { error } = await supabase
          .from('ta_assignments')
          .insert(assignmentsToInsert)

        if (error) throw error
      }

      await fetchTaAssignments()
      closeTAModal()
    } catch (error) {
      console.error('Error saving TA assignments:', error)
      alert('Failed to save TA assignments')
    }
  }

  const handleLogout = async () => {
    await logout()
    navigate('/login')
  }

  if (loading) {
    return <div className="loading">Loading teaching assistants...</div>
  }

  return (
    <div className="container">
      <div className="page-header">
        <h1>Teaching Assistants Management</h1>
        <button onClick={() => navigate('/home')} className="btn btn-secondary-small">
          Back to Home
        </button>
      </div>

      <div className="table-container">
        <div className="table-header-actions">
          <button
            onClick={openAddTAModal}
            className="btn btn-primary-small"
          >
            Add Teaching Assistant
          </button>
        </div>
        <table className="data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Name</th>
              <th>Email</th>
              <th>Type</th>
              <th>Assigned Sections</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {users.filter(user => user.is_ta).map(user => {
              const userAssignments = taAssignments.filter(ta => ta.user_id === user.id)
              const assignedSections = userAssignments.map(ta => ta.sections).filter(Boolean)

              return (
                <tr key={user.id}>
                  <td className="ta-id">{user.id}</td>
                  <td>{user.first_name} {user.last_name}</td>
                  <td>{user.email}</td>
                  <td>{user.is_instructor ? 'Instructor' : 'Student'}</td>
                  <td>
                    {assignedSections.length > 0 ? (
                      <span>{assignedSections.length}</span>
                    ) : (
                      <span className="text-muted">0</span>
                    )}
                  </td>
                  <td>
                    <div className="action-buttons">
                      <button
                        onClick={() => openTAModal(user)}
                        className="btn btn-small btn-action"
                      >
                        Manage Sections
                      </button>
                      <button
                        onClick={() => removeTA(user)}
                        className="btn btn-small btn-action"
                      >
                        Remove Teaching Assistant
                      </button>
                    </div>
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>

      {/* TA Assignment Modal */}
      {showTAModal && selectedUser && (
        <div className="modal-overlay" onClick={closeTAModal}>
          <div className="modal-content manage-modal" onClick={(e) => e.stopPropagation()}>
            <div className="manage-modal-header">
              <div className="manage-modal-title">
                <h2>Manage Sections</h2>
                <div className="manage-summary">
                  <div className="name-with-id">
                    {selectedUser.first_name} {selectedUser.last_name}
                    <span className="tag-id">{selectedUser.id}</span>
                  </div>
                  <div className="email">{selectedUser.email}</div>
                </div>
              </div>
              <button onClick={closeTAModal} className="close-button">✕</button>
            </div>

            <div className="manage-modal-body">
              <div className="search-toolbar">
                <div className="search-block">
                  <label className="filter-label" htmlFor="ta-section-search">Search</label>
                  <div className="search-input-wrapper">
                    <input
                      id="ta-section-search"
                      type="text"
                      value={searchTerm}
                      onChange={(e) => setSearchTerm(e.target.value)}
                      className="search-input"
                      placeholder="Search sections..."
                    />
                    <span className="search-icon">⚲</span>
                  </div>
                </div>
                <div className="selection-summary-right">
                  <span className="selection-badge">{selectedSections.length} selected</span>
                </div>
              </div>

              <div className="ta-sections-rows">
                {getFilteredSections().map(section => {
                  const isSelected = selectedSections.includes(section.id)
                  return (
                    <div key={section.id} className={`ta-section-row ${isSelected ? 'selected' : ''}`}>
                      <button
                        className="ta-section-row-btn"
                        onClick={() => handleSectionToggle(section.id)}
                      >
                        <div className="ta-row-checkbox">
                          {isSelected && (
                            <svg width="12" height="12" viewBox="0 0 24 24" fill="currentColor">
                              <path d="M20.285 2l-11.285 11.567-5.286-5.011-3.714 3.716 9 8.728 15-15.285z" />
                            </svg>
                          )}
                        </div>
                        <div className="ta-row-section-code">{section.section_code}</div>
                        <div className="ta-row-title">{section.courses?.title}</div>
                        <div className="ta-row-schedule">{section.schedule}</div>
                      </button>
                    </div>
                  )
                })}
              </div>

              {getFilteredSections().length === 0 && (
                <div className="ta-no-sections">No sections found</div>
              )}
            </div>

            <div className="modal-actions">
              <button onClick={saveTAAssignments} className="btn btn-primary modal-primary">
                Save {selectedSections.length} Assignment{selectedSections.length !== 1 ? 's' : ''}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Add TA Modal */}
      {showAddTAModal && (
        <div className="modal-overlay" onClick={closeAddTAModal}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()} key={modalKey}>
            <div className="modal-header">
              <h2>Add Teaching Assistant</h2>
              <button onClick={closeAddTAModal} className="close-button">
                ✕
              </button>
            </div>

            <div className="modal-body">
              <div className="form-group">
                <label className="form-label">Who are you adding as a TA?</label>
                <div style={{ 
                  display: 'flex', 
                  gap: '0', 
                  marginTop: '0.5rem',
                  borderBottom: '1px solid #e5e7eb'
                }}>
                  <button
                    type="button"
                    onClick={() => {
                      setTaType('student')
                      setNewTAEmail('')
                      setNewTAPassword('')
                      setTaCandidate(null)
                      setTaEmailError('')
                    }}
                    style={{
                      flex: 1,
                      padding: '0.75rem 1rem',
                      border: 'none',
                      borderBottom: taType === 'student' ? '2px solid #111827' : '2px solid transparent',
                      backgroundColor: 'transparent',
                      fontSize: '0.875rem',
                      fontWeight: taType === 'student' ? '600' : '400',
                      cursor: 'pointer',
                      transition: 'all 0.15s ease',
                      color: taType === 'student' ? '#111827' : '#6b7280',
                      outline: 'none',
                      marginBottom: '-1px',
                      borderRadius: '0'
                    }}
                  >
                    Student
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      setTaType('instructor')
                      setNewTAEmail('')
                      setNewTAPassword('')
                      setTaCandidate(null)
                      setTaEmailError('')
                    }}
                    style={{
                      flex: 1,
                      padding: '0.75rem 1rem',
                      border: 'none',
                      borderBottom: taType === 'instructor' ? '2px solid #111827' : '2px solid transparent',
                      backgroundColor: 'transparent',
                      fontSize: '0.875rem',
                      fontWeight: taType === 'instructor' ? '600' : '400',
                      cursor: 'pointer',
                      transition: 'all 0.15s ease',
                      color: taType === 'instructor' ? '#111827' : '#6b7280',
                      outline: 'none',
                      marginBottom: '-1px',
                      borderRadius: '0'
                    }}
                  >
                    Instructor
                  </button>
                </div>
                <small className="form-help" style={{ display: 'block', marginTop: '0.75rem' }}>
                  {taType === 'student' 
                    ? 'Students need a password to create their login account.'
                    : 'Instructors already have login access, no password needed.'}
                </small>
              </div>

              <div className="form-group">
                <label className="form-label">
                  {taType === 'instructor' ? 'Instructor Email' : 'Student Email'}
                </label>
                <input
                  type="email"
                  value={newTAEmail}
                  onChange={(e) => {
                    setNewTAEmail(e.target.value)
                    setTaCandidate(null)
                    setTaEmailError('')
                  }}
                  onBlur={handleTAEmailBlur}
                  className="form-input"
                  placeholder={taType === 'instructor' ? 'instructor@example.com' : 'student@example.com'}
                  autoComplete="off"
                  autoFocus
                />
                {checkingTAEmail && (
                  <small className="form-help">Checking user...</small>
                )}
                {taCandidate && (
                  <small className="form-help">
                    ✓ Found: {taCandidate.first_name} {taCandidate.last_name}
                  </small>
                )}
                {taEmailError && (
                  <small className="form-help" style={{ color: '#dc2626' }}>{taEmailError}</small>
                )}
              </div>

              {taType === 'student' && (
                <div className="form-group">
                  <label className="form-label">Password</label>
                  <input
                    type="password"
                    value={newTAPassword}
                    onChange={(e) => setNewTAPassword(e.target.value)}
                    className="form-input"
                    placeholder="Create a password for this TA account"
                    autoComplete="new-password"
                  />
                  <small className="form-help">
                    This password will be used to create their login account. Share it securely with the TA.
                  </small>
                </div>
              )}

              <div className="form-actions">
                <button onClick={addTA} className="btn btn-primary">
                  Add Teaching Assistant
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

export default TeachingAssistants
