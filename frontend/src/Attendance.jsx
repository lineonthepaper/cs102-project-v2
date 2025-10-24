import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { supabase } from './supabase'

const API_BASE_URL = 'http://localhost:8080'

function Attendance() {
  const navigate = useNavigate()
  const [loading, setLoading] = useState(true)

  // Sessions State
  const [sectionList, setSectionList] = useState([])
  const [sessionList, setSessionList] = useState([])
  const [filteredSessions, setFilteredSessions] = useState([])
  const [showSessionModal, setShowSessionModal] = useState(false)
  const [editingSession, setEditingSession] = useState(null)
  const [sessionForm, setSessionForm] = useState({
    section_id: '',
    session_date: '',
    scheduled_start_time: '',
    scheduled_end_time: '',
    status: 'SCHEDULED',
    notes: ''
  })

  // Attendance Marking State
  const [showMarkingModal, setShowMarkingModal] = useState(false)
  const [selectedSession, setSelectedSession] = useState(null)
  const [students, setStudents] = useState([])
  const [filteredStudents, setFilteredStudents] = useState([])
  const [attendanceRecords, setAttendanceRecords] = useState({})

  // Search and Filter State (Sessions)
  const [searchTerm, setSearchTerm] = useState('')
  const [sortBy, setSortBy] = useState('default')
  const [sortOrder, setSortOrder] = useState('asc')
  const [sectionFilter, setSectionFilter] = useState('all')
  const [yearFilter, setYearFilter] = useState('all')
  const [semesterFilter, setSemesterFilter] = useState('all')
  const [statusFilterSessions, setStatusFilterSessions] = useState('all')
  
  // Year and semester options
  const yearOptions = [2025, 2026, 2027, 2028, 2029, 2030]
  const semesterOptions = [1, 2]
  
  // Search and Filter State (Marking Modal)
  const [markingSearchTerm, setMarkingSearchTerm] = useState('')
  const [statusFilterMarking, setStatusFilterMarking] = useState('all')

  useEffect(() => {
    fetchInitialData()
  }, [])

  useEffect(() => {
    filterAndSortSessions()
  }, [sessionList, searchTerm, sortBy, sortOrder, sectionFilter, yearFilter, semesterFilter, statusFilterSessions])

  useEffect(() => {
    filterStudents()
  }, [students, markingSearchTerm, statusFilterMarking, attendanceRecords])

  const fetchInitialData = async () => {
    try {
      await Promise.all([
        fetchSections(),
        fetchSessions()
      ])
    } catch (error) {
      console.error('Error fetching initial data:', error)
    } finally {
      setLoading(false)
    }
  }

  const filterAndSortSessions = () => {
    let filtered = [...sessionList]

    // Apply search filter
    if (searchTerm) {
      filtered = filtered.filter(session =>
        session.sections?.section_code?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        session.sections?.courses?.code?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        session.sections?.courses?.title?.toLowerCase().includes(searchTerm.toLowerCase())
      )
    }

    // Apply section filter
    if (sectionFilter !== 'all') {
      filtered = filtered.filter(s => s.section_id === parseInt(sectionFilter))
    }

    // Apply year filter
    if (yearFilter !== 'all') {
      filtered = filtered.filter(s => s.sections?.year === parseInt(yearFilter))
    }

    // Apply semester filter
    if (semesterFilter !== 'all') {
      filtered = filtered.filter(s => s.sections?.semester === parseInt(semesterFilter))
    }

    // Apply status filter
    if (statusFilterSessions !== 'all') {
      filtered = filtered.filter(s => s.status === statusFilterSessions)
    }

    // Apply sorting
    if (sortBy !== 'default') {
      filtered.sort((a, b) => {
        let aVal = a[sortBy]
        let bVal = b[sortBy]
        if (typeof aVal === 'string') {
          aVal = aVal.toLowerCase()
          bVal = bVal.toLowerCase()
        }
        if (sortOrder === 'asc') {
          return aVal > bVal ? 1 : -1
        } else {
          return aVal < bVal ? 1 : -1
        }
      })
    }

    setFilteredSessions(filtered)
  }

  const filterStudents = () => {
    let filtered = [...students]

    // Apply search filter
    if (markingSearchTerm) {
      filtered = filtered.filter(student =>
        student.id?.toLowerCase().includes(markingSearchTerm.toLowerCase()) ||
        student.first_name?.toLowerCase().includes(markingSearchTerm.toLowerCase()) ||
        student.last_name?.toLowerCase().includes(markingSearchTerm.toLowerCase())
      )
    }

    // Apply status filter
    if (statusFilterMarking !== 'all') {
      filtered = filtered.filter(student => {
        const record = attendanceRecords[student.id]
        if (statusFilterMarking === 'unmarked') {
          return !record
        }
        return record && record.status === statusFilterMarking
      })
    }

    setFilteredStudents(filtered)
  }

  const getSortIcon = (column) => {
    if (sortBy !== column) return '↕'
    if (sortOrder === 'default') return '—'
    return sortOrder === 'asc' ? '↑' : '↓'
  }

  const handleSort = (column) => {
    if (sortBy === column) {
      if (sortOrder === 'asc') {
        setSortOrder('desc')
      } else if (sortOrder === 'desc') {
        setSortBy('default')
        setSortOrder('asc')
      } else {
        setSortBy(column)
        setSortOrder('asc')
      }
    } else {
      setSortBy(column)
      setSortOrder('asc')
    }
  }

  const fetchSections = async () => {
    const response = await fetch(`${API_BASE_URL}/api/sections`)
    if (!response.ok) throw new Error('Failed to fetch sections')
    
    const data = await response.json()
    
    // Transform backend data to match frontend expectations
    const transformedData = data.map(section => ({
      ...section,
      courses: section.course ? {
        code: section.course.code,
        title: section.course.title
      } : null
    }))
    
    // Sort by year and semester descending
    transformedData.sort((a, b) => {
      if (b.year !== a.year) return b.year - a.year
      return b.semester - a.semester
    })
    
    setSectionList(transformedData)
  }

  const fetchSessions = async () => {
    const response = await fetch(`${API_BASE_URL}/api/attendance/sessions`)
    if (!response.ok) throw new Error('Failed to fetch sessions')
    
    const data = await response.json()
    
    // Transform backend data to match frontend expectations
    const transformedData = data.map(session => ({
      id: session.id,
      section_id: session.sectionId,
      session_date: session.sessionDate,
      scheduled_start_time: session.scheduledStartTime,
      scheduled_end_time: session.scheduledEndTime,
      status: session.status,
      notes: session.notes,
      sections: session.section ? {
        section_code: session.section.sectionCode,
        year: session.section.year,
        semester: session.section.semester,
        day_of_week: session.section.dayOfWeek,
        start_time: session.section.startTime,
        end_time: session.section.endTime,
        location: session.section.location,
        courses: session.section.course ? {
          code: session.section.course.code,
          title: session.section.course.title
        } : null
      } : null
    }))
    
    // Sort by session date descending
    transformedData.sort((a, b) => new Date(b.session_date) - new Date(a.session_date))
    
    setSessionList(transformedData)
    setFilteredSessions(transformedData)
  }

  const saveSession = async () => {
    try {
      // Validate session date matches section's academic year
      if (sessionForm.section_id && sessionForm.session_date) {
        const selectedSection = sectionList.find(s => s.id === parseInt(sessionForm.section_id))
        if (selectedSection) {
          const sessionYear = new Date(sessionForm.session_date).getFullYear()
          const sectionYear = selectedSection.year
          
          // Allow sessions within 1 year of the section's academic year
          // e.g., Section 2025 can have sessions from 2024-2026
          if (Math.abs(sessionYear - sectionYear) > 1) {
            alert(`Session date year (${sessionYear}) does not match section's academic year (${sectionYear}).\n\nPlease ensure the session date is within the correct academic year.`)
            return
          }
        }
      }

      // Transform frontend form to backend DTO format
      const requestBody = {
        sectionId: parseInt(sessionForm.section_id),
        sessionDate: sessionForm.session_date,
        scheduledStartTime: sessionForm.scheduled_start_time,
        scheduledEndTime: sessionForm.scheduled_end_time,
        status: sessionForm.status,
        notes: sessionForm.notes || null
      }

      let response
      if (editingSession) {
        response = await fetch(`${API_BASE_URL}/api/attendance/sessions/${editingSession.id}`, {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(requestBody)
        })
      } else {
        response = await fetch(`${API_BASE_URL}/api/attendance/sessions`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(requestBody)
        })
      }

      if (!response.ok) {
        const errorData = await response.json()
        throw new Error(errorData.error || 'Failed to save session')
      }

      await fetchSessions()
      setShowSessionModal(false)
      setEditingSession(null)
      resetSessionForm()
    } catch (error) {
      console.error('Error saving session:', error)
      alert('Failed to save session: ' + error.message)
    }
  }

  const editSession = (session) => {
    setEditingSession(session)
    setSessionForm({
      section_id: session.section_id,
      session_date: session.session_date,
      scheduled_start_time: session.scheduled_start_time,
      scheduled_end_time: session.scheduled_end_time,
      status: session.status,
      notes: session.notes || ''
    })
    setShowSessionModal(true)
  }

  const resetSessionForm = () => {
    setSessionForm({
      section_id: '',
      session_date: '',
      scheduled_start_time: '',
      scheduled_end_time: '',
      status: 'SCHEDULED',
      notes: ''
    })
  }

  const openMarkAttendance = async (session) => {
    setSelectedSession(session)
    setMarkingSearchTerm('')
    setStatusFilterMarking('all')
    await fetchStudentsAndRecords(session.id, session.section_id)
    setShowMarkingModal(true)
  }

  const closeMarkingModal = () => {
    setShowMarkingModal(false)
    setSelectedSession(null)
    setStudents([])
    setFilteredStudents([])
    setAttendanceRecords({})
    setMarkingSearchTerm('')
    setStatusFilterMarking('all')
  }

  const fetchStudentsAndRecords = async (sessionId, sectionId) => {
    try {
      // Fetch enrolled students via backend API
      const studentsResponse = await fetch(`${API_BASE_URL}/api/students`)
      if (!studentsResponse.ok) throw new Error('Failed to fetch students')
      
      const allStudents = await studentsResponse.json()
      
      // Filter for students enrolled in this section
      const studentList = allStudents
        .filter(student => student.enrollments?.some(e => e.sectionId === sectionId && e.isActive))
        .map(student => ({
          id: student.id,
          email: student.email,
          first_name: student.firstName,
          last_name: student.lastName
        }))
      setStudents(studentList)
      setFilteredStudents(studentList)

      // Fetch existing attendance records from backend API
      const recordsResponse = await fetch(`${API_BASE_URL}/api/attendance/sessions/${sessionId}/records`)
      if (!recordsResponse.ok) throw new Error('Failed to fetch attendance records')
      
      const records = await recordsResponse.json()

      // Convert to map for easy lookup (transform backend format to frontend format)
      const recordsMap = {}
      records.forEach(record => {
        recordsMap[record.userId] = {
          id: record.id,
          user_id: record.userId,
          session_id: record.sessionId,
          status: record.status,
          checkin_time: record.checkinTime,
          checkout_time: record.checkoutTime,
          notes: record.notes
        }
      })
      setAttendanceRecords(recordsMap)

    } catch (error) {
      console.error('Error fetching students:', error)
      alert('Failed to load students')
    }
  }

  const markAttendance = async (userId, status, notes = '', checkinTime = null) => {
    try {
      const requestBody = {
        sessionId: selectedSession.id,
        userId: userId,
        status: status,
        checkinTime: checkinTime || (status === 'PRESENT' || status === 'LATE' ? new Date().toISOString() : null),
        notes: notes || null
      }

      const response = await fetch(`${API_BASE_URL}/api/attendance/records`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(requestBody)
      })

      if (!response.ok) {
        const errorData = await response.json()
        throw new Error(errorData.error || 'Failed to mark attendance')
      }

      const savedRecord = await response.json()

      await fetchStudentsAndRecords(selectedSession.id, selectedSession.section_id)
    } catch (error) {
      console.error('Error marking attendance:', error)
      alert('Failed to mark attendance: ' + error.message)
    }
  }

  const calculateStatus = (checkinDateTime, sessionDate, scheduledStartTime) => {
    if (!checkinDateTime || !sessionDate || !scheduledStartTime) return 'PRESENT'

    // Parse the check-in date/time (SGT)
    const checkin = new Date(checkinDateTime)
    
    // Parse session date and time (SGT)
    const sessionDateTime = new Date(`${sessionDate}T${scheduledStartTime}`)
    
    // Calculate difference in minutes
    const diffMinutes = (checkin - sessionDateTime) / 1000 / 60
    
    console.log('Checkin DateTime:', checkin.toISOString())
    console.log('Session DateTime:', sessionDateTime.toISOString())
    console.log('Difference (minutes):', diffMinutes)

    // If checking in more than 15 minutes after scheduled start, mark as LATE
    // If checking in before or within 15 minutes of scheduled start, mark as PRESENT
    return diffMinutes >= 15 ? 'LATE' : 'PRESENT'
  }

  const handleCheckIn = (userId) => {
    // Get current date/time in SGT
    const now = new Date()
    
    console.log('=== CHECK IN DEBUG ===')
    console.log('Current Date Object:', now)
    console.log('Session Date:', selectedSession.session_date)
    console.log('Scheduled Start Time:', selectedSession.scheduled_start_time)
    
    const status = calculateStatus(
      now, 
      selectedSession.session_date, 
      selectedSession.scheduled_start_time
    )
    console.log('Calculated Status:', status)
    console.log('===================')
    
    // Store as ISO string (UTC) - will be converted to SGT on display
    markAttendance(userId, status, '', now.toISOString())
  }

  if (loading) {
    return <div className="loading">Loading attendance...</div>
  }

  return (
    <div className="container">
      <div className="page-header">
        <h1>Mark Attendance</h1>
        <button onClick={() => navigate('/home')} className="btn btn-secondary-small">
          Back to Home
        </button>
      </div>

      <div className="tab-content">
            {/* Search and Filter Component */}
            <div className="filters-section">
              <div className="filters-header">
                <h3>Search & Filter</h3>
              </div>
              <div className="search-filter-bar">
                <div className="search-block">
                  <label className="filter-label" htmlFor="sessions-search">Search</label>
                  <div className="search-input-wrapper">
                    <input
                      id="sessions-search"
                      type="text"
                      placeholder="Search by section or course..."
                      value={searchTerm}
                      onChange={(e) => setSearchTerm(e.target.value)}
                      className="search-input"
                    />
                    <span className="search-icon">⚲</span>
                  </div>
                </div>

                <div className="filter-block">
                  <label className="filter-label" htmlFor="section-filter">Section</label>
                  <select
                    id="section-filter"
                    value={sectionFilter}
                    onChange={(e) => setSectionFilter(e.target.value)}
                    className="filter-select"
                  >
                    <option value="all">All Sections</option>
                    {sectionList.map(section => (
                      <option key={section.id} value={section.id}>
                        {section.section_code} ({section.year} S{section.semester})
                      </option>
                    ))}
                  </select>
                </div>

                <div className="filter-block">
                  <label className="filter-label" htmlFor="year-filter">Year</label>
                  <select
                    id="year-filter"
                    value={yearFilter}
                    onChange={(e) => setYearFilter(e.target.value)}
                    className="filter-select"
                  >
                    <option value="all">All Years</option>
                    {yearOptions.map(year => (
                      <option key={year} value={year}>{year}</option>
                    ))}
                  </select>
                </div>

                <div className="filter-block">
                  <label className="filter-label" htmlFor="semester-filter">Semester</label>
                  <select
                    id="semester-filter"
                    value={semesterFilter}
                    onChange={(e) => setSemesterFilter(e.target.value)}
                    className="filter-select"
                  >
                    <option value="all">All Semesters</option>
                    {semesterOptions.map(sem => (
                      <option key={sem} value={sem}>Semester {sem}</option>
                    ))}
                  </select>
                </div>

                <div className="filter-block">
                  <label className="filter-label" htmlFor="status-filter">Status</label>
                  <select
                    id="status-filter"
                    value={statusFilterSessions}
                    onChange={(e) => setStatusFilterSessions(e.target.value)}
                    className="filter-select"
                  >
                    <option value="all">All Status</option>
                    <option value="SCHEDULED">Scheduled</option>
                    <option value="IN_PROGRESS">In Progress</option>
                    <option value="COMPLETED">Completed</option>
                    <option value="CANCELLED">Cancelled</option>
                  </select>
                </div>
              </div>
            </div>

            <div className="panel-header">
              <h3>Attendance Sessions</h3>
              <button
                onClick={() => {
                  resetSessionForm()
                  setShowSessionModal(true)
                }}
                className="btn btn-primary-small"
              >
                Create Session
              </button>
            </div>

            <div className="table-container">
              <table className="data-table">
                <thead>
                  <tr>
                    <th
                      onClick={() => handleSort('session_date')}
                      className="sortable"
                    >
                      Date {getSortIcon('session_date')}
                    </th>
                    <th>Section</th>
                    <th>Time</th>
                    <th
                      onClick={() => handleSort('status')}
                      className="sortable"
                    >
                      Status {getSortIcon('status')}
                    </th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredSessions.map(session => (
                    <tr key={session.id}>
                      <td>{new Date(session.session_date).toLocaleDateString('en-SG', { 
                        timeZone: 'Asia/Singapore',
                        day: '2-digit',
                        month: '2-digit',
                        year: 'numeric'
                      })}</td>
                      <td>
                        {session.sections?.section_code}
                        <br />
                        <small style={{ color: '#6b7280' }}>
                          {session.sections?.year} Semester {session.sections?.semester}
                        </small>
                      </td>
                      <td>{session.scheduled_start_time} - {session.scheduled_end_time}</td>
                      <td>
                        <span className={`status-badge status-${session.status.toLowerCase()}`}>
                          {session.status}
                        </span>
                      </td>
                      <td>
                        <button
                          onClick={() => openMarkAttendance(session)}
                          className="btn btn-small btn-action"
                        >
                          Mark Attendance
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
      </div>

      {/* Mark Attendance Modal */}
      {showMarkingModal && selectedSession && (
        <div className="modal-overlay" onClick={closeMarkingModal}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '1200px', width: '95%' }}>
            <div className="modal-header">
              <div>
                <h2>Mark Attendance</h2>
                <p style={{ color: '#6b7280', margin: '0.5rem 0 0 0', fontSize: '0.875rem' }}>
                  {selectedSession.sections?.section_code} ({selectedSession.sections?.year} S{selectedSession.sections?.semester})
                </p>
                <p style={{ color: '#6b7280', margin: '0.25rem 0 0 0', fontSize: '0.875rem' }}>
                  {new Date(selectedSession.session_date).toLocaleDateString('en-SG', { 
                    timeZone: 'Asia/Singapore',
                    day: '2-digit',
                    month: '2-digit',
                    year: 'numeric'
                  })} • {selectedSession.scheduled_start_time} - {selectedSession.scheduled_end_time}
                </p>
              </div>
              <button onClick={closeMarkingModal} className="close-button">
                ✕
              </button>
            </div>

            <div className="modal-body">

              {/* Search and Filter Component */}
              <div className="filters-section">
                <div className="filters-header">
                  <h3>Search & Filter</h3>
                </div>
                <div className="search-filter-bar">
                  <div className="search-block">
                    <label className="filter-label" htmlFor="marking-search">Search</label>
                    <div className="search-input-wrapper">
                      <input
                        id="marking-search"
                        type="text"
                        placeholder="Search by student ID or name..."
                        value={markingSearchTerm}
                        onChange={(e) => setMarkingSearchTerm(e.target.value)}
                        className="search-input"
                      />
                      <span className="search-icon">⚲</span>
                    </div>
                  </div>

                  <div className="filter-block">
                    <label className="filter-label" htmlFor="marking-status-filter">Status</label>
                    <select
                      id="marking-status-filter"
                      value={statusFilterMarking}
                      onChange={(e) => setStatusFilterMarking(e.target.value)}
                      className="filter-select"
                    >
                      <option value="all">All Students</option>
                      <option value="unmarked">Unmarked</option>
                      <option value="PRESENT">Present</option>
                      <option value="LATE">Late</option>
                      <option value="ABSENT">Absent</option>
                    </select>
                  </div>
                </div>
              </div>

              <div className="table-container">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Student ID</th>
                    <th>Name</th>
                    <th>Status</th>
                    <th>Check-in Time</th>
                    <th>Notes/Remarks</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredStudents.map(student => {
                    const record = attendanceRecords[student.id]
                    return (
                      <tr key={student.id}>
                        <td>{student.id}</td>
                        <td>{student.first_name} {student.last_name}</td>
                        <td>
                          {record ? (
                            <span className={`status-badge status-${record.status.toLowerCase()}`}>
                              {record.status}
                            </span>
                          ) : (
                            <span style={{ color: '#9ca3af', fontSize: '0.875rem' }}>Not marked</span>
                          )}
                        </td>
                        <td style={{ fontSize: '0.875rem' }}>
                          {record?.checkin_time 
                            ? (() => {
                                // Database is storing in a timezone-aware way already
                                // Just add 16 hours to compensate (8 hours lost + 8 hours SGT offset)
                                const dbDate = new Date(record.checkin_time)
                                const sgtDate = new Date(dbDate.getTime() + (16 * 60 * 60 * 1000))
                                const hours = String(sgtDate.getUTCHours()).padStart(2, '0')
                                const minutes = String(sgtDate.getUTCMinutes()).padStart(2, '0')
                                const seconds = String(sgtDate.getUTCSeconds()).padStart(2, '0')
                                return `${hours}:${minutes}:${seconds}`
                              })()
                            : '-'}
                        </td>
                        <td>
                          <input
                            type="text"
                            value={record?.notes || ''}
                            onChange={(e) => {
                              if (record) {
                                markAttendance(student.id, record.status, e.target.value, record.checkin_time)
                              }
                            }}
                            placeholder="MC, emergency, etc."
                            className="form-input"
                            style={{ minWidth: '200px', padding: '0.375rem 0.75rem', fontSize: '0.875rem' }}
                          />
                        </td>
                        <td>
                          <div style={{ display: 'flex', gap: '0.5rem' }}>
                            <button
                              onClick={() => handleCheckIn(student.id)}
                              className="btn btn-small btn-action"
                            >
                              Check In
                            </button>
                            <button
                              onClick={() => markAttendance(student.id, 'ABSENT')}
                              className="btn btn-small btn-action"
                            >
                              Absent
                            </button>
                            <label
                              htmlFor={`file-upload-${student.id}`}
                              className="btn btn-small btn-action"
                              style={{ 
                                cursor: 'pointer', 
                                margin: 0,
                                display: 'inline-flex',
                                alignItems: 'center',
                                justifyContent: 'center'
                              }}
                            >
                              Upload
                            </label>
                            <input
                              id={`file-upload-${student.id}`}
                              type="file"
                              accept="image/*,.pdf"
                              style={{ display: 'none' }}
                              onChange={(e) => {
                                const file = e.target.files?.[0]
                                if (file) {
                                  alert(`File upload functionality: ${file.name}\n\nNote: File storage integration with Supabase Storage will be implemented in the next phase.`)
                                }
                              }}
                            />
                          </div>
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Create/Edit Session Modal */}
      {showSessionModal && (
        <div className="modal-overlay" onClick={() => setShowSessionModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>{editingSession ? 'Edit Session' : 'Create Session'}</h2>
              <button onClick={() => setShowSessionModal(false)} className="close-button">
                ✕
              </button>
            </div>

            <div className="modal-body">
              <div className="form-group">
                <label className="form-label">Section</label>
                <select
                  value={sessionForm.section_id}
                  onChange={(e) => {
                    const sectionId = e.target.value
                    const section = sectionList.find(s => s.id === parseInt(sectionId))
                    setSessionForm({
                      ...sessionForm,
                      section_id: sectionId,
                      scheduled_start_time: section?.start_time || '09:00',
                      scheduled_end_time: section?.end_time || '10:30'
                    })
                  }}
                  className="form-input"
                  required
                >
                  <option value="">Select Section</option>
                  {sectionList.map(section => (
                    <option key={section.id} value={section.id}>
                      {section.section_code} ({section.year} S{section.semester})
                    </option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label className="form-label">Session Date</label>
                <input
                  type="date"
                  value={sessionForm.session_date}
                  onChange={(e) => setSessionForm({ ...sessionForm, session_date: e.target.value })}
                  className="form-input"
                  required
                />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', marginBottom: '1.5rem' }}>
                <div className="form-group" style={{ marginBottom: 0 }}>
                  <label className="form-label">Start Time</label>
                  <input
                    type="time"
                    value={sessionForm.scheduled_start_time}
                    onChange={(e) => setSessionForm({ ...sessionForm, scheduled_start_time: e.target.value })}
                    className="form-input"
                    required
                  />
                </div>

                <div className="form-group" style={{ marginBottom: 0 }}>
                  <label className="form-label">End Time</label>
                  <input
                    type="time"
                    value={sessionForm.scheduled_end_time}
                    onChange={(e) => setSessionForm({ ...sessionForm, scheduled_end_time: e.target.value })}
                    className="form-input"
                    required
                  />
                </div>
              </div>

              <div className="form-group">
                <label className="form-label">Status</label>
                <select
                  value={sessionForm.status}
                  onChange={(e) => setSessionForm({ ...sessionForm, status: e.target.value })}
                  className="form-input"
                >
                  <option value="SCHEDULED">Scheduled</option>
                  <option value="IN_PROGRESS">In Progress</option>
                  <option value="COMPLETED">Completed</option>
                  <option value="CANCELLED">Cancelled</option>
                </select>
              </div>

              <div className="form-group" style={{ marginBottom: '0' }}>
                <label className="form-label">Notes (Optional)</label>
                <textarea
                  value={sessionForm.notes}
                  onChange={(e) => setSessionForm({ ...sessionForm, notes: e.target.value })}
                  className="form-input"
                  rows="3"
                  placeholder="Add any notes about this session..."
                />
              </div>

              <div className="form-actions" style={{ marginTop: '1.25rem', marginBottom: '0.5rem' }}>
                <button onClick={saveSession} className="btn btn-primary">
                  {editingSession ? 'Update Session' : 'Create Session'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

export default Attendance
