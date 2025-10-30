import { useState, useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { supabase } from './supabase'

const API_BASE_URL = 'http://localhost:8080'
const SCAN_SKIP_DURATION_MS = 8000

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

  // Scanner State
  const [showScannerModal, setShowScannerModal] = useState(false)
  const [scannerMessage, setScannerMessage] = useState('Click Start to begin scanning.')
  const [scannerError, setScannerError] = useState('')
  const [recognizedCandidate, setRecognizedCandidate] = useState(null)
  const [isScannerActive, setIsScannerActive] = useState(false)

  const videoRef = useRef(null)
  const canvasRef = useRef(null)
  const scanIntervalRef = useRef(null)
  const isSendingFrameRef = useRef(false)
  const skippedStudentsRef = useRef(new Map())
  const isScanningRef = useRef(false)

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

  useEffect(() => {
    if (!showScannerModal) {
      stopCamera()
      return
    }
    // Always show live preview when modal opens (no scanning yet)
    startPreview()
    return () => {
      stopCamera()
    }
  }, [showScannerModal])

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

  const openScannerModal = () => {
    if (!selectedSession) return
    setScannerError('')
    setScannerMessage('Click Start to begin scanning.')
    setRecognizedCandidate(null)
    setShowScannerModal(true)
    setIsScannerActive(false)
  }

  const closeScannerModal = () => {
    // Stop camera completely when closing modal
    if (videoRef.current && videoRef.current.srcObject) {
      const tracks = videoRef.current.srcObject.getTracks()
      tracks.forEach(track => track.stop())
      videoRef.current.srcObject = null
    }
    setShowScannerModal(false)
    setRecognizedCandidate(null)
    setScannerMessage('Click Start to begin scanning.')
    setScannerError('')
    setIsScannerActive(false)
  }

  const startPreview = async () => {
    if (!showScannerModal) return
    if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
      setScannerError('Camera access is not supported in this browser.')
      setShowScannerModal(false)
      return
    }
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'user', width: { ideal: 640 }, height: { ideal: 480 }, frameRate: { ideal: 20 } } })
      if (videoRef.current) {
        videoRef.current.srcObject = stream
        await videoRef.current.play()
      }
      setScannerMessage('Click Start to begin scanning.')
    } catch (error) {
      console.error('Unable to access camera:', error)
      setScannerError('Unable to access camera. Please check permissions and try again.')
      setShowScannerModal(false)
    }
  }

  const startCamera = async () => {
    if (!showScannerModal) return
    
    // Ensure camera is running (restart if needed)
    if (!videoRef.current?.srcObject || videoRef.current.paused) {
      await startPreview()
    }
    
    setScannerMessage('Scanning faces... Please look at the camera.')
    setIsScannerActive(true)
    isScanningRef.current = true
    startFrameLoop()
  }

  const stopCamera = () => {
    stopFrameLoop()
    isSendingFrameRef.current = false
    isScanningRef.current = false
    setIsScannerActive(false)
    // Don't stop the video stream - keep preview running
    // Only stop the scanning loop
  }

  const startFrameLoop = () => {
    if (!showScannerModal || !selectedSession) {
      return
    }
    stopFrameLoop()
    scanIntervalRef.current = setInterval(captureAndSendFrame, 62)
  }

  const stopFrameLoop = () => {
    if (scanIntervalRef.current) {
      clearInterval(scanIntervalRef.current)
      scanIntervalRef.current = null
    }
  }

  const shouldSkipStudent = (studentId) => {
    if (!studentId) return false
    const lastSeen = skippedStudentsRef.current.get(studentId)
    if (!lastSeen) return false
    if (Date.now() - lastSeen > SCAN_SKIP_DURATION_MS) {
      skippedStudentsRef.current.delete(studentId)
      return false
    }
    return true
  }

  const recordSkipForStudent = (studentId) => {
    if (!studentId) return
    skippedStudentsRef.current.set(studentId, Date.now())
  }

  const captureAndSendFrame = async () => {
    // Don't send frames if not actively scanning or if we already have a match
    if (!isScanningRef.current || recognizedCandidate) {
      return
    }
    
    if (!selectedSession || !videoRef.current || !canvasRef.current) {
      return
    }

    if (isSendingFrameRef.current) {
      return
    }

    const video = videoRef.current
    if (video.readyState < 2) {
      return
    }

    const canvas = canvasRef.current
    const context = canvas.getContext('2d')
    const srcW = video.videoWidth || 640
    const srcH = video.videoHeight || 480
    const targetW = Math.min(480, srcW)
    const scale = targetW / srcW
    const targetH = Math.round(srcH * scale)
    canvas.width = targetW
    canvas.height = targetH
    context.drawImage(video, 0, 0, targetW, targetH)

    const imageData = canvas.toDataURL('image/jpeg', 0.5)
    isSendingFrameRef.current = true

    try {
      const response = await fetch(`${API_BASE_URL}/api/attendance/sessions/${selectedSession.id}/scan`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ imageData })
      })

      if (!response.ok) {
        throw new Error('Scanner request failed')
      }

      const result = await response.json()
      handleScanResponse(result)
    } catch (error) {
      console.error('Face scan error:', error)
      setScannerError('Unable to process the camera feed. Please adjust the camera and try again.')
    } finally {
      isSendingFrameRef.current = false
    }
  }

  const handleScanResponse = (result) => {
    if (!result) {
      return
    }

    setScannerError('')

    if (result.alreadyMarked) {
      setScannerMessage(result.message || 'Student already marked present or late.')
      if (result.student?.id) {
        recordSkipForStudent(result.student.id)
      }
      return
    }

    if (!result.matched) {
      if (result.message) {
        // Show backend messages but keep it friendly
        const msg = result.message
        if (msg.toLowerCase().includes('scanning')) {
          setScannerMessage('Scanning... Keep your face in view.')
        } else if (msg.toLowerCase().includes('no valid frames') || msg.toLowerCase().includes('no clear match')) {
          setScannerMessage('No match found. Click Start to try again.')
          stopCamera()
        } else if (msg.toLowerCase().includes('please')) {
          setScannerMessage(msg)
          stopCamera()
        } else {
          setScannerMessage(msg)
        }
      }
      return
    }

    const student = result.student
    if (!student) {
      return
    }

    if (shouldSkipStudent(student.id)) {
      return
    }

    // Stop scanning completely when match found
    stopFrameLoop()
    isScanningRef.current = false
    setIsScannerActive(false)
    setScannerError('')
    setScannerMessage('Match found. Confirm the student details below.')
    setRecognizedCandidate({
      student,
      similarity: result.similarity,
      recommendedStatus: result.recommendedStatus,
      recommendedCheckInTime: result.recommendedCheckInTime,
      message: result.message || ''
    })
  }

  const handleScannerAccept = async () => {
    if (!recognizedCandidate) return

    const { student, recommendedStatus, recommendedCheckInTime } = recognizedCandidate
    const status = recommendedStatus || calculateStatus(new Date(), selectedSession.session_date, selectedSession.scheduled_start_time)
    const checkInTime = recommendedCheckInTime || new Date().toISOString()

    try {
      setScannerMessage(`Recording attendance for ${student.displayId || student.id}...`)
      await markAttendance(student.id, status, '', checkInTime)
      recordSkipForStudent(student.id)
      setScannerMessage(`✓ ${student.displayId || student.id} marked as ${status}. Click Start to scan next student.`)
      setRecognizedCandidate(null)
      setScannerError('')
    } catch (error) {
      console.error('Failed to record attendance from scanner:', error)
      setScannerError('Unable to record attendance. Please try manual check-in.')
      setScannerMessage('Click Start to try again.')
      recordSkipForStudent(student.id)
      setRecognizedCandidate(null)
    } finally {
      // Stop scanning completely after accept/reject
      stopCamera()
      setIsScannerActive(false)
    }
  }

  const handleScannerReject = () => {
    if (!recognizedCandidate) return
    const { student } = recognizedCandidate
    recordSkipForStudent(student.id)
    setRecognizedCandidate(null)
    setScannerMessage('✗ Match rejected. Click Start to scan again.')
    setScannerError('')
    stopCamera()
    setIsScannerActive(false)
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
      section_code: section.sectionCode, // Map camelCase to snake_case
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
    setFilteredStudents([])  // Clear to show loading state
    setShowMarkingModal(true)  // Show modal immediately
    fetchStudentsAndRecords(session.id, session.section_id)  // Load data in background (no await)
  }

  const closeMarkingModal = () => {
    setShowMarkingModal(false)
    setShowScannerModal(false)
    setSelectedSession(null)
    setStudents([])
    setFilteredStudents([])
    setAttendanceRecords({})
    setMarkingSearchTerm('')
    setStatusFilterMarking('all')
  }

  const fetchStudentsAndRecords = async (sessionId, sectionId) => {
    try {
      // Fetch students enrolled in this section - using optimized endpoint
      const studentsResponse = await fetch(`${API_BASE_URL}/api/sections/${sectionId}/students`)
      if (!studentsResponse.ok) throw new Error('Failed to fetch students')
      
      const students = await studentsResponse.json()
      
      // Map to frontend format
      const studentList = students.map(student => ({
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

      // Just update the single record in state instead of refetching everything
      setAttendanceRecords(prev => ({
        ...prev,
        [userId]: {
          id: savedRecord.id,
          user_id: savedRecord.userId,
          session_id: savedRecord.sessionId,
          status: savedRecord.status,
          checkin_time: savedRecord.checkinTime,
          checkout_time: savedRecord.checkoutTime,
          notes: savedRecord.notes
        }
      }))
    } catch (error) {
      console.error('Error marking attendance:', error)
      alert('Failed to mark attendance: ' + error.message)
    }
  }

  const calculateStatus = (checkinDateTime, sessionDate, scheduledStartTime) => {
    if (!checkinDateTime || !sessionDate || !scheduledStartTime) return 'PRESENT'

    // Parse/check in SGT
    const checkin = new Date(new Date(checkinDateTime).toLocaleString('en-US', { timeZone: 'Asia/Singapore' }))
    const sessionDateTime = new Date(`${sessionDate}T${scheduledStartTime}+08:00`)

    // If the date (SGT) of checkin does NOT match sessionDate, always return present
    const pad2 = (x) => x.toString().padStart(2, '0')
    const checkinYMD = `${checkin.getFullYear()}-${pad2(checkin.getMonth()+1)}-${pad2(checkin.getDate())}`
    if (checkinYMD !== sessionDate) return 'PRESENT'

    // If sessionDate matches, check for late
    const diffMinutes = (checkin - sessionDateTime) / 1000 / 60
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
                        {section.section_code}
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
              <div className="modal-header-actions">
                <button
                  onClick={(e) => {
                    e.stopPropagation()
                    openScannerModal()
                  }}
                  className="btn btn-small btn-action"
                >
                  Record
                </button>
                <button onClick={closeMarkingModal} className="close-button">
                  ✕
                </button>
              </div>
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
                  {filteredStudents.length === 0 ? (
                    <tr>
                      <td colSpan="6" style={{ textAlign: 'center', padding: '2rem', color: '#9ca3af' }}>
                        Loading students...
                      </td>
                    </tr>
                  ) : (
                    filteredStudents.map(student => {
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
                    })
                  )}
                </tbody>
              </table>
              </div>
            </div>
          </div>
        </div>
      )}

      {showScannerModal && (
        <div className="modal-overlay" onClick={closeScannerModal}>
          <div
            className="modal-content scanner-modal"
            onClick={(e) => e.stopPropagation()}
            style={{
              maxWidth: 820,
              minWidth: 350,
              width: '96vw',
              padding: 28,
              borderRadius: 14,
              background: '#ffffff',
              boxShadow: '0 10px 40px rgba(0,0,0,0.22), 0 1px 3px rgba(0,0,0,0.25)',
              display: 'flex',
              flexDirection: window.innerWidth < 650 ? 'column' : 'row',
              alignItems: 'stretch',
              gap: 24,
            }}
          >
            {/* Camera Section - minimal, black background, no heavy border */}
            <div style={{
              flex: '1 1 330px', maxWidth: 380, minWidth: 240,
              display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center',
            }}>
        <div style={{ background: '#111', borderRadius: 10, padding: 6, border: '1px solid #1f1f1f' }}>
          <video ref={videoRef} className="scanner-video" autoPlay playsInline muted style={{ width: 340, height: 255, background: '#000', borderRadius: 8, objectFit: 'cover', boxShadow: '0 2px 10px rgba(0,0,0,0.25)' }} />
                <canvas ref={canvasRef} style={{ display: 'none' }} />
              </div>
        {!isScannerActive && !recognizedCandidate && (
          <button
            style={{
              marginTop: 14,
              fontWeight: 700,
              fontSize: '0.98em',
              padding: '10px 28px',
              background: '#000',
              color: '#fff',
              border: '1px solid #000',
              borderRadius: 6,
              letterSpacing: '0.02em',
              cursor: 'pointer',
            }}
            className="btn"
            onClick={startCamera}
          >
            Start
          </button>
        )}
            </div>

            {/* Subtle Divider */}
            <div style={{
              width: 1, background: '#e9e9e9', alignSelf: 'stretch', display: window.innerWidth < 650 ? 'none' : 'block'
            }} />

            {/* Details Section - black & white only */}
            <div style={{ flex: '1 1 260px', display: 'flex', flexDirection: 'column', minWidth: 240 }}>
              {/* Status */}
              <div className="scanner-status" style={{ marginBottom: recognizedCandidate ? 8 : 16, fontWeight: 500, minHeight: 28, color: '#111' }}>
                <p style={{ margin: 0 }}>{scannerMessage}</p>
                {scannerError && <p className="scanner-error" style={{ color: '#c00', margin: '6px 0 0 0' }}>{scannerError}</p>}
              </div>

              {/* Profile Card (compact) */}
              {recognizedCandidate && (
                <div style={{
                  background: '#fff',
                  border: '1px solid #ededed',
                  borderRadius: 10,
                  padding: '10px 12px',
                  color: '#111',
                  fontSize: '0.98em',
                }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 4 }}>
                    <span style={{ fontWeight: 600, fontSize: 13 }}>ID:</span>
                    <span style={{ fontWeight: 600, fontSize: 13 }}>{recognizedCandidate.student.displayId || recognizedCandidate.student.id}</span>
                    <span style={{ marginLeft: 8, fontSize: 12, color: '#222', background: '#f4f4f4', padding: '2px 6px', borderRadius: 4 }}>
                      {Math.round((recognizedCandidate.similarity || 0) * 100)}% match
                    </span>
                  </div>
                  <div style={{ fontSize: '1.08em', fontWeight: 700, marginBottom: 2 }}>{recognizedCandidate.student.firstName} {recognizedCandidate.student.lastName}</div>
                  <div style={{ fontSize: 13, color: '#555', marginBottom: 2 }}>{recognizedCandidate.student.email}</div>
                  <div style={{ fontSize: 13 }}>
                    {recognizedCandidate.recommendedStatus && (
                      <span><strong>Status:</strong> {recognizedCandidate.recommendedStatus} </span>
                    )}
                    {recognizedCandidate.recommendedCheckInTime && (
                      <span style={{ marginLeft: 6 }}><strong>Check-in:</strong> {new Date(recognizedCandidate.recommendedCheckInTime).toLocaleTimeString('en-SG', { hour: '2-digit', minute: '2-digit', second: '2-digit' })}</span>
                    )}
                  </div>
                </div>
              )}

              {/* Action Buttons OUTSIDE the profile card */}
              {recognizedCandidate && (
                <div style={{ display: 'flex', gap: 10, marginTop: 12 }}>
                  <button onClick={handleScannerAccept} style={{ flex: 1, padding: '12px 0', fontWeight: 600, color: '#fff', background: '#000', border: '1px solid #000', borderRadius: 6 }}>Accept</button>
                  <button onClick={handleScannerReject} style={{ flex: 1, padding: '12px 0', fontWeight: 600, color: '#000', background: '#fff', border: '1px solid #000', borderRadius: 6 }}>Reject</button>
                </div>
              )}
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
