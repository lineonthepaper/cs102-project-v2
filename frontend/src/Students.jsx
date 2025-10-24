import { useState, useEffect, Fragment } from 'react'
import { useNavigate } from 'react-router-dom'
import { supabase } from './supabase'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

function Students() {
  const navigate = useNavigate()
  const [students, setStudents] = useState([])
  const [filteredStudents, setFilteredStudents] = useState([])
  const [loading, setLoading] = useState(true)
  const [searchTerm, setSearchTerm] = useState('')
  const [sortBy, setSortBy] = useState('default')
  const [sortOrder, setSortOrder] = useState('asc')
  const [courseFilter, setCourseFilter] = useState('all')
  const [sectionFilter, setSectionFilter] = useState('all')
  const [courses, setCourses] = useState([])
  const [sections, setSections] = useState([])
  const [selectedStudent, setSelectedStudent] = useState(null)
  const [showModal, setShowModal] = useState(false)
  const [showAddStudentModal, setShowAddStudentModal] = useState(false)
  const [showManageEnrollmentModal, setShowManageEnrollmentModal] = useState(false)
  const [manageEnrollmentStudent, setManageEnrollmentStudent] = useState(null)
  const [enrollmentSelections, setEnrollmentSelections] = useState({})
  const [savingEnrollment, setSavingEnrollment] = useState(false)
  const [expandedCourses, setExpandedCourses] = useState({})
  const parsedCourseFilter = Number(courseFilter)
  const selectedCourseId =
    courseFilter === 'all' || Number.isNaN(parsedCourseFilter) ? null : parsedCourseFilter
  const availableSections = (() => {
    if (sections.length === 0) return []
    if (selectedCourseId === null || Number.isNaN(selectedCourseId)) {
      return sections
    }
    return sections.filter(
      (section) => Number(section.course_id) === selectedCourseId
    )
  })()

  useEffect(() => {
    fetchStudents()
    fetchCourses()
    fetchSections()
  }, [])

  useEffect(() => {
    filterAndSortStudents()
  }, [students, searchTerm, sortBy, sortOrder, courseFilter, sectionFilter])

  useEffect(() => {
    setSectionFilter('all')
  }, [courseFilter])

  useEffect(() => {
    if (!selectedStudent) return
    const updatedStudent = filteredStudents.find(
      (student) => student.id === selectedStudent.id
    )

    if (updatedStudent) {
      if (updatedStudent !== selectedStudent) {
        setSelectedStudent(updatedStudent)
      }
    } else {
      setSelectedStudent(null)
      setShowModal(false)
    }
  }, [filteredStudents, selectedStudent])

  const fetchStudents = async () => {
    try {
      const response = await fetch(`${API_BASE_URL}/api/students`)
      if (!response.ok) {
        throw new Error('Failed to fetch students')
      }

      const studentsData = await response.json()

      // Transform Java DTO to match frontend expectations
      const transformedStudents = studentsData.map(student => ({
        ...student,
        id: student.id,
        displayId: student.displayId,
        first_name: student.firstName,
        last_name: student.lastName,
        email: student.email,
        enabled: student.enabled,
        totalSessions: student.totalSessions,
        presentSessions: student.presentSessions,
        lateSessions: student.lateSessions,
        attendanceRate: student.attendanceRate,
        punctualityRate: student.punctualityRate,
        attendanceRecords: (student.attendanceRecords || []).map(record => ({
          ...record,
          user_id: record.userId,
          session_id: record.sessionId,
          status: record.status,
          checkin_time: record.checkinTime,
          checkout_time: record.checkoutTime,
          attendance_sessions: record.attendanceSession ? {
            session_date: record.attendanceSession.sessionDate,
            sections: record.attendanceSession.section ? {
              id: record.attendanceSession.section.id,
              section_code: record.attendanceSession.section.sectionCode,
              courses: record.attendanceSession.section.course ? {
                id: record.attendanceSession.section.course.id,
                code: record.attendanceSession.section.course.code
              } : null
            } : null
          } : null
        })),
        enrollments: (student.enrollments || []).map(enrollment => ({
          ...enrollment,
          user_id: enrollment.userId,
          section_id: enrollment.sectionId,
          is_active: enrollment.isActive,
          enrolled_at: enrollment.enrolledAt,
          sections: enrollment.section ? {
            id: enrollment.section.id,
            section_code: enrollment.section.sectionCode,
            year: enrollment.section.year,
            semester: enrollment.section.semester,
            meeting_day: enrollment.section.meetingDay,
            start_time: enrollment.section.startTime,
            end_time: enrollment.section.endTime,
            location: enrollment.section.location,
            courses: enrollment.section.course ? {
              id: enrollment.section.course.id,
              code: enrollment.section.course.code
            } : null
          } : null
        }))
      }))

      setStudents(transformedStudents)
    } catch (error) {
      console.error('Error fetching students:', error)
      alert('Failed to fetch students from server')
    } finally {
      setLoading(false)
    }
  }

  const filterAndSortStudents = () => {
    const parsedCourseId = Number(courseFilter)
    const courseId =
      courseFilter === 'all' || Number.isNaN(parsedCourseId) ? null : parsedCourseId
    const parsedSectionId = Number(sectionFilter)
    const sectionId =
      sectionFilter === 'all' || Number.isNaN(parsedSectionId) ? null : parsedSectionId
    let filtered = [...students]

    // Apply search filter
    if (searchTerm) {
      const searchLower = searchTerm.toLowerCase()
      const includesSearch = (value) =>
        typeof value === 'string'
          ? value.toLowerCase().includes(searchLower)
          : value != null && String(value).toLowerCase().includes(searchLower)

      filtered = filtered.filter(student =>
        includesSearch(`${student.first_name} ${student.last_name}`) ||
        includesSearch(student.email) ||
        includesSearch(student.displayId) ||
        includesSearch(student.id)
      )
    }

    filtered = filtered
      .map((student) => {
        const attendanceRecords = student.attendanceRecords || []
        const enrollments = student.enrollments || []

        const filteredAttendanceRecords = attendanceRecords.filter((record) => {
          const recordCourseId = Number(record.attendance_sessions?.sections?.courses?.id)
          const recordSectionId = Number(record.attendance_sessions?.sections?.id)

          if (courseId !== null && recordCourseId !== courseId) {
            return false
          }

          if (sectionId !== null && recordSectionId !== sectionId) {
            return false
          }

          return true
        })

        const filteredEnrollments = enrollments.filter((enrollment) => {
          const enrollmentCourseId = Number(enrollment.sections?.courses?.id)
          const enrollmentSectionId = Number(enrollment.section_id)

          if (courseId !== null && enrollmentCourseId !== courseId) {
            return false
          }

          if (sectionId !== null && enrollmentSectionId !== sectionId) {
            return false
          }

          return true
        })

        const hasFilterSelection = courseId !== null || sectionId !== null
        const hasFilteredMatch =
          filteredAttendanceRecords.length > 0 || filteredEnrollments.length > 0

        if (hasFilterSelection && !hasFilteredMatch) {
          return null
        }

        const totalSessions = filteredAttendanceRecords.length
        const lateSessions = filteredAttendanceRecords.filter(
          (record) => record.status === 'LATE'
        ).length
        const presentSessions = filteredAttendanceRecords.filter(
          (record) => record.status === 'PRESENT' || record.status === 'LATE'
        ).length
        const absentSessions = Math.max(totalSessions - presentSessions, 0)

        const attendanceRate =
          totalSessions > 0 ? Math.round((presentSessions / totalSessions) * 100) : 0
        const punctualityRate =
          totalSessions > 0
            ? Math.round(((totalSessions - lateSessions) / totalSessions) * 100)
            : 0

        return {
          ...student,
          filteredAttendanceRecords,
          filteredEnrollments,
          filteredTotalSessions: totalSessions,
          filteredPresentSessions: presentSessions,
          filteredLateSessions: lateSessions,
          filteredAbsentSessions: absentSessions,
          filteredAttendanceRate: attendanceRate,
          filteredPunctualityRate: punctualityRate
        }
      })
      .filter(Boolean)

    // Apply sorting
    if (sortBy !== 'default') {
      filtered.sort((a, b) => {
        const resolveValue = (student, key) => {
          if (key === 'id') {
            return student.displayId ?? student.id
          }
          if (key === 'attendanceRate') {
            return student.filteredAttendanceRate ?? student.attendanceRate ?? 0
          }
          if (key === 'punctualityRate') {
            return student.filteredPunctualityRate ?? student.punctualityRate ?? 0
          }
          return student[key]
        }

        let aVal = resolveValue(a, sortBy)
        let bVal = resolveValue(b, sortBy)

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

    setFilteredStudents(filtered)
  }

  const fetchCourses = async () => {
    try {
      const { data: coursesData, error: coursesError } = await supabase
        .from('courses')
        .select('*')

      if (coursesError) throw coursesError
      setCourses(coursesData || [])
    } catch (error) {
      console.error('Error fetching courses:', error)
    }
  }

  const fetchSections = async () => {
    try {
      const { data: sectionsData, error: sectionsError } = await supabase
        .from('sections')
        .select(`
          id,
          section_code,
          course_id,
          year,
          semester,
          meeting_day,
          start_time,
          end_time,
          location
        `)
        .order('course_id', { ascending: true })
        .order('section_code', { ascending: true })

      if (sectionsError) throw sectionsError
      setSections(sectionsData || [])
    } catch (error) {
      console.error('Error fetching sections:', error)
    }
  }

  const formatMeetingDay = (value) => {
    if (value === null || value === undefined) return ''
    const dayNames = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday']
    return dayNames[value] ?? `Day ${value}`
  }

  const formatSectionSummary = (section) => {
    if (!section) return 'Not enrolled'
    const termLabel = section.year && section.semester ? `${section.year} S${section.semester}` : null
    const parts = [
      section.section_code,
      termLabel,
      section.meeting_day != null ? formatMeetingDay(section.meeting_day) : null,
      section.start_time ? section.start_time.slice(0, 5) : null
    ].filter(Boolean)
    return parts.join(' • ')
  }

  const formatTimeRange = (start, end) => {
    if (!start && !end) return ''
    const startFormatted = start ? start.slice(0, 5) : null
    const endFormatted = end ? end.slice(0, 5) : null
    if (startFormatted && endFormatted) {
      return `${startFormatted} – ${endFormatted}`
    }
    return startFormatted || endFormatted || ''
  }

  const formatSessionDate = (value) => {
    if (!value) return '—'
    const date = new Date(value)
    if (Number.isNaN(date.getTime())) return '—'
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    })
  }

  const formatTimeValue = (value) => {
    if (!value) return '—'
    const date = new Date(value)
    if (Number.isNaN(date.getTime())) return '—'
    return date.toLocaleTimeString('en-US', {
      hour: '2-digit',
      minute: '2-digit',
      hour12: false
    })
  }

  const selectedStudentSummary = (() => {
    if (!selectedStudent) return null

    const recordsSource =
      selectedStudent.filteredAttendanceRecords ??
      selectedStudent.attendanceRecords ??
      []
    const records = [...recordsSource]
    const recordTimestamp = (record) => {
      const value = record.attendance_sessions?.session_date
      if (!value) return 0
      const date = new Date(value)
      return Number.isNaN(date.getTime()) ? 0 : date.getTime()
    }
    records.sort((a, b) => recordTimestamp(b) - recordTimestamp(a))

    const totalSessions =
      selectedStudent.filteredTotalSessions ??
      records.length ??
      0
    const presentSessions =
      selectedStudent.filteredPresentSessions ??
      records.filter((record) => record.status === 'PRESENT' || record.status === 'LATE').length
    const lateSessions =
      selectedStudent.filteredLateSessions ??
      records.filter((record) => record.status === 'LATE').length
    const absentSessions =
      selectedStudent.filteredAbsentSessions ??
      Math.max(totalSessions - presentSessions, 0)

    const attendanceRate =
      selectedStudent.filteredAttendanceRate ??
      (totalSessions > 0 ? Math.round((presentSessions / totalSessions) * 100) : 0)

    const punctualityRate =
      selectedStudent.filteredPunctualityRate ??
      (totalSessions > 0
        ? Math.round(((totalSessions - lateSessions) / totalSessions) * 100)
        : 0)

    const metrics = [
      { key: 'totalSessions', label: 'Total Sessions', value: totalSessions },
      { key: 'presentSessions', label: 'Sessions Present', value: presentSessions },
      { key: 'lateSessions', label: 'Sessions Late', value: lateSessions },
      { key: 'absentSessions', label: 'Sessions Absent', value: absentSessions },
      { key: 'attendanceRate', label: 'Attendance Rate', value: `${attendanceRate}%` },
      { key: 'punctualityRate', label: 'Punctuality', value: `${punctualityRate}%` }
    ]

    return {
      totalSessions,
      presentSessions,
      lateSessions,
      absentSessions,
      attendanceRate,
      punctualityRate,
      metrics,
      records
    }
  })()

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

  const getSortIcon = (column) => {
    if (sortBy !== column) return '↕'
    if (sortOrder === 'default') return '—'
    return sortOrder === 'asc' ? '↑' : '↓'
  }

  const handleStudentClick = (student) => {
    setSelectedStudent(student)
    setShowModal(true)
  }

  const closeModal = () => {
    setShowModal(false)
    setSelectedStudent(null)
  }

  const closeAddStudentModal = () => {
    setShowAddStudentModal(false)
  }

  const openManageEnrollmentModal = (student) => {
    const initialSelections = {}
    const expanded = {}
    ;(student.enrollments || []).forEach((enrollment) => {
      const courseId = enrollment.sections?.courses?.id || null
      if (courseId) {
        initialSelections[courseId] = enrollment.section_id
      }
    })
    setEnrollmentSelections(initialSelections)
    setExpandedCourses(expanded)
    setManageEnrollmentStudent(student)
    setShowManageEnrollmentModal(true)
  }

  const closeManageEnrollmentModal = () => {
    setShowManageEnrollmentModal(false)
    setManageEnrollmentStudent(null)
    setEnrollmentSelections({})
    setSavingEnrollment(false)
    setExpandedCourses({})
  }

  const handleSectionToggle = (courseId, sectionId, isChecked) => {
    setEnrollmentSelections((prev) => {
      const next = { ...prev }
      if (isChecked) {
        next[courseId] = sectionId
      } else if (next[courseId] === sectionId) {
        delete next[courseId]
      }
      return next
    })
  }

  const toggleCourseAccordion = (courseId) => {
    setExpandedCourses((prev) => ({
      ...prev,
      [courseId]: !prev[courseId]
    }))
  }

  const handleSaveEnrollment = async () => {
    if (!manageEnrollmentStudent) return
    setSavingEnrollment(true)

    try {
      const studentId = manageEnrollmentStudent.id
      const selectedSectionIds = Object.values(enrollmentSelections).filter((value) => value)

      const response = await fetch(`${API_BASE_URL}/api/students/${studentId}/enrollments`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          sectionIds: selectedSectionIds
        })
      })

      if (!response.ok) {
        const errorData = await response.json()
        throw new Error(errorData.message || 'Failed to update enrollments')
      }

      await fetchStudents()
      closeManageEnrollmentModal()
    } catch (error) {
      console.error('Error updating enrollments:', error)
      alert(error.message || 'Failed to update enrollments')
    } finally {
      setSavingEnrollment(false)
    }
  }

  if (loading) {
    return <div className="loading">Loading students...</div>
  }

  return (
    <div className="container">
      <div className="page-header">
        <h1>Students Management</h1>
        <button onClick={() => navigate('/home')} className="btn btn-secondary-small">
          Back to Home
        </button>
      </div>

      {/* Search and Filters */}
      <div className="filters-section">
        <div className="filters-header">
          <h3>Search & Filter</h3>
        </div>
        <div className="search-filter-bar">
          <div className="search-block">
            <label className="filter-label" htmlFor="student-search">Search</label>
            <div className="search-input-wrapper">
              <input
                id="student-search"
                type="text"
                placeholder="Search by name, email, or ID..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="search-input"
              />
              <span className="search-icon">⚲</span>
            </div>
          </div>

          <div className="filter-block">
            <label className="filter-label" htmlFor="course-filter">Course</label>
            <select
              id="course-filter"
              value={courseFilter}
              onChange={(e) => setCourseFilter(e.target.value)}
              className="filter-select"
            >
              <option value="all">All Courses</option>
              {courses.map(course => (
                <option key={course.id} value={course.id}>
                  {course.code} - {course.title}
                </option>
              ))}
            </select>
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
              {availableSections.map((section) => (
                <option key={section.id} value={section.id}>
                  {section.section_code} - {section.year && section.semester ? `${section.year} S${section.semester}` : 'No term'}
                </option>
              ))}
            </select>
          </div>

        </div>
      </div>

      {/* Students Table */}
      <div className="table-header">
        <button onClick={() => setShowAddStudentModal(true)} className="btn btn-primary-small">
          Add Student
        </button>
      </div>
      <div className="table-container">
        <table className="students-table">
          <thead>
            <tr>
              <th onClick={() => handleSort('id')} className="sortable">
                ID {getSortIcon('id')}
              </th>
              <th onClick={() => handleSort('last_name')} className="sortable">
                Name {getSortIcon('last_name')}
              </th>
              <th onClick={() => handleSort('email')} className="sortable">
                Email {getSortIcon('email')}
              </th>
              <th onClick={() => handleSort('attendanceRate')} className="sortable">
                Attendance {getSortIcon('attendanceRate')}
              </th>
              <th onClick={() => handleSort('punctualityRate')} className="sortable">
                Punctuality {getSortIcon('punctualityRate')}
              </th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {filteredStudents.map((student) => {
              const attendanceRateValue =
                student.filteredAttendanceRate ?? student.attendanceRate ?? 0
              const punctualityRateValue =
                student.filteredPunctualityRate ?? student.punctualityRate ?? 0
              return (
                <tr key={student.displayId || student.id}>
                  <td className="student-id">{student.displayId || student.id}</td>
                  <td className="student-name">
                    {student.first_name} {student.last_name}
                  </td>
                  <td>{student.email}</td>
                  <td>
                    <div className="attendance-rate">
                      <span className="rate-text">{attendanceRateValue}%</span>
                      <div className="rate-bar">
                        <div
                          className="rate-fill"
                          style={{ width: `${attendanceRateValue}%` }}
                        ></div>
                      </div>
                    </div>
                  </td>
                  <td>
                    <div className="attendance-rate">
                      <span className="rate-text">{punctualityRateValue}%</span>
                      <div className="rate-bar">
                        <div
                          className="rate-fill"
                          style={{ width: `${punctualityRateValue}%` }}
                        ></div>
                      </div>
                    </div>
                  </td>
                  <td className="action-buttons">
                    <button
                      onClick={() => handleStudentClick(student)}
                      className="btn btn-small btn-action"
                      style={{ marginRight: '0.5rem' }}
                    >
                      View Details
                    </button>
                    <button
                      onClick={() => openManageEnrollmentModal(student)}
                      className="btn btn-small btn-action"
                    >
                      Manage Enrolment
                    </button>
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>

        {filteredStudents.length === 0 && (
          <div className="no-results">
            <p>No students found matching your criteria.</p>
          </div>
        )}
      </div>

      {/* Student Details Modal */}
      {showModal && selectedStudent && (
        <div className="modal-overlay" onClick={closeModal}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>Student Overview</h2>
              <button onClick={closeModal} className="close-button">
                ✕
              </button>
            </div>

            <div className="student-info">
              <div className="student-header">
                <div className="student-details">
                  <div className="student-name-row">
                    <h3>{selectedStudent.first_name} {selectedStudent.last_name}</h3>
                    <span className="student-id-badge">
                      {selectedStudent.displayId || selectedStudent.id}
                    </span>
                  </div>
                  <p className="student-email">{selectedStudent.email}</p>
                </div>
              </div>
            </div>

            <div className="modal-body student-details-body">
              <div className="details-overview">
                <div className="details-overview-header">
                  <h3>Attendance Summary</h3>
                  {selectedStudentSummary && (
                    <span className="details-overview-meta">
                      {selectedStudentSummary.totalSessions} sessions tracked
                    </span>
                  )}
                </div>

                <div className="metrics-grid">
                  {selectedStudentSummary?.metrics.map((metric) => (
                    <div key={metric.key} className="metric-card">
                      <span className="metric-label">{metric.label}</span>
                      <span className="metric-value">{metric.value}</span>
                      {metric.caption && (
                        <span className="metric-caption">{metric.caption}</span>
                      )}
                    </div>
                  ))}
                </div>
              </div>

              <div className="attendance-history">
                <div className="attendance-history-header">
                  <h3>Attendance History</h3>
                  {selectedStudentSummary && (
                    <span className="history-meta">
                      {selectedStudentSummary.records.length} records
                    </span>
                  )}
                </div>

                {selectedStudentSummary && selectedStudentSummary.records.length > 0 ? (
                  <div className="history-table-wrapper">
                    <table className="attendance-table">
                      <thead>
                        <tr>
                          <th>Date</th>
                          <th>Course</th>
                          <th>Section</th>
                          <th>Check In</th>
                          <th>Check Out</th>
                          <th>Status</th>
                        </tr>
                      </thead>
                      <tbody>
                        {selectedStudentSummary.records.map((record) => {
                          const session = record.attendance_sessions
                          const section = session?.sections

                          return (
                            <tr key={record.id}>
                              <td>{formatSessionDate(session?.session_date)}</td>
                              <td>{section?.courses?.code || '—'}</td>
                              <td>{section?.section_code || '—'}</td>
                              <td>{formatTimeValue(record.checkin_time)}</td>
                              <td>{formatTimeValue(record.checkout_time)}</td>
                              <td>
                                <span className={`status-badge ${(record.status || '').toLowerCase()}`}>
                                  {record.status || '—'}
                                </span>
                              </td>
                            </tr>
                          )
                        })}
                      </tbody>
                    </table>
                  </div>
                ) : (
                  <p className="no-records">No attendance records found for this student.</p>
                )}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Manage Sections Modal */}
      {showManageEnrollmentModal && manageEnrollmentStudent && (
        <div className="modal-overlay" onClick={closeManageEnrollmentModal}>
          <div className="modal-content manage-modal" onClick={(e) => e.stopPropagation()}>
            <div className="manage-modal-header">
              <div className="manage-modal-title">
                <h2>Manage Sections</h2>
                <div className="manage-summary">
                  <div className="name-with-id">
                    {manageEnrollmentStudent.first_name} {manageEnrollmentStudent.last_name}
                    <span className="tag-id">{manageEnrollmentStudent.displayId || manageEnrollmentStudent.id}</span>
                  </div>
                  <div className="email">{manageEnrollmentStudent.email}</div>
                </div>
              </div>
              <button onClick={closeManageEnrollmentModal} className="close-button">✕</button>
            </div>

            <div className="manage-modal-body manage-enrollment-body">
              {courses.length === 0 ? (
                <p className="empty-state">No courses available to enrol.</p>
              ) : (
                <div className="course-list simple-course-list">
                  {courses.map((course) => {
                    const courseSections = sections.filter(section => Number(section.course_id) === Number(course.id))
                    const selectedSectionId = enrollmentSelections[course.id] ?? null
                    const currentSection = sections.find((section) => Number(section.id) === Number(selectedSectionId))
                    const isExpanded = expandedCourses[course.id] ?? false
                    const selectionLabel = currentSection ? formatSectionSummary(currentSection) : 'Not enrolled'

                    return (
                      <Fragment key={course.id}>
                        <button
                          type="button"
                          className={`course-accordion-toggle ${isExpanded ? 'open' : ''}`}
                          onClick={() => toggleCourseAccordion(course.id)}
                        >
                          <div className="course-title-group">
                            <span className="course-code">{course.code}</span>
                            <span className="course-name">{course.title}</span>
                          </div>
                          <div className={`course-selection-pill ${currentSection ? '' : 'empty'}`}>
                            {selectionLabel}
                          </div>
                          <span className="toggle-icon">{isExpanded ? '▾' : '▸'}</span>
                        </button>

                        {isExpanded && (
                          <div className="course-options simple-course-options">
                            {courseSections.length === 0 ? (
                              <p className="no-sections">No sections available for this course.</p>
                            ) : (
                              courseSections.map((section) => {
                                const termLabel = section.year && section.semester ? `${section.year} S${section.semester}` : null
                                const metaParts = [
                                  termLabel,
                                  section.meeting_day != null ? formatMeetingDay(section.meeting_day) : null,
                                  formatTimeRange(section.start_time, section.end_time) || null,
                                  section.location || null
                                ].filter(Boolean)
                                const isChecked = Number(selectedSectionId) === Number(section.id)
                                const isDisabled = selectedSectionId != null && Number(selectedSectionId) !== Number(section.id)

                                return (
                                  <label
                                    key={section.id}
                                    className={`section-option simple-option ${isChecked ? 'selected' : ''} ${isDisabled ? 'disabled' : ''}`}
                                  >
                                    <input
                                      type="checkbox"
                                      name={`course-${course.id}`}
                                      checked={isChecked}
                                      disabled={isDisabled}
                                      onChange={(e) => handleSectionToggle(course.id, section.id, e.target.checked)}
                                    />
                                    <div className="section-details">
                                      <span className="section-code">{section.section_code}</span>
                                      <span className="section-meta">
                                        {metaParts.length > 0 ? metaParts.join(' • ') : 'Details unavailable'}
                                      </span>
                                    </div>
                                  </label>
                                )
                              })
                            )}
                          </div>
                        )}
                      </Fragment>
                    )
                  })}
                </div>
              )}
            </div>

            <div className="modal-actions">
              <button
                type="button"
                onClick={handleSaveEnrollment}
                className="btn btn-primary modal-primary"
                disabled={savingEnrollment}
              >
                {savingEnrollment ? 'Saving...' : 'Save Changes'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Add Student Modal */}
      {showAddStudentModal && (
        <div className="modal-overlay" onClick={closeAddStudentModal}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>Add New Student</h2>
              <button onClick={closeAddStudentModal} className="close-button">
                ✕
              </button>
            </div>

            <div className="add-student-form">
              <form onSubmit={async (e) => {
                e.preventDefault()
                const formData = new FormData(e.target)
                const studentData = {
                  email: formData.get('email'),
                  firstName: formData.get('firstName'),
                  lastName: formData.get('lastName')
                }

                try {
                  const response = await fetch(`${API_BASE_URL}/api/students`, {
                    method: 'POST',
                    headers: {
                      'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(studentData)
                  })

                  const result = await response.json()

                  if (!response.ok) {
                    throw new Error(result.message || 'Failed to add student')
                  }

                  // Refresh students list
                  await fetchStudents()
                  closeAddStudentModal()
                  alert(result.message || 'Student added successfully!')
                } catch (error) {
                  console.error('Error adding student:', error)
                  alert(error.message || 'Failed to add student')
                }
              }}>
                <div className="form-group">
                  <label className="form-label">Email Address</label>
                  <input
                    type="email"
                    name="email"
                    className="form-input"
                    placeholder="student@smu.edu.sg"
                    autoComplete="off"
                    required
                  />
                </div>

  
                <div className="form-group">
                  <label className="form-label">First Name</label>
                  <input
                    type="text"
                    name="firstName"
                    className="form-input"
                    placeholder="John"
                    autoComplete="off"
                    required
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Last Name</label>
                  <input
                    type="text"
                    name="lastName"
                    className="form-input"
                    placeholder="Doe"
                    autoComplete="off"
                    required
                  />
                </div>

                <div className="form-actions">
                  <button type="submit" className="btn btn-primary">
                    Add Student
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

export default Students
