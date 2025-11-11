import { useAuth } from "./AuthContext";
import { useNavigate } from "react-router-dom";
import { useState } from "react";
import { supabase } from "./supabase";
import { useRole } from "./role";
import ExportModal from "./ExportModal";
import ImportSummaryModal from "./ImportSummaryModal";

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
    const [showImportSummary, setShowImportSummary] = useState(false);
    const [importSummary, setImportSummary] = useState(null);
    const [importing, setImporting] = useState(false);
    const handleImportStudents = async (event) => {
        const file = event.target.files[0];
        if (!file) return;

        if (!file.name.toLowerCase().endsWith('.zip')) {
            alert('Please upload a ZIP file containing a CSV and student image folders');
            event.target.value = '';
            return;
        }

        try {
            setImporting(true);
            setImportSummary(null);
            setShowImportSummary(false);
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

            setImportSummary(result);
            setShowImportSummary(true);
            event.target.value = '';

        } catch (error) {
            console.error("Import failed:", error);
            alert(`Failed to import students: ${error.message}`);
            event.target.value = '';
        } finally {
            setImporting(false);
        }
    };

    const triggerFileInput = () => {
        const input = document.getElementById('homeCsvFileInput');
        if (input) {
            input.click();
        }
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

                {userRole === "admin" && (
                    <div className="card">
                        <h3>Import Students</h3>
                        <p>Import student data and face images from a ZIP package</p>
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                            <input
                                id="homeCsvFileInput"
                                type="file"
                                accept=".zip"
                                style={{ display: 'none' }}
                                onChange={handleImportStudents}
                            />
                            <button onClick={triggerFileInput} className="btn btn-secondary" disabled={importing}>
                                Import Students (ZIP)
                            </button>
                        </div>
                    </div>
                )}
                <ImportSummaryModal
                    isOpen={showImportSummary}
                    summary={importSummary}
                    onClose={() => {
                        setShowImportSummary(false);
                        setImportSummary(null);
                    }}
                />
            </div>
        </div>
    );
}

export default Home;
