import { useState, useEffect, useRef, useMemo } from 'react'
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
  const [activeTab, setActiveTab] = useState('active') // 'active' or 'archived'
  const [showSessionModal, setShowSessionModal] = useState(false)
  const [editingSession, setEditingSession] = useState(null)
  const [sessionForm, setSessionForm] = useState({
    section_id: '',
    session_date: '',
    scheduled_start_time: '',
    scheduled_end_time: '',
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
  const [scannerMessage, setScannerMessage] = useState('Starting camera...')
  const [scannerError, setScannerError] = useState('')
  const [recognizedCandidate, setRecognizedCandidate] = useState(null)
  const [isScannerActive, setIsScannerActive] = useState(false)

  const videoRef = useRef(null)
  const canvasRef = useRef(null)
  const overlayCanvasRef = useRef(null)
  const scanIntervalRef = useRef(null)
  const isSendingFrameRef = useRef(false)
  const skippedStudentsRef = useRef(new Map())
  const isScanningRef = useRef(false)
  const recognizedCandidateRef = useRef(null)
  const lastResponseIdRef = useRef(0)

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

  // Get unique sections (no duplicates from different years/semesters)
  const uniqueSections = useMemo(() => {
    const seen = new Set()
    return sectionList.filter(section => {
      if (seen.has(section.section_code)) {
        return false
      }
      seen.add(section.section_code)
      return true
    })
  }, [sectionList])

  useEffect(() => {
    fetchInitialData()
  }, [])

  useEffect(() => {
    filterAndSortSessions()
  }, [sessionList, searchTerm, sortBy, sortOrder, sectionFilter, yearFilter, semesterFilter, statusFilterSessions, activeTab])

  useEffect(() => {
    filterStudents()
  }, [students, markingSearchTerm, statusFilterMarking, attendanceRecords])

  useEffect(() => {
    // Sync recognizedCandidate ref with state
    recognizedCandidateRef.current = recognizedCandidate
  }, [recognizedCandidate])
  
  useEffect(() => {
    if (!showScannerModal) {
      stopCamera()
      return
    }
    // Start camera and immediately begin scanning when modal opens
    startCamera()
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

    // Filter by archive status based on activeTab
    if (activeTab === 'archived') {
      filtered = filtered.filter(s => s.status === 'ARCHIVED')
    } else {
      filtered = filtered.filter(s => s.status !== 'ARCHIVED')
    }

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
    setScannerMessage('Starting camera...')
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
    setScannerMessage('Starting camera...')
    setScannerError('')
    setIsScannerActive(false)
  }

  const startCamera = async () => {
    if (!showScannerModal) return
    
    // Ensure camera is running (start if needed)
    if (!videoRef.current?.srcObject || videoRef.current.paused) {
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
      } catch (error) {
        console.error('Unable to access camera:', error)
        setScannerError('Unable to access camera. Please check permissions and try again.')
        setShowScannerModal(false)
        return
      }
    }
    
    setScannerMessage('Scanning faces...')
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
    // Don't send frames if not actively scanning
    if (!isScanningRef.current) {
      return
    }
    
    // Don't send if we already have a candidate to prevent duplicates
    if (recognizedCandidateRef.current) {
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
    const thisRequestId = ++lastResponseIdRef.current

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
      // Only process if this is the most recent request
      if (thisRequestId === lastResponseIdRef.current) {
        handleScanResponse(result)
      }
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
          setScannerMessage('No match found. Please look at the camera.')
          // Don't stop camera - keep scanning
        } else if (msg.toLowerCase().includes('please')) {
          setScannerMessage(msg)
          // Don't stop camera - keep scanning
        } else {
          setScannerMessage(msg)
        }
      }
      
      // Draw overlays for all detected faces
      if (result.allDetections && result.allDetections.length > 0) {
        // Deduplicate by student ID (in case backend sends same person multiple times)
        const uniqueDetections = []
        const seenIds = new Set()
        for (const det of result.allDetections) {
          if (det.student?.id && !seenIds.has(det.student.id)) {
            seenIds.add(det.student.id)
            uniqueDetections.push(det)
          }
        }
        drawMultiOverlay(uniqueDetections)
      } else if (result.student && result.boundingBox) {
        // Fallback to single overlay for backwards compatibility
        drawOverlay(result.boundingBox, result.similarity, result.student)
      } else {
        clearOverlay()
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

    // Don't stop scanning - just show confirmation modal
    // Scanning continues in background
    setScannerError('')
    setScannerMessage('Match found - please confirm')
    setRecognizedCandidate({
      student,
      similarity: result.similarity,
      recommendedStatus: result.recommendedStatus,
      recommendedCheckInTime: result.recommendedCheckInTime,
      message: result.message || ''
    })
    
    // Draw bounding box overlay if available
    if (result.boundingBox) {
      drawOverlay(result.boundingBox, result.similarity, student)
    }
  }

  const drawMultiOverlay = (detections) => {
    if (!overlayCanvasRef.current || !videoRef.current) {
      return
    }
    
    const overlay = overlayCanvasRef.current
    const video = videoRef.current
    const ctx = overlay.getContext('2d')
    
    // Get actual display dimensions from video
    const displayWidth = video.offsetWidth || video.videoWidth || 640
    const displayHeight = video.offsetHeight || video.videoHeight || 480
    overlay.width = displayWidth
    overlay.height = displayHeight
    
    // Setting width/height automatically clears the canvas, but ensure it's cleared
    ctx.clearRect(0, 0, overlay.width, overlay.height)
    
    // Draw each detection
    for (const detection of detections) {
      if (!detection.boundingBox || !detection.student) continue
      
      const bbox = detection.boundingBox
      const student = detection.student
      const similarity = detection.similarity || 0
      
      // Scale bounding box from original to display dimensions
      const scaleX = bbox.originalWidth ? displayWidth / bbox.originalWidth : 1
      const scaleY = bbox.originalHeight ? displayHeight / bbox.originalHeight : 1
      
      const x = bbox.x * scaleX
      const y = bbox.y * scaleY
      const width = bbox.width * scaleX
      const height = bbox.height * scaleY
      
      // Draw bounding box with thicker line
      ctx.strokeStyle = '#00ff00'
      ctx.lineWidth = 4
      ctx.strokeRect(x, y, width, height)
      
      // Prepare name text
      const firstName = student?.firstName || 'Unknown'
      const lastName = student?.lastName || ''
      const nameText = `${firstName} ${lastName}`.trim()
      const matchText = `${Math.round(similarity * 100)}%`
      
      // Draw name label (above) with larger font
      ctx.font = 'bold 16px sans-serif'
      const nameMetrics = ctx.measureText(nameText)
      const nameWidth = nameMetrics.width + 12
      const nameHeight = 24
      
      ctx.fillStyle = 'rgba(0, 255, 0, 0.95)'
      ctx.fillRect(x, y - nameHeight - 2, nameWidth, nameHeight)
      ctx.fillStyle = '#000'
      ctx.fillText(nameText, x + 6, y - 6)
      
      // Draw match label (below) with larger font
      ctx.font = 'bold 14px sans-serif'
      const matchMetrics = ctx.measureText(matchText)
      const matchWidth = matchMetrics.width + 12
      const matchHeight = 22
      
      ctx.fillStyle = 'rgba(0, 255, 0, 0.95)'
      ctx.fillRect(x, y + height + 2, matchWidth, matchHeight)
      ctx.fillStyle = '#000'
      ctx.fillText(matchText, x + 6, y + height + 18)
    }
  }

  const drawOverlay = (bbox, similarity, student) => {
    if (!overlayCanvasRef.current || !videoRef.current) {
      return
    }
    
    const overlay = overlayCanvasRef.current
    const video = videoRef.current
    const ctx = overlay.getContext('2d')
    
    // Get actual display dimensions from video
    const displayWidth = video.offsetWidth || video.videoWidth || 640
    const displayHeight = video.offsetHeight || video.videoHeight || 480
    overlay.width = displayWidth
    overlay.height = displayHeight
    
    // Clear previous drawing
    ctx.clearRect(0, 0, overlay.width, overlay.height)
    
    if (!bbox) {
      return
    }
    
    // Scale bounding box from original to display dimensions
    const scaleX = bbox.originalWidth ? displayWidth / bbox.originalWidth : 1
    const scaleY = bbox.originalHeight ? displayHeight / bbox.originalHeight : 1
    
    const x = bbox.x * scaleX
    const y = bbox.y * scaleY
    const width = bbox.width * scaleX
    const height = bbox.height * scaleY
    
    // Draw bounding box with thicker line
    ctx.strokeStyle = '#00ff00'
    ctx.lineWidth = 4
    ctx.strokeRect(x, y, width, height)
    
    // Prepare name text
    const firstName = student?.firstName || 'Unknown'
    const lastName = student?.lastName || ''
    const nameText = `${firstName} ${lastName}`.trim()
    const matchText = `${Math.round(similarity * 100)}%`
    
    // Draw name label (above) with larger font
    ctx.font = 'bold 16px sans-serif'
    const nameMetrics = ctx.measureText(nameText)
    const nameWidth = nameMetrics.width + 12
    const nameHeight = 24
    
    ctx.fillStyle = 'rgba(0, 255, 0, 0.95)'
    ctx.fillRect(x, y - nameHeight - 2, nameWidth, nameHeight)
    ctx.fillStyle = '#000'
    ctx.fillText(nameText, x + 6, y - 6)
    
    // Draw match label (below) with larger font
    ctx.font = 'bold 14px sans-serif'
    const matchMetrics = ctx.measureText(matchText)
    const matchWidth = matchMetrics.width + 12
    const matchHeight = 22
    
    ctx.fillStyle = 'rgba(0, 255, 0, 0.95)'
    ctx.fillRect(x, y + height + 2, matchWidth, matchHeight)
    ctx.fillStyle = '#000'
    ctx.fillText(matchText, x + 6, y + height + 18)
  }
  
  const clearOverlay = () => {
    if (!overlayCanvasRef.current) {
      return
    }
    const ctx = overlayCanvasRef.current.getContext('2d')
    ctx.clearRect(0, 0, overlayCanvasRef.current.width, overlayCanvasRef.current.height)
  }

  const resetScannerForNextStudent = () => {
    // Reset states without closing modal
    setScannerError('')
    setScannerMessage('Scanning faces...')
    clearOverlay()
    
    // Resume scanning immediately
    isScanningRef.current = true
    setIsScannerActive(true)
    startFrameLoop()
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
      
      // Clear candidate immediately to hide profile card, show success message
      setRecognizedCandidate(null)
      setScannerMessage(`✓ ${student.displayId || student.id} marked as ${status}`)
      setScannerError('')
      
      // Reset for next student after a short delay
      setTimeout(() => {
        resetScannerForNextStudent()
      }, 1500)
    } catch (error) {
      console.error('Failed to record attendance from scanner:', error)
      
      // Clear candidate and show error
      setRecognizedCandidate(null)
      setScannerError('Unable to record attendance. Please try manual check-in.')
      setScannerMessage('Error occurred. Ready to scan next student.')
      recordSkipForStudent(student.id)
      
      // Reset for next student after showing error
      setTimeout(() => {
        resetScannerForNextStudent()
      }, 2000)
    }
  }

  const handleScannerReject = () => {
    if (!recognizedCandidate) return
    const { student } = recognizedCandidate
    recordSkipForStudent(student.id)
    
    // Clear candidate first so scanning can resume
    setRecognizedCandidate(null)
    
    // Immediately reset for next student without delay or message
    resetScannerForNextStudent()
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
      // Status is automatically determined by backend based on date/time
      const requestBody = {
        sectionId: parseInt(sessionForm.section_id),
        sessionDate: sessionForm.session_date,
        scheduledStartTime: sessionForm.scheduled_start_time,
        scheduledEndTime: sessionForm.scheduled_end_time,
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

  const handleReopenSession = async (sessionId, e) => {
    e.stopPropagation()
    if (!confirm('Are you sure you want to reopen this session? This will allow attendance to be edited.')) {
      return
    }

    try {
      const response = await fetch(`${API_BASE_URL}/api/attendance/sessions/${sessionId}/reopen`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' }
      })

      if (!response.ok) throw new Error('Failed to reopen session')

      await fetchSessions()
      alert('Session reopened successfully')
    } catch (error) {
      console.error('Error reopening session:', error)
      alert('Failed to reopen session: ' + error.message)
    }
  }

  const handleArchiveSession = async (sessionId, e) => {
    e.stopPropagation()
    if (!confirm('Are you sure you want to archive this session? This will lock it and prevent any further edits.')) {
      return
    }

    try {
      const response = await fetch(`${API_BASE_URL}/api/attendance/sessions/${sessionId}/archive`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' }
      })

      if (!response.ok) throw new Error('Failed to archive session')

      await fetchSessions()
      alert('Session archived successfully')
    } catch (error) {
      console.error('Error archiving session:', error)
      alert('Failed to archive session: ' + error.message)
    }
  }

  const handleUnarchiveSession = async (sessionId, e) => {
    e.stopPropagation()
    if (!confirm('Are you sure you want to unarchive this session? This will allow attendance to be edited.')) {
      return
    }

    try {
      // Use reopen endpoint to unarchive (changes ARCHIVED -> ACTIVE)
      const response = await fetch(`${API_BASE_URL}/api/attendance/sessions/${sessionId}/reopen`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' }
      })

      if (!response.ok) throw new Error('Failed to unarchive session')

      await fetchSessions()
      alert('Session unarchived successfully')
    } catch (error) {
      console.error('Error unarchiving session:', error)
      alert('Failed to unarchive session: ' + error.message)
    }
  }

  const handleCancelSession = async (sessionId, e) => {
    e.stopPropagation()
    if (!confirm('Are you sure you want to cancel this session?')) {
      return
    }

    try {
      const response = await fetch(`${API_BASE_URL}/api/attendance/sessions/${sessionId}/cancel`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' }
      })

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({ message: 'Failed to cancel session' }))
        throw new Error(errorData.message || errorData.error || 'Failed to cancel session')
      }

      await fetchSessions()
      alert('Session cancelled successfully')
    } catch (error) {
      console.error('Error cancelling session:', error)
      alert('Failed to cancel session: ' + error.message)
    }
  }

  const handleReactivateSession = async (sessionId, e) => {
    e.stopPropagation()
    if (!confirm('Are you sure you want to reactivate this session? This will allow attendance to be marked.')) {
      return
    }

    try {
      // Use reopen endpoint to reactivate (changes CANCELLED -> ACTIVE)
      const response = await fetch(`${API_BASE_URL}/api/attendance/sessions/${sessionId}/reopen`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' }
      })

      if (!response.ok) throw new Error('Failed to reactivate session')

      await fetchSessions()
      alert('Session reactivated successfully')
    } catch (error) {
      console.error('Error reactivating session:', error)
      alert('Failed to reactivate session: ' + error.message)
    }
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

      {/* Tab Navigation */}
      <div className="tabs-container">
        <div className="tabs">
          <button
            className={`tab ${activeTab === 'active' ? 'active' : ''}`}
            onClick={() => setActiveTab('active')}
          >
            Active Sessions
          </button>
          <button
            className={`tab ${activeTab === 'archived' ? 'active' : ''}`}
            onClick={() => setActiveTab('archived')}
          >
            Archived Sessions
          </button>
        </div>
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
                    {uniqueSections.map(section => (
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

                {activeTab === 'active' && (
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
                      <option value="ARCHIVED">Archived</option>
                    </select>
                  </div>
                )}
              </div>
            </div>

            <div className="panel-header">
              <h3>Attendance Sessions</h3>
              {activeTab === 'active' && (
                <button
                  onClick={() => {
                    resetSessionForm()
                    setShowSessionModal(true)
                  }}
                  className="btn btn-primary-small"
                >
                  Create Session
                </button>
              )}
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
                        <div className="action-buttons">
                          {session.status === 'ARCHIVED' ? (
                            <button
                              onClick={(e) => handleUnarchiveSession(session.id, e)}
                              className="btn btn-small btn-action"
                              title="Unarchive session to allow editing"
                            >
                              Unarchive
                            </button>
                          ) : (
                            <>
                              {session.status !== 'CANCELLED' && (
                                <button
                                  onClick={() => openMarkAttendance(session)}
                                  className="btn btn-small btn-action"
                                >
                                  Mark Attendance
                                </button>
                              )}
                              {['COMPLETED', 'CLOSED', 'ENDED'].includes(session.status) && (
                                <button
                                  onClick={(e) => handleArchiveSession(session.id, e)}
                                  className="btn btn-small btn-action"
                                  title="Archive session to lock it"
                                >
                                  Archive
                                </button>
                              )}
                              {session.status === 'CANCELLED' && (
                                <>
                                  <button
                                    onClick={(e) => handleReactivateSession(session.id, e)}
                                    className="btn btn-small btn-action"
                                    title="Reactivate session to allow attendance marking"
                                  >
                                    Reactivate
                                  </button>
                                  <button
                                    onClick={(e) => handleArchiveSession(session.id, e)}
                                    className="btn btn-small btn-action"
                                    title="Archive session to lock it"
                                  >
                                    Archive
                                  </button>
                                </>
                              )}
                              {['SCHEDULED', 'IN_PROGRESS'].includes(session.status) && (
                                <button
                                  onClick={(e) => handleCancelSession(session.id, e)}
                                  className="btn btn-small btn-action"
                                  title="Cancel session"
                                >
                                  Cancel
                                </button>
                              )}
                            </>
                          )}
                        </div>
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

      {/* Camera Modal */}
      {showScannerModal && (
        <div 
          className="modal-overlay" 
          onClick={closeScannerModal}
        >
          <div className="modal-content" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '1200px', width: '95%', maxHeight: '90vh' }}>
            <div className="modal-header">
              <div>
                <h2>Face Recognition</h2>
                <p style={{ color: '#6b7280', margin: '0.5rem 0 0 0', fontSize: '0.875rem' }}>
                  {scannerMessage}
                </p>
              </div>
              <div className="modal-header-actions">
                <button onClick={closeScannerModal} className="close-button">
                  ✕
                </button>
              </div>
            </div>

            <div className="modal-body" style={{ padding: 0 }}>
              {/* Camera View */}
              <div style={{ 
                position: 'relative',
                width: '100%',
                height: 'calc(90vh - 120px)',
                minHeight: '500px',
                background: '#000',
                overflow: 'hidden'
              }}>
                <video ref={videoRef} 
                  className="scanner-video" 
                  autoPlay 
                  playsInline 
                  muted 
                  style={{ 
                    width: '100%', 
                    height: '100%', 
                    display: 'block',
                    objectFit: 'contain'
                  }} 
                />
                <canvas ref={overlayCanvasRef} style={{ 
                  position: 'absolute', 
                  top: 0, 
                  left: 0, 
                  width: '100%', 
                  height: '100%', 
                  pointerEvents: 'none'
                }} />
                <canvas ref={canvasRef} style={{ display: 'none' }} />
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Confirmation Modal - pops up over camera */}
      {recognizedCandidate && showScannerModal && (
        <div 
          className="modal-overlay"
          style={{ 
            position: 'fixed',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            background: 'rgba(0, 0, 0, 0.7)',
            zIndex: 9999,
          }}
          onClick={(e) => {
            e.stopPropagation()
            handleScannerReject()
          }}
        >
          <div
            style={{
              position: 'absolute',
              top: '50%',
              left: '50%',
              transform: 'translate(-50%, -50%)',
              background: '#fff',
              borderRadius: '14px',
              padding: '24px',
              maxWidth: '500px',
              width: '90vw',
              boxShadow: '0 10px 40px rgba(0,0,0,0.3)',
            }}
            onClick={(e) => e.stopPropagation()}
          >
            <h2 style={{ marginTop: 0, marginBottom: '16px', fontSize: '20px', fontWeight: 600 }}>
              Confirm Attendance
            </h2>

            {/* Profile Card */}
            <div style={{
              background: '#fff',
              border: '1px solid #ededed',
              borderRadius: '10px',
              padding: '16px',
              color: '#111',
              fontSize: '0.98em',
              marginBottom: '20px',
            }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 8 }}>
                <span style={{ fontWeight: 600, fontSize: 13 }}>ID:</span>
                <span style={{ fontWeight: 600, fontSize: 13 }}>{recognizedCandidate.student.displayId || recognizedCandidate.student.id}</span>
                <span style={{ marginLeft: 8, fontSize: 12, color: '#222', background: '#f4f4f4', padding: '2px 6px', borderRadius: 4 }}>
                  {Math.round((recognizedCandidate.similarity || 0) * 100)}% match
                </span>
              </div>
              <div style={{ fontSize: '1.2em', fontWeight: 700, marginBottom: 4 }}>
                {recognizedCandidate.student.firstName} {recognizedCandidate.student.lastName}
              </div>
              <div style={{ fontSize: 14, color: '#555', marginBottom: 8 }}>
                {recognizedCandidate.student.email}
              </div>
              <div style={{ fontSize: 13 }}>
                {recognizedCandidate.recommendedStatus && (
                  <div style={{ marginBottom: 4 }}>
                    <strong>Status:</strong> {recognizedCandidate.recommendedStatus}
                  </div>
                )}
                {recognizedCandidate.recommendedCheckInTime && (
                  <div>
                    <strong>Check-in:</strong> {new Date(recognizedCandidate.recommendedCheckInTime).toLocaleTimeString('en-SG', { hour: '2-digit', minute: '2-digit', second: '2-digit' })}
                  </div>
                )}
              </div>
            </div>

            {/* Action Buttons */}
            <div style={{ display: 'flex', gap: 12 }}>
              <button 
                onClick={handleScannerAccept} 
                style={{ 
                  flex: 1, 
                  padding: '12px 0', 
                  fontWeight: 600, 
                  color: '#fff', 
                  background: '#000', 
                  border: '1px solid #000', 
                  borderRadius: 6,
                  cursor: 'pointer',
                  fontSize: '15px'
                }}
              >
                Accept
              </button>
              <button 
                onClick={handleScannerReject} 
                style={{ 
                  flex: 1, 
                  padding: '12px 0', 
                  fontWeight: 600, 
                  color: '#000', 
                  background: '#fff', 
                  border: '1px solid #000', 
                  borderRadius: 6,
                  cursor: 'pointer',
                  fontSize: '15px'
                }}
              >
                Reject
              </button>
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
