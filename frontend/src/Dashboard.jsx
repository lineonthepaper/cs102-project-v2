import { useRole } from "./role";
import { useAuth } from "./AuthContext";
import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import ExportModal from "./ExportModal";

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

    const canExport = userRole === "instructor" || userRole === "teaching assistant";
    const canImport = userRole === "admin";

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
                {canImport && (
                    <div className="card">
                        <h3>Import Students</h3>
                        <p>Import student data and face images from a ZIP package</p>
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
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
                        </div>
                    </div>
                )}

                {canExport && (
                    <div className="card">
                        <h3>Export Data</h3>
                        <p>Download attendance and enrollment reports for your sections</p>
                        <button onClick={() => { setShowExportModal(true) }} className="btn btn-secondary">
                            Export Data
                        </button>
                    </div>
                )}

                <ExportModal
                    isOpen={showExportModal}
                    onClose={() => setShowExportModal(false)}
                    userId={user.user.id}
                    userRole={userRole}
                />
            </div>
        </div>
    );
}

export default Dashboard;
