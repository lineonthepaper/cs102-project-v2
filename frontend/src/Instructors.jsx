import { useState, useEffect } from "react";
import { supabase } from "./supabase";
import { useNavigate } from "react-router-dom";
import { useAuth } from "./AuthContext";
import { useRole } from "./role";

const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

function Instructors() {
  const navigate = useNavigate();
  const { user } = useAuth();
  // console.log(user.user.id);
  const userRole = useRole();

  const [loading, setLoading] = useState(true);
  const [instructors, setInstructors] = useState([]);
  const [courses, setCourses] = useState([]);
  const [sections, setSections] = useState([]);
  const [instructorAssignments, setInstructorAssignments] = useState([]);
  const [instructorAssignmentsList, setInstructorAssignmentsList] = useState(
    []
  );

  const [showAssignModal, setShowAssignModal] = useState(false);
  const [showAddModal, setShowAddModal] = useState(false);

  const [selectedInstructor, setSelectedInstructor] = useState(null);
  const [selectedSections, setSelectedSections] = useState([]);

  const [listSearchTerm, setListSearchTerm] = useState("");
  const [sectionSearchTerm, setSectionSearchTerm] = useState("");
  const [sectionCourseFilter, setSectionCourseFilter] = useState("all");
  const [modalCourseFilter, setModalCourseFilter] = useState("all");
  const [modalYearFilter, setModalYearFilter] = useState("all");
  const [modalSemesterFilter, setModalSemesterFilter] = useState("all");

  const [newInstructorEmail, setNewInstructorEmail] = useState("");
  const [newInstructorPassword, setNewInstructorPassword] = useState("");
  const [newInstructorFirstName, setNewInstructorFirstName] = useState("");
  const [newInstructorLastName, setNewInstructorLastName] = useState("");
  const [addingInstructor, setAddingInstructor] = useState(false);
  const [addModalKey, setAddModalKey] = useState(0);

  useEffect(() => {
    fetchInitialData();
    if (user) {
    }
  }, []);

  const meetingDayNames = [
    "Sunday",
    "Monday",
    "Tuesday",
    "Wednesday",
    "Thursday",
    "Friday",
    "Saturday",
  ];

  const formatMeetingDay = (value) => {
    if (value === null || value === undefined) return "Day ?";
    if (typeof value === "number") {
      return meetingDayNames[value] ?? `Day ${value}`;
    }
    const numericValue = Number(value);
    if (!Number.isNaN(numericValue)) {
      return meetingDayNames[numericValue] ?? `Day ${numericValue}`;
    }
    if (typeof value === "string" && value.trim().length > 0) {
      return value.trim();
    }
    return "Day ?";
  };

  const formatTimePart = (value) => {
    if (!value) return "";
    return value.slice(0, 5);
  };

  const formatTimeRange = (start, end) => {
    const startFormatted = formatTimePart(start);
    const endFormatted = formatTimePart(end);
    if (!startFormatted && !endFormatted) {
      return "";
    }
    if (startFormatted && endFormatted) {
      return `${startFormatted} - ${endFormatted}`;
    }
    return startFormatted || endFormatted;
  };

  const formatSemesterLabel = (value) => {
    if (value === null || value === undefined || value === "") {
      return "Semester ?";
    }
    return `Semester ${value}`;
  };

  const fetchInitialData = async () => {
    try {
      await Promise.all([
        fetchInstructors(),
        fetchCourses(),
        fetchSections(),
        fetchInstructorAssignments(),
      ]);
    } catch (error) {
      console.error("Error loading instructors data:", error);
    } finally {
      setLoading(false);
    }
  };

  const fetchInstructors = async () => {
    try {
      const response = await fetch(`${API_BASE_URL}/api/instructors`);
      if (!response.ok) {
        throw new Error("Failed to fetch instructors");
      }

      const instructorsData = await response.json();
      // Transform backend data to frontend format
      const transformedInstructors = instructorsData.map((instructor) => ({
        id: instructor.id,
        email: instructor.email,
        first_name: instructor.firstName,
        last_name: instructor.lastName,
        enabled: instructor.enabled,
        auth_id: instructor.authId,
        is_instructor: true,
      }));

      setInstructors(transformedInstructors);

      // Extract section assignments
      const assignments = [];
      instructorsData.forEach((instructor) => {
        if (
          instructor.sectionAssignments &&
          instructor.sectionAssignments.length > 0
        ) {
          console.log(instructor);
          instructor.sectionAssignments.forEach((assignment) => {
            assignments.push({
              id: assignment.id,
              user_id: assignment.userId,
              section_id: assignment.sectionId,
              role: assignment.role,
              is_active: assignment.isActive,
              sections: assignment.section
                ? {
                    id: assignment.section.id,
                    section_code: assignment.section.sectionCode,
                    course_id: assignment.section.courseId,
                    courses: assignment.section.course
                      ? {
                          id: assignment.section.course.id,
                          code: assignment.section.course.code,
                          title: assignment.section.course.title,
                        }
                      : null,
                  }
                : null,
            });
          });
        }
      });

      setInstructorAssignmentsList(assignments);
      setInstructorAssignments(assignments);
    } catch (error) {
      console.error("Error fetching instructors:", error);
      throw error;
    }
  };

  const fetchSections = async () => {
    const { data, error } = await supabase.from("sections").select(`
        *,
        courses(*)
      `);

    if (error) throw error;
    setSections(data || []);
  };

  const fetchCourses = async () => {
    const { data, error } = await supabase.from("courses").select("*");

    if (error) throw error;
    setCourses(data || []);
  };

  const fetchInstructorAssignments = async () => {
    // This is now handled by fetchInstructors
    // Just in case we need to refresh separately
    await fetchInstructors();
  };

  const openAssignModal = (instructor) => {
    setSelectedInstructor(instructor);
    const currentAssignments = instructorAssignments
      .filter((assignment) => assignment.user_id === instructor.id)
      .map((assignment) => Number(assignment.section_id));

    setSelectedSections(currentAssignments);
    setSectionSearchTerm("");
    setModalCourseFilter("all");
    setModalYearFilter("all");
    setModalSemesterFilter("all");
    setShowAssignModal(true);
  };

  const closeAssignModal = () => {
    setShowAssignModal(false);
    setSelectedInstructor(null);
    setSelectedSections([]);
    setSectionSearchTerm("");
    setModalCourseFilter("all");
    setModalYearFilter("all");
    setModalSemesterFilter("all");
  };

  const openAddModal = () => {
    // Clear all fields before opening
    setNewInstructorEmail("");
    setNewInstructorPassword("");
    setNewInstructorFirstName("");
    setNewInstructorLastName("");
    setAddModalKey((prev) => prev + 1); // Force modal to remount with fresh state
    setShowAddModal(true);
  };

  const closeAddModal = () => {
    setShowAddModal(false);
    setNewInstructorEmail("");
    setNewInstructorPassword("");
    setNewInstructorFirstName("");
    setNewInstructorLastName("");
  };

  const getFilteredSections = () => {
    if (!selectedInstructor) return [];

    const takenSections = new Set(
      instructorAssignments
        .filter(
          (assignment) =>
            assignment.user_id !== selectedInstructor.id &&
            assignment.section_id != null
        )
        .map((assignment) => Number(assignment.section_id))
    );

    return sections
      .filter((section) => {
        const sectionId = Number(section.id);
        if (
          modalCourseFilter !== "all" &&
          Number(section.course_id) !== Number(modalCourseFilter)
        ) {
          return false;
        }
        if (
          modalYearFilter !== "all" &&
          Number(section.year) !== Number(modalYearFilter)
        ) {
          return false;
        }
        if (
          modalSemesterFilter !== "all" &&
          Number(section.semester) !== Number(modalSemesterFilter)
        ) {
          return false;
        }
        if (
          takenSections.has(sectionId) &&
          !selectedSections.includes(sectionId)
        ) {
          return false;
        }
        if (!sectionSearchTerm) return true;
        const lower = sectionSearchTerm.toLowerCase();
        return (
          section.section_code?.toLowerCase().includes(lower) ||
          section.courses?.code?.toLowerCase().includes(lower) ||
          section.courses?.title?.toLowerCase().includes(lower) ||
          `${section.year ?? ""}`.toLowerCase().includes(lower) ||
          `${section.semester ?? ""}`.toLowerCase().includes(lower) ||
          section.schedule?.toLowerCase().includes(lower) ||
          section.location?.toLowerCase().includes(lower)
        );
      })
      .sort((a, b) => a.section_code.localeCompare(b.section_code));
  };

  const toggleSectionSelection = (sectionId) => {
    const normalizedId = Number(sectionId);
    setSelectedSections((prev) => {
      if (prev.includes(normalizedId)) {
        return prev.filter((id) => id !== normalizedId);
      }
      return [...prev, normalizedId];
    });
  };

  const saveAssignments = async () => {
    if (!selectedInstructor) return;

    try {
      const response = await fetch(
        `${API_BASE_URL}/api/instructors/${selectedInstructor.id}/assignments`,
        {
          method: "PUT",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            sectionIds: selectedSections,
          }),
        }
      );

      const result = await response.json();

      if (!response.ok) {
        throw new Error(
          result.message || "Failed to save instructor assignments"
        );
      }

      await fetchInstructorAssignments();
      closeAssignModal();
    } catch (error) {
      console.error("Failed to save instructor assignments:", error);
      alert(error.message || "Failed to save instructor assignments");
    }
  };

  const addInstructor = async () => {
    const email = newInstructorEmail.trim();
    const password = newInstructorPassword.trim();
    const firstName = newInstructorFirstName.trim();
    const lastName = newInstructorLastName.trim();

    if (!email || !password || !firstName || !lastName) {
      alert("Please complete all fields.");
      return;
    }

    setAddingInstructor(true);

    try {
      // First, create Supabase Auth account
      const adminSession = await supabase.auth.getSession();
      const adminTokens = adminSession?.data?.session
        ? {
            access_token: adminSession.data.session.access_token,
            refresh_token: adminSession.data.session.refresh_token,
          }
        : null;

      const { data: signUpData, error: signUpError } =
        await supabase.auth.signUp({
          email,
          password,
        });

      if (signUpError) {
        throw new Error(
          signUpError.message || "Unable to create authentication account"
        );
      }

      const authId = signUpData?.user?.id;

      if (!authId) {
        throw new Error(
          "Failed to obtain authentication id for the instructor."
        );
      }

      if (signUpData?.session && adminTokens) {
        const { error: restoreError } = await supabase.auth.setSession(
          adminTokens
        );
        if (restoreError) {
          console.warn(
            "Instructor account created but admin session was not restored automatically.",
            restoreError
          );
        }
      }

      // Then, call backend API to create instructor in database
      const response = await fetch(`${API_BASE_URL}/api/instructors`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          email,
          password,
          firstName,
          lastName,
        }),
      });

      const result = await response.json();

      if (!response.ok) {
        throw new Error(result.message || "Failed to add instructor");
      }

      // Update the instructor with auth_id
      if (result.id && authId) {
        const { error: authUpdateError } = await supabase
          .from("users")
          .update({ auth_id: authId })
          .eq("id", result.id);

        if (authUpdateError) {
          console.error("Failed to update auth_id:", authUpdateError);
        }
      }

      await fetchInstructors();
      await fetchInstructorAssignments();
      closeAddModal();
      alert("Instructor account created successfully.");
    } catch (error) {
      console.error("Error adding instructor:", error);
      alert(`Failed to add instructor: ${error.message || "Unknown error"}`);
    } finally {
      setAddingInstructor(false);
    }
  };

  const removeInstructor = async (instructor) => {
    if (
      !window.confirm(
        `Remove ${instructor.first_name} ${instructor.last_name} as an instructor?`
      )
    ) {
      return;
    }

    try {
      // Get the user's auth_id before removing instructor
      const { data: userData, error: userFetchError } = await supabase
        .from("users")
        .select("auth_id")
        .eq("id", instructor.id)
        .single();

      if (userFetchError) throw userFetchError;

      // Call backend API to remove instructor
      const response = await fetch(
        `${API_BASE_URL}/api/instructors/${instructor.id}`,
        {
          method: "DELETE",
        }
      );

      const result = await response.json();

      if (!response.ok) {
        throw new Error(result.message || "Failed to remove instructor");
      }

      // If user had auth_id, remove them from Supabase Auth
      if (userData?.auth_id) {
        const authId = userData.auth_id;
        console.log(
          "Attempting to remove instructor from Supabase Auth. Auth ID:",
          authId
        );

        try {
          const authResponse = await fetch(
            `${API_BASE_URL}/api/admin/instructors/${authId}`,
            {
              method: "DELETE",
            }
          );

          if (!authResponse.ok) {
            const errorText = await authResponse.text();
            console.warn(
              "Failed to remove instructor from Supabase Auth:",
              authResponse.status,
              errorText
            );
          } else {
            console.log("Instructor successfully removed from Supabase Auth.");
          }
        } catch (authError) {
          console.error(
            "Error calling backend to delete auth user:",
            authError
          );
        }
      }

      await fetchInstructors();
      await fetchInstructorAssignments();
    } catch (error) {
      console.error("Failed to remove instructor:", error);
      alert(error.message || "Failed to remove instructor");
    }
  };

  const availableYears = Array.from(
    new Set(
      (sections || [])
        .map((section) => section?.year)
        .filter((year) => year !== null && year !== undefined)
    )
  ).sort((a, b) => Number(a) - Number(b));

  const availableSemesters = Array.from(
    new Set(
      (sections || [])
        .map((section) => section?.semester)
        .filter((semester) => semester !== null && semester !== undefined)
    )
  ).sort((a, b) => Number(a) - Number(b));

  if (loading) {
    return <div className="loading">Loading instructors...</div>;
  }

  const filteredInstructors = instructors.filter((instructor) => {
    if (sectionCourseFilter !== "all") {
      const courseMatches = instructorAssignments.some(
        (assignment) =>
          assignment.user_id === instructor.id &&
          assignment.sections &&
          Number(assignment.sections.course_id) === Number(sectionCourseFilter)
      );

      if (!courseMatches) {
        return false;
      }
    }

    if (!listSearchTerm) return true;
    const lower = listSearchTerm.toLowerCase();
    return (
      `${instructor.first_name} ${instructor.last_name}`
        .toLowerCase()
        .includes(lower) || instructor.email.toLowerCase().includes(lower)
    );
  });

  return (
    <div className="container">
      <div className="page-header">
        <h1>Instructors Management</h1>
        <button
          onClick={() => navigate("/home")}
          className="btn btn-secondary-small"
        >
          Back to Home
        </button>
      </div>

      <div className="filters-section">
        <div className="filters-header">
          <h3>Search & Filter</h3>
        </div>
        <div className="search-filter-bar">
          <div className="search-block">
            <label className="filter-label" htmlFor="instructors-search">
              Search
            </label>
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
            <label className="filter-label" htmlFor="instructors-course-filter">
              Course
            </label>
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
          {userRole === "admin" && (
            <button onClick={openAddModal} className="btn btn-primary-small">
            Add Instructor
          </button>
          )}
          
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
              );

              return (
                <tr key={instructor.id}>
                  <td className="instructor-id">{instructor.id}</td>
                  <td>
                    {instructor.first_name} {instructor.last_name}
                  </td>
                  <td>{instructor.email}</td>
                  <td>{assignments.length}</td>
                  <td>
                    <div className="action-buttons">
                      {(userRole === "admin" ||
                        instructor.id === user.user.id) && (
                        <button
                          onClick={() => openAssignModal(instructor)}
                          className="btn btn-small btn-action"
                        >
                          Manage Sections
                        </button>
                      )}
                      {userRole === "admin" && (
                        <button
                          onClick={() => removeInstructor(instructor)}
                          className="btn btn-small btn-action"
                        >
                          Remove Instructor
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      {showAssignModal && selectedInstructor && (
        <div className="modal-overlay" onClick={closeAssignModal}>
          <div
            className="modal-content manage-modal"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="manage-modal-header">
              <div className="manage-modal-title">
                <h2>Manage Sections</h2>
                <div className="manage-summary">
                  <div className="name-with-id">
                    {selectedInstructor.first_name}{" "}
                    {selectedInstructor.last_name}
                    <span className="tag-id">{selectedInstructor.id}</span>
                  </div>
                  <div className="email">{selectedInstructor.email}</div>
                </div>
              </div>
              <button onClick={closeAssignModal} className="close-button">
                ✕
              </button>
            </div>

            <div className="manage-modal-body">
              <div className="section-controls">
                <div className="section-filter-row">
                  <div className="filter-block">
                    <label className="filter-label" htmlFor="instructor-section-search">
                      Search
                    </label>
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
                  <div className="filter-block">
                    <label className="filter-label" htmlFor="section-course-filter">
                      Course
                    </label>
                    <select
                      id="section-course-filter"
                      value={modalCourseFilter}
                      onChange={(e) => setModalCourseFilter(e.target.value)}
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
                  <div className="filter-block">
                    <label className="filter-label" htmlFor="section-year-filter">
                      Year
                    </label>
                    <select
                      id="section-year-filter"
                      value={modalYearFilter}
                      onChange={(e) => setModalYearFilter(e.target.value)}
                      className="filter-select"
                    >
                      <option value="all">All Years</option>
                      {availableYears.map((year) => (
                        <option key={year} value={year}>
                          {year}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div className="filter-block">
                    <label className="filter-label" htmlFor="section-semester-filter">
                      Semester
                    </label>
                    <select
                      id="section-semester-filter"
                      value={modalSemesterFilter}
                      onChange={(e) => setModalSemesterFilter(e.target.value)}
                      className="filter-select"
                    >
                      <option value="all">All Semesters</option>
                      {availableSemesters.map((semester) => (
                        <option key={semester} value={semester}>
                          {formatSemesterLabel(semester)}
                        </option>
                      ))}
                    </select>
                  </div>
                </div>
                <div className="section-summary-row">
                  <span className="selection-badge">{selectedSections.length} selected</span>
                </div>
              </div>

              <div className="ta-section-table-container">
                <table className="data-table small dense ta-section-table">
                  <thead>
                    <tr>
                      <th></th>
                      <th>Section</th>
                      <th>Course</th>
                      <th>Term</th>
                      <th>Schedule</th>
                      <th>Location</th>
                    </tr>
                  </thead>
                  <tbody>
                    {getFilteredSections().map((section) => {
                      const isSelected = selectedSections.includes(section.id);
                      const meetingDayLabel = formatMeetingDay(
                        section.day_of_week ?? section.meeting_day
                      );
                      const timeRangeLabel = formatTimeRange(
                        section.start_time,
                        section.end_time
                      );
                      const yearLabel =
                        section.year !== null && section.year !== undefined
                          ? section.year
                          : "Year ?";
                      const semesterLabel = formatSemesterLabel(section.semester);
                      return (
                        <tr
                          key={section.id}
                          className={isSelected ? "selected-row" : ""}
                          onClick={() => toggleSectionSelection(section.id)}
                        >
                          <td className="checkbox-cell">
                            <input
                              type="checkbox"
                              checked={isSelected}
                              readOnly
                            />
                          </td>
                          <td>{section.section_code}</td>
                          <td>{section.courses?.title || "—"}</td>
                          <td>
                            <div>{yearLabel}</div>
                            <div>{semesterLabel}</div>
                          </td>
                          <td>
                            <div>{meetingDayLabel}</div>
                            <div>{timeRangeLabel || "—"}</div>
                          </td>
                          <td>{section.location || "—"}</td>
                        </tr>
                      );
                    })}
                    {getFilteredSections().length === 0 && (
                      <tr>
                        <td colSpan={6} style={{ textAlign: "center" }}>
                          No sections available
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            </div>

            <div className="modal-actions">
              <button
                onClick={saveAssignments}
                className="btn btn-primary modal-primary"
              >
                Save {selectedSections.length} Assignment
                {selectedSections.length === 1 ? "" : "s"}
              </button>
            </div>
          </div>
        </div>
      )}

      {showAddModal && (
        <div className="modal-overlay" onClick={closeAddModal}>
          <div
            className="modal-content"
            onClick={(e) => e.stopPropagation()}
            key={addModalKey}
          >
            
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
                  autoComplete="off"
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
                  autoComplete="off"
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
                  autoComplete="off"
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
                  autoComplete="new-password"
                />
                <small className="form-help">
                  Instructors can sign in immediately using this password. Share
                  it securely and encourage them to change it after the first
                  login.
                </small>
              </div>

              <div className="form-actions">
                <button
                  onClick={addInstructor}
                  className="btn btn-primary"
                  disabled={addingInstructor}
                >
                  {addingInstructor ? "Adding..." : "Add Instructor"}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default Instructors;
