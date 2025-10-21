import { useState, useEffect } from 'react'
import { supabase } from './supabase'
import { useNavigate } from 'react-router-dom'

function Instructors() {
  const navigate = useNavigate()

  const [loading, setLoading] = useState(true)
  const [instructors, setInstructors] = useState([])
  const [courses, setCourses] = useState([])
  const [sections, setSections] = useState([])
  const [instructorAssignments, setInstructorAssignments] = useState([])

  const [showAssignModal, setShowAssignModal] = useState(false)
  const [showAddModal, setShowAddModal] = useState(false)

  const [selectedInstructor, setSelectedInstructor] = useState(null)
  const [selectedSections, setSelectedSections] = useState([])

  const [listSearchTerm, setListSearchTerm] = useState('')
  const [sectionSearchTerm, setSectionSearchTerm] = useState('')
  const [sectionCourseFilter, setSectionCourseFilter] = useState('all')
  const [modalCourseFilter, setModalCourseFilter] = useState('all')

  const [newInstructorEmail, setNewInstructorEmail] = useState('')
  const [newInstructorPassword, setNewInstructorPassword] = useState('')
  const [newInstructorFirstName, setNewInstructorFirstName] = useState('')
  const [newInstructorLastName, setNewInstructorLastName] = useState('')
  const [addingInstructor, setAddingInstructor] = useState(false)

  useEffect(() => {
    fetchInitialData()
  }, [])

  const fetchInitialData = async () => {
    try {
      await Promise.all([fetchInstructors(), fetchCourses(), fetchSections(), fetchInstructorAssignments()])
    } catch (error) {
      console.error('Error loading instructors data:', error)
    } finally {
      setLoading(false)
    }
  }

  const fetchInstructors = async () => {
    const { data, error } = await supabase
      .from('users')
      .select('*')
      .eq('is_instructor', true)

    if (error) throw error
    setInstructors(data || [])
  }

  const fetchSections = async () => {
    const { data, error } = await supabase
      .from('sections')
      .select(`
        *,
        courses(*)
      `)

    if (error) throw error
    setSections(data || [])
  }

  const fetchCourses = async () => {
    const { data, error } = await supabase
      .from('courses')
      .select('*')

    if (error) throw error
    setCourses(data || [])
  }

  const fetchInstructorAssignments = async () => {
    const { data, error } = await supabase
      .from('section_assignments')
      .select(`
        id,
        user_id,
        section_id,
        role,
        is_active,
        sections (
          *,
          courses (*)
        )
      `)
      .eq('role', 'INSTRUCTOR')
      .eq('is_active', true)

    if (error) throw error
    setInstructorAssignments(data || [])
  }

  const openAssignModal = (instructor) => {
    setSelectedInstructor(instructor)
    const currentAssignments = instructorAssignments
      .filter((assignment) => assignment.user_id === instructor.id)
      .map((assignment) => Number(assignment.section_id))

    setSelectedSections(currentAssignments)
    setSectionSearchTerm('')
    setModalCourseFilter('all')
    setShowAssignModal(true)
  }

  const closeAssignModal = () => {
    setShowAssignModal(false)
    setSelectedInstructor(null)
    setSelectedSections([])
    setSectionSearchTerm('')
    setModalCourseFilter('all')
  }

  const closeAddModal = () => {
    setShowAddModal(false)
    setNewInstructorEmail('')
    setNewInstructorPassword('')
    setNewInstructorFirstName('')
    setNewInstructorLastName('')
  }

  const getFilteredSections = () => {
    if (!selectedInstructor) return []

    const takenSections = new Set(
      instructorAssignments
        .filter(
          (assignment) =>
            assignment.user_id !== selectedInstructor.id &&
            assignment.section_id != null
        )
        .map((assignment) => Number(assignment.section_id))
    )

    return sections
      .filter((section) => {
        const sectionId = Number(section.id)
        if (modalCourseFilter !== 'all' && Number(section.course_id) !== Number(modalCourseFilter)) {
          return false
        }
        if (takenSections.has(sectionId) && !selectedSections.includes(sectionId)) {
          return false
        }
        if (!sectionSearchTerm) return true
        const lower = sectionSearchTerm.toLowerCase()
        return (
          section.section_code?.toLowerCase().includes(lower) ||
          section.courses?.code?.toLowerCase().includes(lower) ||
          section.courses?.title?.toLowerCase().includes(lower) ||
          section.schedule?.toLowerCase().includes(lower) ||
          section.location?.toLowerCase().includes(lower)
        )
      })
      .sort((a, b) => a.section_code.localeCompare(b.section_code))
  }

  const toggleSectionSelection = (sectionId) => {
    const normalizedId = Number(sectionId)
    setSelectedSections((prev) => {
      if (prev.includes(normalizedId)) {
        return prev.filter((id) => id !== normalizedId)
      }
      return [...prev, normalizedId]
    })
  }

  const saveAssignments = async () => {
    if (!selectedInstructor) return

    try {
      // Remove existing instructor assignments
      await supabase
        .from('section_assignments')
        .delete()
        .eq('user_id', selectedInstructor.id)
        .eq('role', 'INSTRUCTOR')

      if (selectedSections.length > 0) {
        const payload = selectedSections.map((sectionId) => ({
          user_id: selectedInstructor.id,
          section_id: sectionId,
          role: 'INSTRUCTOR',
          is_active: true
        }))

        const { error } = await supabase
          .from('section_assignments')
          .insert(payload)

        if (error) throw error
      }

      await fetchInstructorAssignments()
      closeAssignModal()
    } catch (error) {
      console.error('Failed to save instructor assignments:', error)
      alert('Failed to save instructor assignments')
    }
  }

  const addInstructor = async () => {
    const email = newInstructorEmail.trim()
    const password = newInstructorPassword.trim()
    const firstName = newInstructorFirstName.trim()
    const lastName = newInstructorLastName.trim()

    if (!email || !password || !firstName || !lastName) {
      alert('Please complete all fields.')
      return
    }

    setAddingInstructor(true)

    try {
      const { data: existingUser, error: fetchError } = await supabase
        .from('users')
        .select('*')
        .eq('email', email)
        .maybeSingle()

      if (fetchError && fetchError.code !== 'PGRST116') {
        throw fetchError
      }

      let authId = existingUser?.auth_id ?? null
      const adminSession = await supabase.auth.getSession()
      const adminTokens = adminSession?.data?.session
        ? {
            access_token: adminSession.data.session.access_token,
            refresh_token: adminSession.data.session.refresh_token
          }
        : null

      if (!authId) {
        const { data: signUpData, error: signUpError } = await supabase.auth.signUp({
          email,
          password
        })

        if (signUpError) {
          throw new Error(signUpError.message || 'Unable to create authentication account')
        }

        authId = signUpData?.user?.id || null

        if (signUpData?.session && adminTokens) {
          const { error: restoreError } = await supabase.auth.setSession(adminTokens)
          if (restoreError) {
            console.warn('Instructor account created but admin session was not restored automatically.', restoreError)
          }
        }
      }

      if (!authId) {
        throw new Error('Failed to obtain authentication id for the instructor.')
      }

      if (existingUser) {
        const { error: updateError } = await supabase
          .from('users')
          .update({
            first_name: firstName,
            last_name: lastName,
            is_instructor: true,
            enabled: true,
            auth_id: authId
          })
          .eq('id', existingUser.id)

        if (updateError) throw updateError
      } else {
        const { error: insertError } = await supabase
          .from('users')
          .insert({
            id: authId,
            email,
            first_name: firstName,
            last_name: lastName,
            is_instructor: true,
            is_student: false,
            is_ta: false,
            enabled: true,
            auth_id: authId,
            created_at: new Date().toISOString()
          })

        if (insertError) throw insertError
      }

      await fetchInstructors()
      await fetchInstructorAssignments()
      closeAddModal()
      alert('Instructor account created successfully.')
    } catch (error) {
      console.error('Error adding instructor:', error)
      alert(`Failed to add instructor: ${error.message || 'Unknown error'}`)
    } finally {
      setAddingInstructor(false)
    }
  }

  const removeInstructor = async (instructor) => {
    if (!window.confirm(`Remove ${instructor.first_name} ${instructor.last_name} as an instructor?`)) {
      return
    }

    try {
      await supabase
        .from('section_assignments')
        .delete()
        .eq('user_id', instructor.id)
        .eq('role', 'INSTRUCTOR')

      await supabase
        .from('ta_assignments')
        .delete()
        .eq('user_id', instructor.id)

      const { error } = await supabase
        .from('users')
        .update({
          is_instructor: false,
          is_ta: false
        })
        .eq('id', instructor.id)

      if (error) throw error

      await fetchInstructors()
      await fetchInstructorAssignments()
    } catch (error) {
      console.error('Failed to remove instructor:', error)
      alert('Failed to remove instructor')
    }
  }

  if (loading) {
    return <div className="loading">Loading instructors...</div>
  }

  const filteredInstructors = instructors.filter((instructor) => {
    if (sectionCourseFilter !== 'all') {
      const courseMatches = instructorAssignments.some((assignment) =>
        assignment.user_id === instructor.id &&
        assignment.sections &&
        Number(assignment.sections.course_id) === Number(sectionCourseFilter)
      )

      if (!courseMatches) {
        return false
      }
    }

    if (!listSearchTerm) return true
    const lower = listSearchTerm.toLowerCase()
    return (
      `${instructor.first_name} ${instructor.last_name}`.toLowerCase().includes(lower) ||
      instructor.email.toLowerCase().includes(lower)
    )
  })

  return (
    <div className="container">
      <div className="page-header">
        <h1>Instructors Management</h1>
        <button onClick={() => navigate('/dashboard')} className="btn btn-secondary-small">
          Back to Dashboard
        </button>
      </div>

      <div className="filters-section">
        <div className="filters-header">
          <h3>Search & Filter</h3>
        </div>
        <div className="search-filter-bar">
          <div className="search-block">
            <label className="filter-label" htmlFor="instructors-search">Search</label>
            <div className="search-input-wrapper">
              <input
                id="instructors-search"
                type="text"
                placeholder="Search by instructor name or email..."
                value={listSearchTerm}
                onChange={(e) => setListSearchTerm(e.target.value)}
                className="search-input"
              />
              <span className="search-icon">⚲</span>
            </div>
          </div>

          <div className="filter-block">
            <label className="filter-label" htmlFor="instructors-course-filter">Course</label>
            <select
              id="instructors-course-filter"
              value={sectionCourseFilter}
              onChange={(e) => setSectionCourseFilter(e.target.value)}
              className="filter-select"
            >
              <option value="all">All Courses</option>
              {courses.map((course) => (
                <option key={course.id} value={course.id}>
                  {course.code} - {course.title}
                </option>
              ))}
            </select>
          </div>
        </div>
      </div>

      <div className="table-container">
        <div className="table-header-actions">
          <button
            onClick={() => setShowAddModal(true)}
            className="btn btn-primary-small"
          >
            Add Instructor
          </button>
        </div>
        <table className="data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Name</th>
              <th>Email</th>
              <th>Assigned Sections</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {filteredInstructors.map((instructor) => {
              const assignments = instructorAssignments.filter(
                (assignment) => assignment.user_id === instructor.id
              )

              return (
                <tr key={instructor.id}>
                  <td className="instructor-id">{instructor.id}</td>
                  <td>{instructor.first_name} {instructor.last_name}</td>
                  <td>{instructor.email}</td>
                  <td>{assignments.length}</td>
                  <td>
                    <div className="action-buttons">
                      <button
                        onClick={() => openAssignModal(instructor)}
                        className="btn btn-small btn-action"
                      >
                        Manage Sections
                      </button>
                      <button
                        onClick={() => removeInstructor(instructor)}
                        className="btn btn-small btn-action"
                      >
                        Remove Instructor
                      </button>
                    </div>
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>

      {showAssignModal && selectedInstructor && (
        <div className="modal-overlay" onClick={closeAssignModal}>
          <div className="modal-content manage-modal" onClick={(e) => e.stopPropagation()}>
            <div className="manage-modal-header">
              <div className="manage-modal-title">
                <h2>Manage Sections</h2>
                <div className="manage-summary">
                  <div className="name-with-id">
                    {selectedInstructor.first_name} {selectedInstructor.last_name}
                    <span className="tag-id">{selectedInstructor.id}</span>
                  </div>
                  <div className="email">{selectedInstructor.email}</div>
                </div>
              </div>
              <button onClick={closeAssignModal} className="close-button">✕</button>
            </div>

            <div className="manage-modal-body">
              <div className="search-toolbar">
                <div className="search-block">
                  <label className="filter-label" htmlFor="instructor-section-search">Search</label>
                  <div className="search-input-wrapper">
                    <input
                      id="instructor-section-search"
                      type="text"
                      value={sectionSearchTerm}
                      onChange={(e) => setSectionSearchTerm(e.target.value)}
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
                {getFilteredSections().map((section) => {
                  const isSelected = selectedSections.includes(section.id)
                  return (
                    <div key={section.id} className={`ta-section-row ${isSelected ? 'selected' : ''}`}>
                      <button
                        className="ta-section-row-btn"
                        onClick={() => toggleSectionSelection(section.id)}
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
                        <div className="ta-row-schedule">
                          {section.schedule || `${section.day_of_week || 'Day ?'} ${section.start_time || ''}-${section.end_time || ''}`}
                        </div>
                      </button>
                    </div>
                  )
                })}
              </div>

              {getFilteredSections().length === 0 && (
                <div className="ta-no-sections">No sections available</div>
              )}
            </div>

            <div className="modal-actions">
              <button onClick={saveAssignments} className="btn btn-primary modal-primary">
                Save {selectedSections.length} Assignment{selectedSections.length === 1 ? '' : 's'}
              </button>
            </div>
          </div>
        </div>
      )}

      {showAddModal && (
        <div className="modal-overlay" onClick={closeAddModal}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>Add Instructor</h2>
              <button onClick={closeAddModal} className="close-button">
                ✕
              </button>
            </div>

            <div className="modal-body">
              <div className="form-group">
                <label className="form-label">First Name</label>
                <input
                  type="text"
                  value={newInstructorFirstName}
                  onChange={(e) => setNewInstructorFirstName(e.target.value)}
                  className="form-input"
                  placeholder="Enter first name"
                />
              </div>

              <div className="form-group">
                <label className="form-label">Last Name</label>
                <input
                  type="text"
                  value={newInstructorLastName}
                  onChange={(e) => setNewInstructorLastName(e.target.value)}
                  className="form-input"
                  placeholder="Enter last name"
                />
              </div>

              <div className="form-group">
                <label className="form-label">Email Address</label>
                <input
                  type="email"
                  value={newInstructorEmail}
                  onChange={(e) => setNewInstructorEmail(e.target.value)}
                  className="form-input"
                  placeholder="Enter instructor email address"
                />
              </div>

              <div className="form-group">
                <label className="form-label">Password</label>
                <input
                  type="password"
                  value={newInstructorPassword}
                  onChange={(e) => setNewInstructorPassword(e.target.value)}
                  className="form-input"
                  placeholder="Provide a temporary password"
                />
                <small className="form-help">
                  Instructors can sign in immediately using this password. Share it securely and encourage them to change it after the first login.
                </small>
              </div>

              <div className="form-actions">
                <button onClick={addInstructor} className="btn btn-primary" disabled={addingInstructor}>
                  {addingInstructor ? 'Adding...' : 'Add Instructor'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

export default Instructors
