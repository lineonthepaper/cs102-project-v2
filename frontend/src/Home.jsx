import { useAuth } from "./AuthContext";
import { useNavigate } from "react-router-dom";
import { useState, useEffect } from "react";
import { supabase } from "./supabase";

function Home() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [userRole, setUserRole] = useState("Loading...");

  useEffect(() => {
    if (user) {
      if (user.user.isInstructor) setUserRole("Instructor");
      else if (user.user.isTA) setUserRole("Teaching Assistant");
      else if (user.user.isStudent) setUserRole("Student");
      else setUserRole("Admin");
    }
  }, [user]);

  const handleLogout = async () => {
    logout();
    navigate("/login");
  };

  const downloadReport = async () => {
    try {
      const response = await fetch("http://localhost:8080/api/export/full-database-zip");
      const blob = await response.blob();

      const url = window.URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = "fullDB.zip";
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

  return (
    <div className="container">
      <div className="header">
        <div className="header-content">
          <div className="header-title">
            <h1>Smart Attendance System</h1>
            <span className="role-text">{userRole}</span>
          </div>
          <div className="header-actions">
            <button onClick={handleLogout} className="btn btn-secondary-small">
              Logout
            </button>
          </div>
        </div>
      </div>

      <div className="dashboard-grid">
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

        <div className="card">
          <h3>Mark Attendance</h3>
          <p>Create sessions, mark student attendance, and manage records</p>
          <button
            onClick={() => navigate("/attendance")}
            className="btn btn-secondary"
          >
            Mark Attendance
          </button>
        </div>

        <div className="card">
          <h3>Import / Export</h3>
          <p>Import students from CSV or export database report</p>
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
            <button onClick={downloadReport} className="btn btn-secondary">
              Export Report to CSV
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

export default Home;
