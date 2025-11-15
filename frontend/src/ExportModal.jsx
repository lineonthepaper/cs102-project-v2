import { useEffect, useMemo, useState } from "react";

const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

function formatMeetingDay(value) {
  const meetingDayNames = [
    "Sunday",
    "Monday",
    "Tuesday",
    "Wednesday",
    "Thursday",
    "Friday",
    "Saturday",
  ];

  if (value === null || value === undefined) return "—";
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
  return "—";
}

function formatTimePart(value) {
  if (!value) return "";
  return value.slice(0, 5);
}

function formatTimeRange(start, end) {
  const startFormatted = formatTimePart(start);
  const endFormatted = formatTimePart(end);
  if (!startFormatted && !endFormatted) {
    return "—";
  }
  if (startFormatted && endFormatted) {
    return `${startFormatted} – ${endFormatted}`;
  }
  return startFormatted || endFormatted || "—";
}

function extractFilename(disposition, fallback) {
  if (!disposition) return fallback;
  const match = disposition.match(/filename\*?=(?:UTF-8'')?"?([^\";]+)"?/i);
  if (match && match[1]) {
    try {
      return decodeURIComponent(match[1].replace(/\+/g, "%20"));
    } catch {
      return match[1];
    }
  }
  return fallback;
}

function ExportModal({ isOpen, onClose, userId, userRole }) {
  const [sections, setSections] = useState([]);
  const [searchTerm, setSearchTerm] = useState("");
  const [courseFilter, setCourseFilter] = useState("all");
  const [yearFilter, setYearFilter] = useState("all");
  const [semesterFilter, setSemesterFilter] = useState("all");
  const [selectedSectionIds, setSelectedSectionIds] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [exporting, setExporting] = useState(false);

  useEffect(() => {
    if (!isOpen) return;
    setSearchTerm("");
    setCourseFilter("all");
    setYearFilter("all");
    setSemesterFilter("all");
    setSelectedSectionIds([]);
    fetchAssignedSections();
  }, [isOpen, userId, userRole]);

  const fetchAssignedSections = async () => {
    if (!userId || !userRole) {
      setSections([]);
      return;
    }

    try {
      setLoading(true);
      setError("");

      let endpoint = "";
      if (userRole === "instructor") {
        endpoint = `${API_BASE_URL}/api/instructors`;
      } else if (userRole === "teaching assistant") {
        endpoint = `${API_BASE_URL}/api/teaching-assistants`;
      } else {
        setSections([]);
        return;
      }

      const response = await fetch(endpoint);
      if (!response.ok) {
        throw new Error("Failed to load section assignments");
      }

      const data = await response.json();
      const currentUser = data.find((item) => item.id === userId);

      if (!currentUser) {
        setSections([]);
        return;
      }

      const assignments =
        userRole === "instructor"
          ? currentUser.sectionAssignments || []
          : currentUser.taAssignments || [];

      const normalized = assignments
        .map((assignment) => {
          const section = assignment.section || assignment.sections;
          if (!section) return null;

          const course = section.course || section.courses || {};

          return {
            assignmentId: assignment.id,
            sectionId: section.id,
            sectionCode: section.sectionCode || section.section_code || "—",
            courseCode: course.code || "",
            courseTitle: course.title || "",
            year:
              section.year !== null && section.year !== undefined
                ? Number(section.year)
                : null,
            semester:
              section.semester !== null && section.semester !== undefined
                ? Number(section.semester)
                : null,
            meetingDay:
              section.meetingDay ||
              section.meeting_day ||
              section.dayOfWeek ||
              section.day_of_week ||
              "",
            startTime:
              section.startTime || section.start_time || section.start_time_utc,
            endTime:
              section.endTime || section.end_time || section.end_time_utc,
            location: section.location || "",
          };
        })
        .filter(Boolean);

      const uniqueMap = new Map();
      normalized.forEach((section) => {
        if (!uniqueMap.has(section.sectionId)) {
          uniqueMap.set(section.sectionId, section);
        }
      });

      const uniqueSections = Array.from(uniqueMap.values()).sort((a, b) => {
        const yearDiff = (b.year || 0) - (a.year || 0);
        if (yearDiff !== 0) return yearDiff;
        const semesterDiff = (a.semester || 0) - (b.semester || 0);
        if (semesterDiff !== 0) return semesterDiff;
        return a.sectionCode.localeCompare(b.sectionCode);
      });

      setSections(uniqueSections);
    } catch (err) {
      console.error("Failed to fetch assigned sections:", err);
      setError(err.message || "Failed to fetch assigned sections");
      setSections([]);
    } finally {
      setLoading(false);
    }
  };

  const courseOptions = useMemo(() => {
    const map = new Map();
    sections.forEach((section) => {
      const value = section.courseCode || section.courseTitle;
      if (value) {
        map.set(value, {
          value,
          label: section.courseCode
            ? `${section.courseCode}${section.courseTitle ? ` — ${section.courseTitle}` : ""
              }`
            : section.courseTitle,
        });
      }
    });
    return Array.from(map.values()).sort((a, b) =>
      a.label.localeCompare(b.label, undefined, { numeric: true })
    );
  }, [sections]);

  const yearOptions = useMemo(() => {
    const values = Array.from(
      new Set(
        sections
          .map((section) => section.year)
          .filter((year) => year !== null && year !== undefined)
      )
    );
    return values.sort((a, b) => b - a);
  }, [sections]);

  const semesterOptions = useMemo(() => {
    const values = Array.from(
      new Set(
        sections
          .map((section) => section.semester)
          .filter((semester) => semester !== null && semester !== undefined)
      )
    );
    return values.sort((a, b) => a - b);
  }, [sections]);

  const filteredSections = useMemo(() => {
    const query = searchTerm.trim().toLowerCase();

    return sections.filter((section) => {
      if (courseFilter !== "all") {
        if (
          section.courseCode !== courseFilter &&
          section.courseTitle !== courseFilter
        ) {
          return false;
        }
      }

      if (yearFilter !== "all" && String(section.year) !== String(yearFilter)) {
        return false;
      }

      if (
        semesterFilter !== "all" &&
        String(section.semester) !== String(semesterFilter)
      ) {
        return false;
      }

      if (!query) return true;

      const haystack = [
        section.sectionCode,
        section.courseCode,
        section.courseTitle,
        section.location,
        section.year ? `year ${section.year}` : "",
        section.semester ? `semester ${section.semester}` : "",
        section.meetingDay,
      ]
        .filter(Boolean)
        .join(" ")
        .toLowerCase();

      return haystack.includes(query);
    });
  }, [sections, courseFilter, yearFilter, semesterFilter, searchTerm]);

  const toggleSectionSelection = (sectionId) => {
    setSelectedSectionIds((prev) => {
      if (prev.includes(sectionId)) {
        return prev.filter((id) => id !== sectionId);
      }
      return [...prev, sectionId];
    });
  };

  const buildPayload = (sectionCode, year, semester) => ({
    userId,
    role: userRole,
    sectionCode,
    year,
    semester,
  });

  const downloadFile = async (format, payload, fallbackName) => {
    const endpoint =
      format === "csv"
        ? `${API_BASE_URL}/api/export/export-csv`
        : `${API_BASE_URL}/api/export/export-xlsx`;

    const response = await fetch(endpoint, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(payload),
    });

    if (!response.ok) {
      throw new Error("Export request failed");
    }

    const blob = await response.blob();
    const disposition = response.headers.get("Content-Disposition");
    const filename = extractFilename(
      disposition,
      format === "csv" ? `${fallbackName}.csv` : `${fallbackName}.xlsx`
    );

    const url = window.URL.createObjectURL(blob);
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = filename;
    anchor.click();
    window.URL.revokeObjectURL(url);
  };

  const handleExport = async (format) => {
    if (exporting) return;
    if (!sections.length) {
      alert("You do not have any section assignments to export.");
      return;
    }

    try {
      setExporting(true);

      const hasSelection = selectedSectionIds.length > 0;
      if (hasSelection) {
        const sectionsToExport = sections.filter((section) =>
          selectedSectionIds.includes(section.sectionId)
        );

        for (const section of sectionsToExport) {
          const yearParam =
            section.year !== null && section.year !== undefined
              ? String(section.year)
              : "all";
          const semesterParam =
            section.semester !== null && section.semester !== undefined
              ? String(section.semester)
              : "all";
          const sectionCode = section.sectionCode || "all";
          const fallbackName = `Attendance_${sectionCode.replace(/\s+/g, "_")}_${yearParam}_${semesterParam}`;

          await downloadFile(
            format,
            buildPayload(sectionCode, yearParam, semesterParam),
            fallbackName
          );
        }
      } else {
        const yearParam =
          yearFilter !== "all" ? String(yearFilter) : "all";
        const semesterParam =
          semesterFilter !== "all" ? String(semesterFilter) : "all";

        const fallbackName = `Attendance_All_${yearParam}_${semesterParam}`;

        await downloadFile(
          format,
          buildPayload("all", yearParam, semesterParam),
          fallbackName
        );
      }
    } catch (err) {
      console.error("Export failed:", err);
      alert(err.message || "Failed to export data");
    } finally {
      setExporting(false);
    }
  };

  if (!isOpen) {
    return null;
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div
        className="modal-content manage-modal export-modal"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="manage-modal-header">
          <div className="manage-modal-title">
            <h2>Export Data</h2>
          </div>
          <button onClick={onClose} className="close-button">
            ✕
          </button>
        </div>

        <div className="manage-modal-body">
          <div className="section-controls">
            <div className="section-filter-row">
              <div className="filter-block">
                <label className="filter-label" htmlFor="export-search">
                  Search
                </label>
                <div className="search-input-wrapper">
                  <input
                    id="export-search"
                    type="text"
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    className="search-input"
                    placeholder="Search by section, course, location..."
                  />
                  <span className="search-icon">⚲</span>
                </div>
              </div>

              <div className="filter-block">
                <label className="filter-label" htmlFor="export-course-filter">
                  Course
                </label>
                <select
                  id="export-course-filter"
                  value={courseFilter}
                  onChange={(e) => setCourseFilter(e.target.value)}
                  className="filter-select"
                >
                  <option value="all">All Courses</option>
                  {courseOptions.map((option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>
              </div>

              <div className="filter-block">
                <label className="filter-label" htmlFor="export-year-filter">
                  Year
                </label>
                <select
                  id="export-year-filter"
                  value={yearFilter}
                  onChange={(e) => setYearFilter(e.target.value)}
                  className="filter-select"
                >
                  <option value="all">All Years</option>
                  {yearOptions.map((year) => (
                    <option key={year} value={year}>
                      {year}
                    </option>
                  ))}
                </select>
              </div>

              <div className="filter-block">
                <label
                  className="filter-label"
                  htmlFor="export-semester-filter"
                >
                  Semester
                </label>
                <select
                  id="export-semester-filter"
                  value={semesterFilter}
                  onChange={(e) => setSemesterFilter(e.target.value)}
                  className="filter-select"
                >
                  <option value="all">All Semesters</option>
                  {semesterOptions.map((semester) => (
                    <option key={semester} value={semester}>
                      Semester {semester}
                    </option>
                  ))}
                </select>
              </div>
            </div>
            <div className="section-summary-row">
              <span className="selection-badge">
                {selectedSectionIds.length} selected
              </span>
            </div>
          </div>

          {error && <div className="export-error">{error}</div>}

          <div className="ta-section-table-container export-section-table">
            {loading ? (
              <div className="ta-no-sections">Loading your sections...</div>
            ) : filteredSections.length ? (
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
                  {filteredSections.map((section) => {
                    const isSelected = selectedSectionIds.includes(
                      section.sectionId
                    );
                    const meetingDayLabel = formatMeetingDay(
                      section.meetingDay
                    );
                    const timeRangeLabel = formatTimeRange(
                      section.startTime,
                      section.endTime
                    );
                    const yearLabel =
                      section.year !== null && section.year !== undefined
                        ? `Year ${section.year}`
                        : "Year —";
                    const semesterLabel =
                      section.semester !== null && section.semester !== undefined
                        ? `Semester ${section.semester}`
                        : "Semester —";
                    const courseLabel = section.courseCode
                      ? `${section.courseCode}${
                          section.courseTitle
                            ? ` — ${section.courseTitle}`
                            : ""
                        }`
                      : section.courseTitle || "Untitled Course";

                    return (
                      <tr
                        key={section.sectionId}
                        className={`clickable-row${
                          isSelected ? " selected-row" : ""
                        }`}
                        onClick={() =>
                          toggleSectionSelection(section.sectionId)
                        }
                      >
                        <td className="checkbox-cell">
                          <input type="checkbox" readOnly checked={isSelected} />
                        </td>
                        <td>{section.sectionCode}</td>
                        <td>{courseLabel}</td>
                        <td>
                          <div>{yearLabel}</div>
                          <div>{semesterLabel}</div>
                        </td>
                        <td>
                          <div>{meetingDayLabel}</div>
                          <div>{timeRangeLabel}</div>
                        </td>
                        <td>{section.location || "—"}</td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            ) : (
              <div className="ta-no-sections">
                {sections.length
                  ? "No sections match your current filters."
                  : "You don't have any section assignments yet."}
              </div>
            )}
          </div>
        </div>

        <div className="modal-actions">
          <button
            type="button"
            onClick={onClose}
            className="btn btn-secondary modal-secondary"
            disabled={exporting}
          >
            Close
          </button>
          <button
            type="button"
            onClick={() => handleExport("csv")}
            className="btn btn-primary modal-primary"
            disabled={exporting || (!sections.length && !selectedSectionIds.length)}
          >
            {exporting ? "Exporting…" : "Export CSV"}
          </button>
          <button
            type="button"
            onClick={() => handleExport("xlsx")}
            className="btn btn-primary modal-primary"
            disabled={exporting || (!sections.length && !selectedSectionIds.length)}
          >
            {exporting ? "Exporting…" : "Export XLSX"}
          </button>
        </div>
      </div>
    </div>
  );
}

export default ExportModal;

