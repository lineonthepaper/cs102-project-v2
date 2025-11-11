import { useAuth } from "./AuthContext";
import { useNavigate } from "react-router-dom";
import { useState } from "react";
import { supabase } from "./supabase";
import { useRole } from "./role";
import ExportModal from "./ExportModal";

function Home() {
    const { user, logout } = useAuth();
    const navigate = useNavigate();
    const userRole = useRole();

    const handleLogout = async () => {
        logout();
        navigate("/login");
    };
    const API_BASE_URL = 'http://localhost:8080'

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

    const canExport = userRole === "instructor" || userRole === "teaching assistant";

    return (
        <div className="container">
            <div className="header">
                <div className="header-content">
                    <div className="header-title">
                        <h1>Smart Attendance System</h1>
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
                {(userRole === "admin" || userRole === "instructor") && (
                    <div className="card">
                        <h3>Students</h3>
                        <p>Manage student records, enrollments, and face recognition data</p>
                        <button
                            onClick={() => navigate("/students")}
                            className="btn btn-secondary"
                        >
                            Manage Students
                        </button>
                    </div>
                )}


                <div className="card">
                    <h3>Teaching Assistants</h3>
                    <p>Manage Teaching Assistant status and section assignments</p>
                    <button
                        onClick={() => navigate("/teaching-assistants")}
                        className="btn btn-secondary"
                    >
                        Manage Teaching Assistants
                    </button>
                </div>

                {(userRole === "admin" || userRole === "instructor") && (
                    <div className="card">
                        <h3>Instructors</h3>
                        <p>Manage instructor accounts and teaching assignments</p>
                        <button
                            onClick={() => navigate("/instructors")}
                            className="btn btn-secondary"
                        >
                            Manage Instructors
                        </button>
                    </div>

                )}

                {(userRole === "admin" || userRole === "instructor") && (
                    <div className="card">
                        <h3>Classes</h3>
                        <p>Create and manage course sections and schedules</p>
                        <button
                            onClick={() => navigate("/classes")}
                            className="btn btn-secondary"
                        >
                            Manage Classes
                        </button>
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

export default Home;
