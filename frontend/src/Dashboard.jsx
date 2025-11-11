import { useRole } from "./role";
import { useAuth } from "./AuthContext";
import { useState, useEffect, useMemo } from "react";
import { useNavigate } from "react-router-dom";

function Dashboard() {
    const navigate = useNavigate();
    const userRole = useRole();
    const { user, logout } = useAuth();
    const [loading, setLoading] = useState(false);
    // console.log(userRole);
    const [myAssignments, setMyAssignments] = useState([]);
    const API_BASE_URL = "http://localhost:8080";

    // import/export states
    const [showExportModal, setShowExportModal] = useState(false);
    const [sectionList, setSectionList] = useState([]);
    const [sectionFilter, setSectionFilter] = useState('all');
    const [yearFilter, setYearFilter] = useState('all');
    const [semesterFilter, setSemesterFilter] = useState('all');
    const yearOptions = [2025, 2026, 2027, 2028, 2029, 2030]
    const semesterOptions = [1, 2];

        // import and export stuff

    const downloadReportAsCSV = async () => {
        try {
            const response = await fetch(`${API_BASE_URL}/api/export/export-csv`,
                {
                    method: "POST",
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        userId: user.user.id,
                        sectionCode: sectionFilter,
                        year: yearFilter,
                        semester: semesterFilter
                    })
                }
            );

            const blob = await response.blob();

            const url = window.URL.createObjectURL(blob);
            const a = document.createElement("a");
            a.href = url;
            a.download = "sectionReport.csv";
            a.click();
            window.URL.revokeObjectURL(url);
        } catch (error) {
            console.error("Download failed:", error);
            alert("Failed to download report");
        }
    };

    const downloadReportAsXLSX = async () => {
        try {
            const response = await fetch(`${API_BASE_URL}/api/export/export-xlsx`,
                {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json",
                    },
                    body: JSON.stringify({
                        userId: user.user.id,
                        sectionCode: sectionFilter,
                        year: yearFilter,
                        semester: semesterFilter
                    })
                }
            );

            const blob = await response.blob();

            const url = window.URL.createObjectURL(blob);
            const a = document.createElement("a");
            a.href = url;
            a.download = "sectionReport.xlsx";
            a.click();
            window.URL.revokeObjectURL(url);
        } catch (error) {
            console.error("Download failed:", error);
            alert("Failed to download report");
        }
    };

    const handleImportStudents = async (event) => {
        const file = event.target.files[0];
        if (!file) return;

        if (!file.name.toLowerCase().endsWith('.zip')) {
            alert('Please upload a ZIP file containing a CSV and student image folders');
            event.target.value = '';
            return;
        }

        try {
            const formData = new FormData();
            formData.append('file', file);

            const response = await fetch('http://localhost:8080/api/students/import', {
                method: 'POST',
                body: formData
            });

            const result = await response.json();

            if (!response.ok) {
                throw new Error(result.message || 'Import failed');
            }

            let message = `Successfully imported ${result.imported} out of ${result.total} students!`;
            if (result.errors && result.errors.length > 0) {
                message += `\n\nErrors:\n${result.errors.slice(0, 5).join('\n')}`;
                if (result.errors.length > 5) {
                    message += `\n... and ${result.errors.length - 5} more errors`;
                }
            }

            alert(message);
            event.target.value = '';

        } catch (error) {
            console.error("Import failed:", error);
            alert(`Failed to import students: ${error.message}`);
            event.target.value = '';
        }
    };

    const triggerFileInput = () => {
        document.getElementById('csvFileInput').click();
    };

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
    };

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
        fetchSections()
    }, [])

    // logout handler
    const handleLogout = async () => {
        logout();
        navigate("/login");
    };

    useEffect(() => {
        const fetchMyAssignments = async () => {
            try {
                setLoading(true);
                let endpoint = "";
                if (userRole === "instructor") {
                    endpoint = `${API_BASE_URL}/api/instructors`;
                } else if (userRole === "teaching assistant") {
                    endpoint = `${API_BASE_URL}/api/teaching-assistants`;
                } else {
                    return;
                }

                const response = await fetch(endpoint);
                if (!response.ok) throw new Error(`Failed to fetch ${userRole}s`);

                const data = await response.json();
                const currentUser = data.find((item) => item.id === user.user.id);

                if (!currentUser) {
                    setMyAssignments([]);
                    return;
                }
                const assignments =
                    userRole === "instructor"
                        ? currentUser.sectionAssignments || []
                        : currentUser.taAssignments || [];

                setMyAssignments(assignments);
                // console.log(assignments[0].sectionId);
                setLoading(false);
            } catch (err) {
                console.error("Error fetching my assignments:", err);
            }
        };

        fetchMyAssignments();
    }, [userRole, user]);

    if (loading) {
        return <div className="loading">Loading sections...</div>
    }

    return (

        <div className="container">
            <div className="header">
                <div className="header-content">
                    <div className="header-title">
                        <h1>Dashboard</h1>
                        <span className="role-text">{user.user.firstName} {user.user.lastName} ({userRole})</span>
                    </div>
                    <div className="header-actions">
                        <button onClick={handleLogout} className="btn btn-secondary-small">
                            Logout
                        </button>
                    </div>
                </div>
            </div>

            <div className="dashboard-grid">
                {myAssignments.map((assignment) => (
                    <div key={assignment.id} className="card">
                        <h3>{assignment.section.sectionCode}</h3>
                        <p>
                            Year {assignment.section.year} - Semester{" "}
                            {assignment.section.semester}
                        </p>
                        <button
                            onClick={() => navigate(`/attendance/${assignment.sectionId}/${assignment.section.sectionCode}`)}
                            className="btn btn-secondary"
                        >
                            Manage Section
                        </button>
                    </div>
                ))}
                {userRole === "instructor" && (
                  <div className="card">
                    <h3>Import / Export</h3>
                    <p>Import students from CSV/XLSX or export database report</p>
                    <div style={{ display: 'flex', gap: '10px', flexDirection: 'column' }}>
                        <input
                            id="csvFileInput"
                            type="file"
                            accept=".zip"
                            style={{ display: 'none' }}
                            onChange={handleImportStudents}
                        />

                        <button onClick={triggerFileInput} className="btn btn-secondary">
                            Import Students (ZIP)
                        </button>
                        <button onClick={() => { setShowExportModal(true) }} className="btn btn-secondary">
                            Export Report to CSV
                        </button>
                    </div>
                </div>
                )}
                

                {/* Export Modal */}
                {showExportModal && (
                    <div className="modal-overlay" onClick={() => { setShowExportModal(false) }}>
                        <div className="modal-content" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '1200px', width: '55%', maxHeight: '90vh' }}>
                            <div className="modal-header">
                                <div>
                                    <h2>Export Course Data</h2>
                                    <p style={{ color: '#6b7280', margin: '0.5rem 0 0 0', fontSize: '0.875rem' }}>
                                        Export attendance and student data for selected sections
                                    </p>
                                </div>
                                <div className="modal-header-actions">
                                    <button onClick={() => { setShowExportModal(false) }} className="close-button">
                                        ✕
                                    </button>
                                </div>
                            </div>
                            <div className="modal-body">
                                <div style={{ display: 'flex', marginBottom: '1.5rem', justifyContent: 'space-between' }}>
                                    <div className="filter-block" style={{ 'flex-grow': 1, 'margin-right': '1rem' }}>
                                        <label className="filter-label" htmlFor="section-filter">Section</label>
                                        <select
                                            id="section-filter"
                                            value={sectionFilter}
                                            onChange={(e) => setSectionFilter(e.target.value)}
                                            className="filter-select"
                                        >
                                            <option value="all">All Sections</option>
                                            {uniqueSections.map(section => (
                                                <option key={section.id} value={section.section_code}>
                                                    {section.section_code}
                                                </option>
                                            ))}
                                        </select>
                                    </div>

                                    <div className="filter-block" style={{ flexGrow: 1, marginRight: '1rem' }}>
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

                                    <div className="filter-block" style={{ flexGrow: 1 }}>
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
                                </div>
                                <div>
                                    <button className="btn btn-primary-small" onClick={downloadReportAsCSV}>Export as CSV</button>
                                    <button className="btn btn-primary-small" style={{ marginLeft: '1rem' }} onClick={downloadReportAsXLSX}>Export as XLSX</button>
                                </div>
                            </div>
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
}

export default Dashboard;
