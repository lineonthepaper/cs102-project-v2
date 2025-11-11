import { useRole } from "./role";
import { useAuth } from "./AuthContext";
import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";

function Dashboard() {
  const navigate = useNavigate();
  const userRole = useRole();
  const { user,logout } = useAuth();
  const [loading,setLoading] = useState(false);
  // console.log(userRole);
  const [myAssignments, setMyAssignments] = useState([]);
  const API_BASE_URL = "http://localhost:8080";

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
      </div>
    </div>
  );
}

export default Dashboard;
