import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { supabase } from './supabase'

function Classes() {
  const navigate = useNavigate()
  const [activeTab, setActiveTab] = useState('courses')
  const [loading, setLoading] = useState(true)

  // Courses State
  const [courseList, setCourseList] = useState([])
  const [filteredCourses, setFilteredCourses] = useState([])
  const [instructors, setInstructors] = useState([])
  const [showCourseModal, setShowCourseModal] = useState(false)
  const [editingCourse, setEditingCourse] = useState(null)
  const [courseForm, setCourseForm] = useState({
    code: '',
    title: '',
    description: ''
  })

  // Sections State
  const [sectionList, setSectionList] = useState([])
  const [filteredSections, setFilteredSections] = useState([])
  const [showSectionModal, setShowSectionModal] = useState(false)
  const [editingSection, setEditingSection] = useState(null)
  const [sectionForm, setSectionForm] = useState({
    course_id: '',
    section_code: '',
    // Detailed schedule fields
    day_of_week: '',
    start_time: '',
    end_time: '',
    // Legacy schedule field for backward compatibility
    schedule: '',
    location: ''
  })

  // Search and Filter State
  const [searchTerm, setSearchTerm] = useState('')
  const [sortBy, setSortBy] = useState('default')
  const [sortOrder, setSortOrder] = useState('asc')
  const [courseFilter, setCourseFilter] = useState('all')
  const [dayFilter, setDayFilter] = useState('all')

  useEffect(() => {
    fetchInitialData()
  }, [])

  useEffect(() => {
    filterAndSortData()
  }, [courseList, sectionList, searchTerm, sortBy, sortOrder, courseFilter, dayFilter, activeTab])

  const fetchInitialData = async () => {
    try {
      await Promise.all([
        fetchCourses(),
        fetchInstructors(),
        fetchSections()
      ])
    } catch (error) {
      console.error('Error fetching initial data:', error)
    } finally {
      setLoading(false)
    }
  }

  const filterAndSortData = () => {
    if (activeTab === 'courses') {
      let filtered = [...courseList]

      // Apply search filter
      if (searchTerm) {
        filtered = filtered.filter(course =>
          course.code.toLowerCase().includes(searchTerm.toLowerCase()) ||
          course.title.toLowerCase().includes(searchTerm.toLowerCase()) ||
          course.description?.toLowerCase().includes(searchTerm.toLowerCase())
        )
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

      setFilteredCourses(filtered)
    } else if (activeTab === 'sections') {
      let filtered = [...sectionList]

      // Apply search filter
      if (searchTerm) {
        filtered = filtered.filter(section =>
          section.section_code.toLowerCase().includes(searchTerm.toLowerCase()) ||
          section.schedule?.toLowerCase().includes(searchTerm.toLowerCase()) ||
          section.location?.toLowerCase().includes(searchTerm.toLowerCase()) ||
          section.courses?.code?.toLowerCase().includes(searchTerm.toLowerCase()) ||
          section.courses?.title?.toLowerCase().includes(searchTerm.toLowerCase())
        )
      }

      // Apply course filter
      if (courseFilter !== 'all') {
        filtered = filtered.filter(section =>
          section.course_id === parseInt(courseFilter)
        )
      }

      // Apply day filter
      if (dayFilter !== 'all') {
        filtered = filtered.filter(section => {
          // Check both detailed schedule fields and legacy schedule
          if (section.day_of_week) {
            return section.day_of_week === dayFilter
          } else if (section.schedule) {
            // Extract day from legacy schedule format
            const dayMatch = section.schedule.match(/^(\w+)/)
            return dayMatch ? dayMatch[1] === dayFilter : false
          }
          return false
        })
      }

      // Apply sorting
      if (sortBy !== 'default') {
        filtered.sort((a, b) => {
          let aVal, bVal

          // Handle special case for course sorting
          if (sortBy === 'course_id') {
            aVal = `${a.courses?.code || ''} - ${a.courses?.title || ''}`
            bVal = `${b.courses?.code || ''} - ${b.courses?.title || ''}`
          } else {
            aVal = a[sortBy]
            bVal = b[sortBy]
          }

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

      setFilteredSections(filtered)
    }
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

  const fetchCourses = async () => {
    const { data, error } = await supabase
      .from('courses')
      .select('*')

    if (error) throw error
    setCourseList(data || [])
    setFilteredCourses(data || [])
  }

  
  // Courses Functions
  const fetchInstructors = async () => {
    const { data, error } = await supabase
      .from('users')
      .select('*')
      .eq('is_instructor', true)

    if (error) throw error
    setInstructors(data || [])
  }

  const saveCourse = async () => {
    try {
      // Only include fields that exist in the database schema
      const courseData = {
        code: courseForm.code,
        title: courseForm.title,
        description: courseForm.description
      }

      if (editingCourse) {
        const { error } = await supabase
          .from('courses')
          .update(courseData)
          .eq('id', editingCourse.id)
        if (error) throw error
      } else {
        const { error } = await supabase
          .from('courses')
          .insert(courseData)
        if (error) throw error
      }

      await fetchCourses()
      setShowCourseModal(false)
      setEditingCourse(null)
      resetCourseForm()
    } catch (error) {
      console.error('Error saving course:', error)
      alert('Failed to save course')
    }
  }

  const deleteCourse = async (courseId) => {
    if (!confirm('Are you sure you want to delete this course?')) return

    try {
      const { error } = await supabase
        .from('courses')
        .delete()
        .eq('id', courseId)

      if (error) throw error

      await fetchCourses()
    } catch (error) {
      console.error('Error deleting course:', error)
      alert('Failed to delete course')
    }
  }

  const editCourse = (course) => {
    setEditingCourse(course)
    setCourseForm({
      code: course.code,
      title: course.title,
      description: course.description || ''
    })
    setShowCourseModal(true)
  }

  const resetCourseForm = () => {
    setCourseForm({
      code: '',
      title: '',
      description: ''
    })
  }

  // Sections Functions
  const fetchSections = async () => {
    const { data, error } = await supabase
      .from('sections')
      .select(`
        *,
        courses(*)
      `)

    if (error) throw error
    setSectionList(data || [])
    setFilteredSections(data || [])
  }

  const saveSection = async () => {
    try {
      // Generate formatted schedule string from detailed inputs
      const dayAbbr = sectionForm.day_of_week ? sectionForm.day_of_week.substring(0, 3) : ''
      const formattedSchedule = `${dayAbbr} ${sectionForm.start_time}-${sectionForm.end_time}`

      // Prepare section data for database
      const sectionData = {
        course_id: sectionForm.course_id,
        section_code: sectionForm.section_code,
        schedule: formattedSchedule, // Legacy format for backward compatibility
        // New detailed fields (if they exist in database)
        day_of_week: sectionForm.day_of_week,
        start_time: sectionForm.start_time,
        end_time: sectionForm.end_time,
        location: sectionForm.location
      }

      // Only include new fields if they exist in database schema
      const dbData = {
        course_id: sectionData.course_id,
        section_code: sectionData.section_code,
        schedule: sectionData.schedule,
        location: sectionData.location
      }

      if (editingSection) {
        const { error } = await supabase
          .from('sections')
          .update(dbData)
          .eq('id', editingSection.id)
        if (error) throw error
      } else {
        const { error } = await supabase
          .from('sections')
          .insert(dbData)
        if (error) throw error
      }

      await fetchSections()
      setShowSectionModal(false)
      setEditingSection(null)
      resetSectionForm()
    } catch (error) {
      console.error('Error saving section:', error)
      alert('Failed to save section')
    }
  }

  const deleteSection = async (sectionId) => {
    if (!confirm('Are you sure you want to delete this section?')) return

    try {
      const { error } = await supabase
        .from('sections')
        .delete()
        .eq('id', sectionId)

      if (error) throw error

      await fetchSections()
    } catch (error) {
      console.error('Error deleting section:', error)
      alert('Failed to delete section')
    }
  }

  const editSection = (section) => {
    setEditingSection(section)

    // Parse existing schedule to extract day and times
    let dayOfWeek = section.day_of_week || ''
    let startTime = section.start_time || ''
    let endTime = section.end_time || ''

    // If new fields don't exist, try to parse from legacy schedule
    if (!dayOfWeek && section.schedule) {
      const scheduleParts = section.schedule.split(' ')
      if (scheduleParts.length >= 2) {
        dayOfWeek = scheduleParts[0] // e.g., "Mon"
        const timeRange = scheduleParts[1] // e.g., "10:00-11:30"
        const timeParts = timeRange.split('-')
        if (timeParts.length === 2) {
          startTime = timeParts[0]
          endTime = timeParts[1]
        }
      }
    }

    setSectionForm({
      course_id: section.course_id || '',
      section_code: section.section_code || '',
      day_of_week: dayOfWeek,
      start_time: startTime,
      end_time: endTime,
      schedule: section.schedule || '',
      location: section.location || ''
    })
    setShowSectionModal(true)
  }

  const resetSectionForm = () => {
    setSectionForm({
      course_id: '',
      section_code: '',
      day_of_week: '',
      start_time: '',
      end_time: '',
      schedule: '',
      location: ''
    })
  }

  if (loading) {
    return <div className="loading">Loading classes management...</div>
  }

  return (
    <div className="container">
      <div className="page-header">
        <h1>Classes Management</h1>
        <button onClick={() => navigate('/dashboard')} className="btn btn-secondary-small">
          Back to Dashboard
        </button>
      </div>

      {/* Tab Navigation */}
      <div className="tabs-container">
        <div className="tabs">
          <button
            className={`tab ${activeTab === 'courses' ? 'active' : ''}`}
            onClick={() => setActiveTab('courses')}
          >
            Courses
          </button>
          <button
            className={`tab ${activeTab === 'sections' ? 'active' : ''}`}
            onClick={() => setActiveTab('sections')}
          >
            Sections
          </button>
        </div>
      </div>

      {/* Tab Content */}
      <div className="tab-content">
        
        {/* Courses Tab */}
        {activeTab === 'courses' && (
          <div className="tab-panel">
            {/* Search and Filter Component */}
            <div className="filters-section">
              <div className="filters-header">
                <h3>Search & Filter</h3>
              </div>
              <div className="search-container">
                <div className="search-input-wrapper">
                  <input
                    type="text"
                    placeholder="Search by code, title, or description..."
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    className="search-input"
                  />
                  <div className="search-icon">⚲</div>
                </div>
              </div>
              </div>

            <div className="panel-header">
              <h3>Manage Courses</h3>
              <button
                onClick={() => {
                  resetCourseForm()
                  setShowCourseModal(true)
                }}
                className="btn btn-primary-small"
              >
                Add Course
              </button>
            </div>

            <div className="table-container">
              <table className="data-table">
                <thead>
                  <tr>
                    <th
                      onClick={() => handleSort('code')}
                      className="sortable"
                    >
                      Code {getSortIcon('code')}
                    </th>
                    <th
                      onClick={() => handleSort('title')}
                      className="sortable"
                    >
                      Title {getSortIcon('title')}
                    </th>
                    <th
                      onClick={() => handleSort('description')}
                      className="sortable"
                    >
                      Description {getSortIcon('description')}
                    </th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredCourses.map(course => (
                    <tr key={course.id}>
                      <td>{course.code}</td>
                      <td>{course.title}</td>
                      <td>{course.description}</td>
                      <td>
                        <div className="action-buttons">
                          <button
                            onClick={() => editCourse(course)}
                            className="btn btn-small btn-action"
                          >
                            Edit
                          </button>
                          <button
                            onClick={() => deleteCourse(course.id)}
                            className="btn btn-small btn-action"
                          >
                            Delete
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* Sections Tab */}
        {activeTab === 'sections' && (
          <div className="tab-panel">
            {/* Search and Filter Component */}
            <div className="filters-section">
              <div className="filters-header">
                <h3>Search & Filter</h3>
              </div>
              <div className="search-filter-bar">
                <div className="search-block">
                  <label className="filter-label" htmlFor="sections-search">Search</label>
                  <div className="search-input-wrapper">
                    <input
                      id="sections-search"
                      type="text"
                      placeholder="Search by section code, course, schedule, or location..."
                      value={searchTerm}
                      onChange={(e) => setSearchTerm(e.target.value)}
                      className="search-input"
                    />
                    <span className="search-icon">⚲</span>
                  </div>
                </div>

                <div className="filter-block">
                  <label className="filter-label" htmlFor="sections-course-filter">Course</label>
                  <select
                    id="sections-course-filter"
                    value={courseFilter}
                    onChange={(e) => setCourseFilter(e.target.value)}
                    className="filter-select"
                  >
                    <option value="all">All Courses</option>
                    {courseList.map(course => (
                      <option key={course.id} value={course.id}>
                        {course.code} - {course.title}
                      </option>
                    ))}
                  </select>
                </div>

                <div className="filter-block">
                  <label className="filter-label" htmlFor="sections-day-filter">Day</label>
                  <select
                    id="sections-day-filter"
                    value={dayFilter}
                    onChange={(e) => setDayFilter(e.target.value)}
                    className="filter-select"
                  >
                    <option value="all">All Days</option>
                    <option value="Monday">Monday</option>
                    <option value="Tuesday">Tuesday</option>
                    <option value="Wednesday">Wednesday</option>
                    <option value="Thursday">Thursday</option>
                    <option value="Friday">Friday</option>
                    <option value="Saturday">Saturday</option>
                    <option value="Sunday">Sunday</option>
                  </select>
                </div>
              </div>
            </div>

            <div className="panel-header">
              <h3>Manage Sections</h3>
              <button
                onClick={() => {
                  resetSectionForm()
                  setShowSectionModal(true)
                }}
                className="btn btn-primary-small"
              >
                Add Section
              </button>
            </div>

            <div className="table-container">
              <table className="data-table">
                <thead>
                  <tr>
                    <th
                      onClick={() => handleSort('section_code')}
                      className="sortable"
                    >
                      Section Code {getSortIcon('section_code')}
                    </th>
                    <th
                      onClick={() => handleSort('course_id')}
                      className="sortable"
                    >
                      Course {getSortIcon('course_id')}
                    </th>
                    <th
                      onClick={() => handleSort('day_of_week')}
                      className="sortable"
                    >
                      Day {getSortIcon('day_of_week')}
                    </th>
                    <th
                      onClick={() => handleSort('start_time')}
                      className="sortable"
                    >
                      Start Time {getSortIcon('start_time')}
                    </th>
                    <th
                      onClick={() => handleSort('end_time')}
                      className="sortable"
                    >
                      End Time {getSortIcon('end_time')}
                    </th>
                    <th
                      onClick={() => handleSort('location')}
                      className="sortable"
                    >
                      Location {getSortIcon('location')}
                    </th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredSections.map(section => (
                    <tr key={section.id}>
                      <td>{section.section_code}</td>
                      <td>{section.courses?.code} - {section.courses?.title}</td>
                      <td>
                        {(() => {
                          // Prefer day_of_week field, fall back to parsing legacy schedule
                          if (section.day_of_week) {
                            return section.day_of_week
                          }
                          else if (section.schedule) {
                            // Extract day from legacy schedule format (e.g., "Monday 09:00-10:30")
                            const dayMatch = section.schedule.match(/^(\w+)/);
                            return dayMatch ? dayMatch[1] : 'Not set';
                          }
                          else {
                            return 'Not set';
                          }
                        })()}
                      </td>
                      <td>
                        {(() => {
                          // Prefer start_time field, fall back to parsing legacy schedule
                          if (section.start_time) {
                            return section.start_time
                          }
                          else if (section.schedule) {
                            // Extract start time from legacy schedule format (e.g., "Monday 09:00-10:30")
                            const timeMatch = section.schedule.match(/(\d{2}:\d{2})-/);
                            return timeMatch ? timeMatch[1] : 'Not set';
                          }
                          else {
                            return 'Not set';
                          }
                        })()}
                      </td>
                      <td>
                        {(() => {
                          // Prefer end_time field, fall back to parsing legacy schedule
                          if (section.end_time) {
                            return section.end_time
                          }
                          else if (section.schedule) {
                            // Extract end time from legacy schedule format (e.g., "Monday 09:00-10:30")
                            const timeMatch = section.schedule.match(/-(\d{2}:\d{2})/);
                            return timeMatch ? timeMatch[1] : 'Not set';
                          }
                          else {
                            return 'Not set';
                          }
                        })()}
                      </td>
                      <td>{section.location}</td>
                      <td>
                        <div className="action-buttons">
                          <button
                            onClick={() => editSection(section)}
                            className="btn btn-small btn-action"
                          >
                            Edit
                          </button>
                          <button
                            onClick={() => deleteSection(section.id)}
                            className="btn btn-small btn-action"
                          >
                            Delete
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </div>

      {/* Course Modal */}
      {showCourseModal && (
        <div className="modal-overlay" onClick={() => setShowCourseModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>{editingCourse ? 'Edit Course' : 'Add Course'}</h2>
              <button onClick={() => setShowCourseModal(false)} className="close-button">
                ✕
              </button>
            </div>

            <div className="modal-body">
              <div className="form-group">
                <label className="form-label">Course Code</label>
                <input
                  type="text"
                  value={courseForm.code}
                  onChange={(e) => setCourseForm({...courseForm, code: e.target.value})}
                  className="form-input"
                  placeholder="e.g., CS102"
                  required
                />
              </div>

              <div className="form-group">
                <label className="form-label">Course Title</label>
                <input
                  type="text"
                  value={courseForm.title}
                  onChange={(e) => setCourseForm({...courseForm, title: e.target.value})}
                  className="form-input"
                  placeholder="e.g., Introduction to Computer Science"
                  required
                />
              </div>

              <div className="form-group">
                <label className="form-label">Description</label>
                <textarea
                  value={courseForm.description}
                  onChange={(e) => setCourseForm({...courseForm, description: e.target.value})}
                  className="form-input"
                  placeholder="Course description..."
                  rows={2}
                />
              </div>

              
              
              <div className="form-actions">
                <button onClick={saveCourse} className="btn btn-primary">
                  {editingCourse ? 'Update' : 'Add'} Course
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Section Modal */}
      {showSectionModal && (
        <div className="modal-overlay" onClick={() => setShowSectionModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>{editingSection ? 'Edit Section' : 'Add Section'}</h2>
              <button onClick={() => setShowSectionModal(false)} className="close-button">
                ✕
              </button>
            </div>

            <div className="modal-body">
              <div className="form-group">
                <label className="form-label">Course</label>
                <select
                  value={sectionForm.course_id}
                  onChange={(e) => setSectionForm({...sectionForm, course_id: e.target.value})}
                  className="form-input"
                  required
                >
                  <option value="">Select Course</option>
                  {courseList.map(course => (
                    <option key={course.id} value={course.id}>
                      {course.code} - {course.title}
                    </option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label className="form-label">Section Code</label>
                <input
                  type="text"
                  value={sectionForm.section_code}
                  onChange={(e) => setSectionForm({...sectionForm, section_code: e.target.value})}
                  className="form-input"
                  placeholder="e.g., CS102-01"
                  required
                />
              </div>

              <div className="form-group">
                <label className="form-label">Schedule</label>
                <div className="schedule-form">
                  <div className="schedule-row">
                    <div className="schedule-field">
                      <label className="schedule-label">Day</label>
                      <select
                        value={sectionForm.day_of_week}
                        onChange={(e) => setSectionForm({...sectionForm, day_of_week: e.target.value})}
                        className="form-input"
                        required
                      >
                        <option value="">Select Day</option>
                        <option value="Monday">Monday</option>
                        <option value="Tuesday">Tuesday</option>
                        <option value="Wednesday">Wednesday</option>
                        <option value="Thursday">Thursday</option>
                        <option value="Friday">Friday</option>
                        <option value="Saturday">Saturday</option>
                        <option value="Sunday">Sunday</option>
                      </select>
                    </div>

                    <div className="schedule-field">
                      <label className="schedule-label">Start Time</label>
                      <input
                        type="time"
                        value={sectionForm.start_time}
                        onChange={(e) => setSectionForm({...sectionForm, start_time: e.target.value})}
                        className="form-input"
                        required
                      />
                    </div>

                    <div className="schedule-field">
                      <label className="schedule-label">End Time</label>
                      <input
                        type="time"
                        value={sectionForm.end_time}
                        onChange={(e) => setSectionForm({...sectionForm, end_time: e.target.value})}
                        className="form-input"
                        required
                      />
                    </div>
                  </div>
                </div>
              </div>

              <div className="form-group">
                <label className="form-label">Location</label>
                <input
                  type="text"
                  value={sectionForm.location}
                  onChange={(e) => setSectionForm({...sectionForm, location: e.target.value})}
                  className="form-input"
                  placeholder="e.g., Room 101"
                />
              </div>

              
              <div className="form-actions">
                <button onClick={saveSection} className="btn btn-primary">
                  {editingSection ? 'Update' : 'Add'} Section
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

    </div>
  )
}

export default Classes
