import { useState, useEffect, Fragment, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { supabase } from './supabase'
import { useRole } from './role'

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
  const [showEditStudentModal, setShowEditStudentModal] = useState(false)
  const [editStudent, setEditStudent] = useState(null)
  const [showManageEnrollmentModal, setShowManageEnrollmentModal] = useState(false)
  const [manageEnrollmentStudent, setManageEnrollmentStudent] = useState(null)
  const [enrollmentSelections, setEnrollmentSelections] = useState({})
  const [savingEnrollment, setSavingEnrollment] = useState(false)
  const [expandedCourses, setExpandedCourses] = useState({})
  const [faceImages, setFaceImages] = useState([])
  const [editFaceImages, setEditFaceImages] = useState([])
  const [faceProcessingSummary, setFaceProcessingSummary] = useState(null)
  const [loadingStudentDetails, setLoadingStudentDetails] = useState(false)
  const fileInputRef = useRef(null)
  const editFileInputRef = useRef(null)
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
    fetchCourses()
    fetchSections()
  }, [])

  useEffect(() => {
    fetchStudents()
  }, [courseFilter, sectionFilter])

  useEffect(() => {
    filterAndSortStudents()
  }, [students, searchTerm, sortBy, sortOrder])

  useEffect(() => {
    setSectionFilter('all')
  }, [courseFilter])

  useEffect(() => {
    if (!selectedStudent) return
    // Don't overwrite selectedStudent from filteredStudents to preserve full attendance records
    // Only close modal if student was deleted
    const studentExists = filteredStudents.some(
      (student) => student.id === selectedStudent.id
    )

    if (!studentExists) {
      setSelectedStudent(null)
      setShowModal(false)
    }
  }, [filteredStudents, selectedStudent])
  const userRole = useRole();

  const fetchStudents = async () => {
    try {
      // Use summary endpoint with query params for better performance
      let url = `${API_BASE_URL}/api/students?summary=true`
      if (courseFilter !== 'all') {
        url += `&courseId=${courseFilter}`
      }
      if (sectionFilter !== 'all') {
        url += `&sectionId=${sectionFilter}`
      }

      const response = await fetch(url)
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
        faceImages: student.faceImages || [],
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
          confidence_level: record.confidenceLevel,
          is_automatic: record.isAutomatic,
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

    // Backend now handles course/section filtering, so students already have correct stats
    // Just use the stats directly from the backend response

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
      day: 'numeric',
      timeZone: 'Asia/Singapore'
    })
  }

  const formatTimeValue = (value) => {
    if (!value) return '—'
    const date = new Date(value)
    if (Number.isNaN(date.getTime())) return '—'
    return date.toLocaleTimeString('en-US', {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: false,
      timeZone: 'Asia/Singapore'
    })
  }

  const selectedStudentSummary = (() => {
    if (!selectedStudent) return null

    const recordsSource = selectedStudent.attendanceRecords ?? []
    const records = [...recordsSource]
    const recordTimestamp = (record) => {
      const value = record.attendance_sessions?.session_date
      if (!value) return 0
      const date = new Date(value)
      return Number.isNaN(date.getTime()) ? 0 : date.getTime()
    }
    records.sort((a, b) => recordTimestamp(b) - recordTimestamp(a))

    // Use pre-computed stats from backend if available, otherwise calculate from records
    const totalSessions = selectedStudent.totalSessions ?? records.length
    const presentSessions = selectedStudent.presentSessions ?? records.filter((record) => record.status === 'PRESENT' || record.status === 'LATE').length
    const lateSessions = selectedStudent.lateSessions ?? records.filter((record) => record.status === 'LATE').length
    const absentSessions = Math.max(totalSessions - presentSessions, 0)

    const attendanceRate = selectedStudent.attendanceRate ?? (totalSessions > 0 ? Math.round((presentSessions / totalSessions) * 100) : 0)
    // Punctuality should only consider attended sessions (PRESENT + LATE), not absent sessions
    // Formula: (on-time sessions) / (attended sessions) * 100
    // If no present sessions, punctuality is not applicable (show 0% or recalculate to override backend bug)
    const punctualityRate = presentSessions > 0
      ? (selectedStudent.punctualityRate ?? Math.round(((presentSessions - lateSessions) / presentSessions) * 100))
      : 0 // Always 0 when no present sessions, regardless of backend value

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

  const handleStudentClick = async (student) => {
    // Fetch full student data for the modal
    setLoadingStudentDetails(true)
    setShowModal(true)

    try {
      const response = await fetch(`${API_BASE_URL}/api/students/${student.id}`)
      if (!response.ok) {
        throw new Error('Failed to fetch student details')
      }
      const studentData = await response.json()

      // Transform the data
      const transformedStudent = {
        ...studentData,
        id: studentData.id,
        displayId: studentData.displayId,
        first_name: studentData.firstName,
        last_name: studentData.lastName,
        email: studentData.email,
        enabled: studentData.enabled,
        totalSessions: studentData.totalSessions,
        presentSessions: studentData.presentSessions,
        lateSessions: studentData.lateSessions,
        attendanceRate: studentData.attendanceRate,
        punctualityRate: studentData.punctualityRate,
        attendanceRecords: (studentData.attendanceRecords || []).map(record => ({
          ...record,
          user_id: record.userId,
          session_id: record.sessionId,
          status: record.status,
          checkin_time: record.checkinTime,
          checkout_time: record.checkoutTime,
          confidence_level: record.confidenceLevel,
          is_automatic: record.isAutomatic,
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
        enrollments: (studentData.enrollments || []).map(enrollment => ({
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
      }

      setSelectedStudent(transformedStudent)
    } catch (error) {
      console.error('Error fetching student details:', error)
      // Fallback to basic student data
      setSelectedStudent(student)
    } finally {
      setLoadingStudentDetails(false)
    }
  }

  const closeModal = () => {
    setShowModal(false)
    setSelectedStudent(null)
  }

  const resetFaceImageState = () => {
    setFaceImages([])
    if (fileInputRef.current) {
      fileInputRef.current.value = ''
    }
  }

  const closeFaceProcessingSummary = () => {
    setFaceProcessingSummary(null)
  }

  const formatExtractionError = (code) => {
    if (!code) return 'Unknown error'
    const friendly = {
      IMAGE_DECODE_FAILED: 'Image could not be read',
      NO_FACE_DETECTED: 'No face detected',
      MULTIPLE_FACES_DETECTED: 'Multiple faces detected',
      FACE_QUALITY_REJECTED: 'Face failed quality checks',
      MODEL_ERROR: 'Model error',
      UNKNOWN_ERROR: 'Unknown error'
    }
    if (friendly[code]) {
      return friendly[code]
    }
    return code
      .toLowerCase()
      .split('_')
      .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
      .join(' ')
  }

  const formatRejectionNotes = (item) => {
    if (!item || !item.message) {
      return '—'
    }

    if (item.errorCode) {
      const reason = formatExtractionError(item.errorCode)
      const message = item.message.trim()
      if (message.toLowerCase().startsWith(reason.toLowerCase())) {
        const remainder = message.slice(reason.length).trim()
        const cleaned = remainder
          .replace(/^[\s:–-]+/, '')
          .replace(/^(\.)\s*/, '')
          .replace(/^\((.*)\)\.?$/, '$1')
          .trim()
        if (!cleaned) {
          return '—'
        }
        if (cleaned.length >= 2) {
          return cleaned.charAt(0).toUpperCase() + cleaned.slice(1)
        }
        return cleaned.toUpperCase()
      }
    }

    const normalized = item.message.trim()
    if (!normalized) {
      return '—'
    }
    if (normalized.length >= 2) {
      return normalized.charAt(0).toUpperCase() + normalized.slice(1)
    }
    return normalized.toUpperCase()
  }

  const openAddStudentModal = () => {
    resetFaceImageState()
    setShowAddStudentModal(true)
  }

  const closeAddStudentModal = () => {
    setShowAddStudentModal(false)
    resetFaceImageState()
  }

  const openEditStudentModal = async (student) => {
    // Convert backend face images to format expected by UI
    const existingImages = student.faceImages || []
    const formattedImages = existingImages.map((img, index) => ({
      id: `existing-${index}`,
      preview: `data:image/jpeg;base64,${img}`,
      data: img,
      name: `Face ${index + 1}`,
      isExisting: true
    }))

    setEditFaceImages(formattedImages)
    setEditStudent(student)
    setShowEditStudentModal(true)
  }

  const closeEditStudentModal = () => {
    setShowEditStudentModal(false)
    setEditStudent(null)
    setEditFaceImages([])
    if (editFileInputRef.current) {
      editFileInputRef.current.value = ''
    }
  }

  const generateImageId = () =>
    typeof crypto !== 'undefined' && crypto.randomUUID
      ? crypto.randomUUID()
      : `img-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`

  const readFileAsDataUrl = (file) =>
    new Promise((resolve, reject) => {
      const reader = new FileReader()
      reader.onload = () => {
        if (typeof reader.result === 'string') {
          resolve(reader.result)
        } else {
          reject(new Error('Failed to read file'))
        }
      }
      reader.onerror = () => reject(new Error('Failed to read file'))
      reader.readAsDataURL(file)
    })

  const handleUploadButtonClick = () => {
    if (faceImages.length >= 8) {
      alert('You can upload up to 8 images for each student.')
      return
    }
    fileInputRef.current?.click()
  }

  const handleFaceImageSelection = async (event) => {
    const files = Array.from(event.target.files || [])
    if (files.length === 0) {
      return
    }

    const remainingCapacity = 8 - faceImages.length
    if (remainingCapacity <= 0) {
      alert('Maximum of 8 images reached.')
      event.target.value = ''
      return
    }

    const filesToProcess = files.slice(0, remainingCapacity)
    if (files.length > remainingCapacity) {
      alert('Only the first 8 images were added. Please remove some images before uploading more.')
    }

    try {
      const processed = await Promise.all(
        filesToProcess.map(async (file) => {
          const dataUrl = await readFileAsDataUrl(file)
          const base64Data = dataUrl.includes(',') ? dataUrl.split(',')[1] : dataUrl
          return {
            id: generateImageId(),
            preview: dataUrl,
            data: base64Data,
            name: file.name
          }
        })
      )

      setFaceImages((prev) => [...prev, ...processed])
    } catch (error) {
      console.error('Failed to process face images:', error)
      alert('Failed to process one or more images. Please try again with valid image files.')
    } finally {
      event.target.value = ''
    }
  }

  const handleRemoveFaceImage = (imageId) => {
    setFaceImages((prev) => prev.filter((image) => image.id !== imageId))
  }

  const handleEditUploadButtonClick = () => {
    const totalImages = editFaceImages.length
    if (totalImages >= 8) {
      alert('You can upload up to 8 images for each student.')
      return
    }
    editFileInputRef.current?.click()
  }

  const handleEditFaceImageSelection = async (event) => {
    const files = Array.from(event.target.files || [])
    if (files.length === 0) {
      return
    }

    const remainingCapacity = 8 - editFaceImages.length
    if (remainingCapacity <= 0) {
      alert('Maximum of 8 images reached.')
      event.target.value = ''
      return
    }

    const filesToProcess = files.slice(0, remainingCapacity)
    if (files.length > remainingCapacity) {
      alert('Only the first 8 images were added. Please remove some images before uploading more.')
    }

    try {
      const processed = await Promise.all(
        filesToProcess.map(async (file) => {
          const dataUrl = await readFileAsDataUrl(file)
          const base64Data = dataUrl.includes(',') ? dataUrl.split(',')[1] : dataUrl
          return {
            id: generateImageId(),
            preview: dataUrl,
            data: base64Data,
            name: file.name,
            isExisting: false
          }
        })
      )

      setEditFaceImages((prev) => [...prev, ...processed])
    } catch (error) {
      console.error('Failed to process face images:', error)
      alert('Failed to process one or more images. Please try again with valid image files.')
    } finally {
      event.target.value = ''
    }
  }

  const handleRemoveEditFaceImage = (imageId) => {
    setEditFaceImages((prev) => prev.filter((image) => image.id !== imageId))
  }

  const openManageEnrollmentModal = async (student) => {
    try {
      // Fetch enrollments from the database since summary=true doesn't include them
      const { data: enrollments, error } = await supabase
        .from('section_enrollments')
        .select(`
          user_id,
          section_id,
          is_active,
          enrolled_at,
          sections:section_id (
            id,
            section_code,
            year,
            semester,
            meeting_day,
            start_time,
            end_time,
            location,
            course_id,
            courses:course_id (
              id,
              code,
              title
            )
          )
        `)
        .eq('user_id', student.id)
        .eq('is_active', true)

      if (error) {
        console.error('Error fetching enrollments:', error)
        throw error
      }

      // Build initial selections from fetched enrollments
      const initialSelections = {}
      const expanded = {}
        ; (enrollments || []).forEach((enrollment) => {
          const courseId = enrollment.sections?.courses?.id || enrollment.sections?.course_id
          if (courseId) {
            initialSelections[courseId] = enrollment.section_id
          }
        })

      setEnrollmentSelections(initialSelections)
      setExpandedCourses(expanded)
      // Update student with fetched enrollments
      setManageEnrollmentStudent({ ...student, enrollments })
      setShowManageEnrollmentModal(true)
    } catch (error) {
      console.error('Failed to load enrollments:', error)
      alert('Failed to load enrollment data. Please try again.')
    }
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

  const handleDeleteStudent = async (student) => {
    if (!confirm(`Are you sure you want to delete ${student.first_name} ${student.last_name} (${student.id})? This action cannot be undone.`)) {
      return
    }

    try {
      const response = await fetch(`${API_BASE_URL}/api/students/${student.id}`, {
        method: 'DELETE'
      })

      if (!response.ok) {
        const errorData = await response.json()
        throw new Error(errorData.message || 'Failed to delete student')
      }

      await fetchStudents()
      closeModal()
      alert('Student deleted successfully')
    } catch (error) {
      console.error('Error deleting student:', error)
      alert(error.message || 'Failed to delete student')
    }
  }

  const handleEditStudent = async (student) => {
    // Fetch full student data for editing
    try {
      const response = await fetch(`${API_BASE_URL}/api/students/${student.id}`)
      if (!response.ok) {
        throw new Error('Failed to fetch student details')
      }
      const studentData = await response.json()

      // Transform the data for editing
      const transformedStudent = {
        ...studentData,
        first_name: studentData.firstName,
        last_name: studentData.lastName,
        displayId: studentData.displayId,
        faceImages: studentData.faceImages || []
      }

      await openEditStudentModal(transformedStudent)
    } catch (error) {
      console.error('Error fetching student details:', error)
      // Fallback to basic student data
      await openEditStudentModal(student)
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
        <button onClick={openAddStudentModal} className="btn btn-primary-small">
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
              // Backend now provides pre-calculated stats filtered by course/section
              const attendanceRateValue = student.attendanceRate ?? 0
              // Punctuality should be 0% when no present sessions, regardless of backend value
              const presentSessions = student.presentSessions ?? 0
              const punctualityRateValue = presentSessions > 0
                ? (student.punctualityRate ?? 0)
                : 0 // Always 0 when no present sessions
              return (
                <tr key={student.displayId || student.id}>
                  <td className="student-id">{student.displayId || student.id}</td>
                  <td className="student-name">
                    <div>
                      {student.first_name} {student.last_name}
                    </div>
                    <div style={{ fontSize: '0.75rem', color: '#999', marginTop: '0.15rem' }}>
                      {student.email}
                    </div>
                  </td>
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
                  <td>
                    <div className="action-buttons">
                      <button
                        onClick={() => handleStudentClick(student)}
                        className="btn btn-small btn-action"
                      >
                        View
                      </button>
                      <button
                        onClick={() => openManageEnrollmentModal(student)}
                        className="btn btn-small btn-action"
                      >
                        Sections
                      </button>
                      <button
                        onClick={() => handleEditStudent(student)}
                        className="btn btn-small btn-action"
                      >
                        Edit
                      </button>
                      {userRole === "admin" && (
                        <button
                          onClick={() => handleDeleteStudent(student)}
                          className="btn btn-small btn-action"
                        >
                          Delete
                        </button>
                      )}

                    </div>
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
      {showModal && (
        <div className="modal-overlay" onClick={closeModal}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            {loadingStudentDetails ? (
              <div className="loading">Loading student details...</div>
            ) : selectedStudent ? (
              <>
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
              </>
            ) : null}
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

                // Prevent double submission
                const submitButton = e.target.querySelector('button[type="submit"]')
                if (submitButton.disabled) return
                submitButton.disabled = true

                const formData = new FormData(e.target)
                const studentData = {
                  email: formData.get('email'),
                  firstName: formData.get('firstName'),
                  lastName: formData.get('lastName'),
                  faceImages: faceImages.map((image) => image.data)
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

                  await fetchStudents()

                  const summary = result.faceProcessingSummary
                  if (summary && summary.rejectedCount > 0) {
                    const student = result.student || {}
                    const studentName = [student.firstName, student.lastName].filter(Boolean).join(' ')
                    setFaceProcessingSummary({
                      ...summary,
                      context: 'create',
                      message: result.message,
                      studentName
                    })
                  } else {
                    alert(result.message || 'Student added successfully!')
                  }

                  closeAddStudentModal()
                } catch (error) {
                  console.error('Error adding student:', error)
                  alert(error.message || 'Failed to add student')
                } finally {
                  // Re-enable submit button in case of error
                  if (submitButton) submitButton.disabled = false
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

                <div className="form-group face-images-section">
                  <label className="form-label">Face Images</label>
                  <div className="face-images-row">
                    <button
                      type="button"
                      className="btn-secondary-small upload-button"
                      onClick={handleUploadButtonClick}
                    >
                      Upload Image
                    </button>
                    <input
                      type="file"
                      accept="image/*"
                      multiple
                      ref={fileInputRef}
                      onChange={handleFaceImageSelection}
                      style={{ display: 'none' }}
                    />
                    {faceImages.map((image, index) => (
                      <div key={image.id} className="face-image-item">
                        <img src={image.preview} alt={`Face ${index + 1}`} />
                        <button
                          type="button"
                          className="face-image-remove"
                          onClick={() => handleRemoveFaceImage(image.id)}
                          aria-label={`Remove face image ${index + 1}`}
                        >
                          ✕
                        </button>
                      </div>
                    ))}
                  </div>
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

      {/* Edit Student Modal */}
      {showEditStudentModal && editStudent && (
        <div className="modal-overlay" onClick={closeEditStudentModal}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>Edit Student</h2>
              <button onClick={closeEditStudentModal} className="close-button">
                ✕
              </button>
            </div>

            <div className="add-student-form">
              <form onSubmit={async (e) => {
                e.preventDefault()

                // Prevent double submission
                const submitButton = e.target.querySelector('button[type="submit"]')
                if (submitButton.disabled) return
                submitButton.disabled = true

                const formData = new FormData(e.target)
                const faceImagesData = editFaceImages.map((image) => image.data);
                console.log('Updating student with face images:', faceImagesData.length);
                console.log('Face images preview:', faceImagesData.map(img => img.substring(0, 50) + '...'));

                const studentData = {
                  email: formData.get('email'),
                  firstName: formData.get('firstName'),
                  lastName: formData.get('lastName'),
                  faceImages: faceImagesData
                }

                try {
                  const response = await fetch(`${API_BASE_URL}/api/students/${editStudent.id}`, {
                    method: 'PUT',
                    headers: {
                      'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(studentData)
                  })

                  const result = await response.json()

                  if (!response.ok) {
                    throw new Error(result.message || 'Failed to update student')
                  }

                  await fetchStudents()

                  const summary = result.faceProcessingSummary
                  if (summary && summary.rejectedCount > 0) {
                    const studentData = result.student || {}
                    const studentName = [studentData.firstName, studentData.lastName].filter(Boolean).join(' ')
                    setFaceProcessingSummary({
                      ...summary,
                      context: 'update',
                      message: result.message,
                      studentName
                    })
                  } else {
                    alert(result.message || 'Student updated successfully!')
                  }

                  closeEditStudentModal()
                } catch (error) {
                  console.error('Error updating student:', error)
                  alert(error.message || 'Failed to update student')
                } finally {
                  // Re-enable submit button in case of error
                  if (submitButton) submitButton.disabled = false
                }
              }}>
                <div className="form-group">
                  <label className="form-label">Student ID (Read Only)</label>
                  <input
                    type="text"
                    className="form-input"
                    value={editStudent.displayId || editStudent.id}
                    readOnly
                    disabled
                    style={{ background: '#f3f4f6', cursor: 'not-allowed' }}
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Email Address</label>
                  <input
                    type="email"
                    name="email"
                    className="form-input"
                    placeholder="student@smu.edu.sg"
                    autoComplete="off"
                    defaultValue={editStudent.email}
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
                    defaultValue={editStudent.first_name}
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
                    defaultValue={editStudent.last_name}
                    required
                  />
                </div>

                <div className="form-group face-images-section">
                  <label className="form-label">Face Images</label>
                  <div className="face-images-row">
                    <button
                      type="button"
                      className="btn-secondary-small upload-button"
                      onClick={handleEditUploadButtonClick}
                    >
                      Upload Image
                    </button>
                    <input
                      type="file"
                      accept="image/*"
                      multiple
                      ref={editFileInputRef}
                      onChange={handleEditFaceImageSelection}
                      style={{ display: 'none' }}
                    />
                    {editFaceImages.map((image, index) => (
                      <div key={image.id} className="face-image-item">
                        <img src={image.preview} alt={`Face ${index + 1}`} />
                        <button
                          type="button"
                          className="face-image-remove"
                          onClick={() => handleRemoveEditFaceImage(image.id)}
                          aria-label={`Remove face image ${index + 1}`}
                        >
                          ✕
                        </button>
                      </div>
                    ))}
                  </div>
                </div>

                <div className="form-actions">
                  <button type="submit" className="btn btn-primary">
                    Update Student
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}

      {faceProcessingSummary && (
        <div className="modal-overlay" onClick={closeFaceProcessingSummary}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>Face Image Processing Report</h2>
              <button onClick={closeFaceProcessingSummary} className="close-button">
                ✕
              </button>
            </div>

            <div
              className="modal-body"
              style={{
                backgroundColor: '#fff',
                color: '#111',
                borderTop: '1px solid #e5e7eb',
                borderBottom: '1px solid #e5e7eb',
                padding: '1.5rem'
              }}
            >
              <p style={{ marginBottom: '1rem' }}>
                {faceProcessingSummary.message || 'Face image processing completed.'}
              </p>

              <div style={{ marginBottom: '1.25rem', fontWeight: 500 }}>
                Accepted {faceProcessingSummary.acceptedCount} of {faceProcessingSummary.totalUploaded} uploaded image(s).
              </div>

              {faceProcessingSummary.studentName && (
                <div style={{ marginBottom: '1.25rem' }}>
                  Student: <strong>{faceProcessingSummary.studentName}</strong>
                </div>
              )}

              {Array.isArray(faceProcessingSummary.results) && faceProcessingSummary.results.length > 0 && (
                <div>
                  <table
                    style={{
                      width: '100%',
                      borderCollapse: 'separate',
                      borderSpacing: 0,
                      marginBottom: '1.25rem',
                      fontSize: '0.95rem',
                      border: '1px solid #d1d5db',
                      borderRadius: '0.5rem',
                      overflow: 'hidden',
                      backgroundColor: '#fff'
                    }}
                  >
                    <thead>
                      <tr>
                        <th
                          style={{
                            textAlign: 'left',
                            borderBottom: '1px solid #d1d5db',
                            padding: '0.65rem 0.85rem',
                            backgroundColor: '#f9fafb',
                            fontWeight: 600,
                            color: '#111'
                          }}
                        >
                          Image
                        </th>
                        <th
                          style={{
                            textAlign: 'left',
                            borderBottom: '1px solid #d1d5db',
                            padding: '0.65rem 0.85rem',
                            backgroundColor: '#f9fafb',
                            fontWeight: 600,
                            color: '#111'
                          }}
                        >
                          Status
                        </th>
                        <th
                          style={{
                            textAlign: 'left',
                            borderBottom: '1px solid #d1d5db',
                            padding: '0.65rem 0.85rem',
                            backgroundColor: '#f9fafb',
                            fontWeight: 600,
                            color: '#111'
                          }}
                        >
                          Reason
                        </th>
                        <th
                          style={{
                            textAlign: 'left',
                            borderBottom: '1px solid #d1d5db',
                            padding: '0.65rem 0.85rem',
                            backgroundColor: '#f9fafb',
                            fontWeight: 600,
                            color: '#111'
                          }}
                        >
                          Notes
                        </th>
                      </tr>
                    </thead>
                    <tbody>
                      {faceProcessingSummary.results.map((item) => (
                        <tr key={item.index} style={{ backgroundColor: item.accepted ? '#fff' : '#f9fafb' }}>
                          <td style={{ padding: '0.75rem 0.85rem', borderBottom: '1px solid #f3f4f6' }}>
                            Image #{item.index + 1}
                          </td>
                          <td style={{ padding: '0.75rem 0.85rem', borderBottom: '1px solid #f3f4f6', fontWeight: 600 }}>
                            {item.accepted ? 'Accepted' : 'Rejected'}
                          </td>
                          <td style={{ padding: '0.75rem 0.85rem', borderBottom: '1px solid #f3f4f6' }}>
                            {!item.accepted && item.errorCode ? formatExtractionError(item.errorCode) : '—'}
                          </td>
                          <td style={{ padding: '0.75rem 0.85rem', borderBottom: '1px solid #f3f4f6' }}>
                            {!item.accepted ? formatRejectionNotes(item) : '—'}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>

            <div className="modal-actions" style={{ justifyContent: 'center', padding: '1rem 0' }}>
              <button
                type="button"
                onClick={closeFaceProcessingSummary}
                className="btn btn-secondary"
                style={{ minWidth: '140px' }}
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

export default Students
