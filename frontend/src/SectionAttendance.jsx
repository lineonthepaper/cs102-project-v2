import { useEffect, useState, useRef } from "react";
import { useParams, useNavigate } from "react-router-dom";

const API_BASE_URL = "http://localhost:8080";
const SCAN_SKIP_DURATION_MS = 8000;

function SectionAttendance() {
  const { sectionId, sectionCode } = useParams();
  const navigate = useNavigate();

  const [sessions, setSessions] = useState([]);
  const [filteredSessions, setFilteredSessions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedSession, setSelectedSession] = useState(null);
  const [showMarkingModal, setShowMarkingModal] = useState(false);
  const [showSessionModal, setShowSessionModal] = useState(false);
  const [activeTab, setActiveTab] = useState('active');
  const [sessionForm, setSessionForm] = useState({
    section_id: sectionId,
    session_date: "",
    scheduled_start_time: "09:00",
    scheduled_end_time: "10:30",
    notes: ""
  });
  const [editingSession, setEditingSession] = useState(null);

  const [students, setStudents] = useState([]);
  const [attendanceRecords, setAttendanceRecords] = useState({});
  const [markingSearchTerm, setMarkingSearchTerm] = useState("");
  const [statusFilterMarking, setStatusFilterMarking] = useState("all");
  

  const [showScannerModal, setShowScannerModal] = useState(false);
  const [scannerMessage, setScannerMessage] = useState('Starting camera...');
  const [scannerError, setScannerError] = useState('');
  const [recognizedCandidate, setRecognizedCandidate] = useState(null);

  const videoRef = useRef(null);
  const overlayCanvasRef = useRef(null);
  const canvasRef = useRef(null);
  const scanIntervalRef = useRef(null);
  const isSendingFrameRef = useRef(false);
  const skippedStudentsRef = useRef(new Map());
  const isScanningRef = useRef(false);
  const recognizedCandidateRef = useRef(null);
  const lastResponseIdRef = useRef(0);


  const fetchSessions = async () => {
    setLoading(true);
    try {
      const res = await fetch(`${API_BASE_URL}/api/attendance/sessions`);
      const data = await res.json();
      const filtered = data.filter(s => String(s.sectionId) === String(sectionId));
      setSessions(filtered);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    let filtered = [...sessions];
    
    if (activeTab === 'archived') {
      filtered = filtered.filter(s => s.status === 'ARCHIVED');
    } else {
      filtered = filtered.filter(s => s.status !== 'ARCHIVED');
    }
    
    setFilteredSessions(filtered);
  }, [sessions, activeTab]);


  const handleArchiveSession = async (sessionId, e) => {
    e.stopPropagation();
    if (!confirm('Are you sure you want to archive this session? This will lock it and prevent any further edits.')) {
      return;
    }

    try {
      const response = await fetch(`${API_BASE_URL}/api/attendance/sessions/${sessionId}/archive`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' }
      });

      if (!response.ok) throw new Error('Failed to archive session');

      await fetchSessions();
      alert('Session archived successfully');
    } catch (error) {
      console.error('Error archiving session:', error);
      alert('Failed to archive session: ' + error.message);
    }
  };

 
  const handleUnarchiveSession = async (sessionId, e) => {
    e.stopPropagation();
    if (!confirm('Are you sure you want to unarchive this session? This will allow attendance to be edited.')) {
      return;
    }

    try {
      const response = await fetch(`${API_BASE_URL}/api/attendance/sessions/${sessionId}/reopen`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' }
      });

      if (!response.ok) throw new Error('Failed to unarchive session');

      await fetchSessions();
      alert('Session unarchived successfully');
    } catch (error) {
      console.error('Error unarchiving session:', error);
      alert('Failed to unarchive session: ' + error.message);
    }
  };


  const handleCancelSession = async (sessionId, e) => {
    e.stopPropagation();
    if (!confirm('Are you sure you want to cancel this session?')) {
      return;
    }

    try {
      const response = await fetch(`${API_BASE_URL}/api/attendance/sessions/${sessionId}/cancel`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' }
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({ message: 'Failed to cancel session' }));
        throw new Error(errorData.message || errorData.error || 'Failed to cancel session');
      }

      await fetchSessions();
      alert('Session cancelled successfully');
    } catch (error) {
      console.error('Error cancelling session:', error);
      alert('Failed to cancel session: ' + error.message);
    }
  };


  const handleReactivateSession = async (sessionId, e) => {
    e.stopPropagation();
    if (!confirm('Are you sure you want to reactivate this session? This will allow attendance to be marked.')) {
      return;
    }

    try {
      const response = await fetch(`${API_BASE_URL}/api/attendance/sessions/${sessionId}/reopen`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' }
      });

      if (!response.ok) throw new Error('Failed to reactivate session');

      await fetchSessions();
      alert('Session reactivated successfully');
    } catch (error) {
      console.error('Error reactivating session:', error);
      alert('Failed to reactivate session: ' + error.message);
    }
  };


  const fetchStudents = async (sessionId) => {
    try {
      const res = await fetch(`${API_BASE_URL}/api/sections/${sectionId}/students`);
      const data = await res.json();
      const studentsList = (data.students || data || []).map(student => ({
        ...student,
        id: student.id,
        first_name: student.first_name || student.firstName || "",
        last_name: student.last_name || student.lastName || "",
        email: student.email || ""
      }));
      setStudents(studentsList);


      const recordsRes = await fetch(`${API_BASE_URL}/api/attendance/sessions/${sessionId}/records`);
      const records = await recordsRes.json();
      const recordsMap = {};
      records.forEach(record => {
        recordsMap[record.userId] = {
          status: record.status,
          notes: record.notes || "",
          checkin_time: record.checkinTime
        };
      });
      setAttendanceRecords(recordsMap);
    } catch (err) {
      console.error(err);
      setStudents([]);
      setAttendanceRecords({});
    }
  };


  const openMarkAttendance = (session) => {
    setSelectedSession(session);
    fetchStudents(session.id);
    setShowMarkingModal(true);
  };


  const saveSession = async () => {
    try {
      const requestBody = {
        sectionId: parseInt(sectionId),
        sessionDate: sessionForm.session_date,
        scheduledStartTime: sessionForm.scheduled_start_time,
        scheduledEndTime: sessionForm.scheduled_end_time,
        notes: sessionForm.notes || null
      };

      const method = editingSession ? "PUT" : "POST";
      const url = editingSession
        ? `${API_BASE_URL}/api/attendance/sessions/${editingSession.id}`
        : `${API_BASE_URL}/api/attendance/sessions`;
      
      await fetch(url, {
        method,
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(requestBody),
      });
      
      fetchSessions();
      setShowSessionModal(false);
      setEditingSession(null);
      setSessionForm({ ...sessionForm, session_date: "", notes: "" });
    } catch (err) {
      console.error(err);
    }
  };


  const markAttendance = async (studentId, status, notes = "", checkinTime = null, similarity = -1) => {
    try {
      const requestBody = {
        sessionId: selectedSession.id,
        userId: studentId,
        status: status,
        checkinTime: checkinTime || (status === 'PRESENT' || status === 'LATE' ? new Date().toISOString() : null),
        notes: notes || null,
        confidenceLevel: similarity,
        isAutomatic: similarity !== -1
      };

      const response = await fetch(`${API_BASE_URL}/api/attendance/records`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(requestBody)
      });

      if (!response.ok) throw new Error('Failed to mark attendance');


      setAttendanceRecords(prev => ({
        ...prev,
        [studentId]: { status, notes, checkin_time: checkinTime }
      }));
    } catch (error) {
      console.error('Error marking attendance:', error);
      alert('Failed to mark attendance');
    }
  };

  const filteredStudents = students.filter(student => {
    const matchesSearch = `${student.first_name} ${student.last_name}`.toLowerCase().includes(markingSearchTerm.toLowerCase()) ||
                          student.id.toString().includes(markingSearchTerm);
    const matchesStatus = statusFilterMarking === "all" || 
                          (attendanceRecords[student.id]?.status || "unmarked") === statusFilterMarking ||
                          (statusFilterMarking === "unmarked" && !attendanceRecords[student.id]);
    return matchesSearch && matchesStatus;
  });

  // KNN STUPID CS102
  useEffect(() => {
    recognizedCandidateRef.current = recognizedCandidate;
  }, [recognizedCandidate]);

  useEffect(() => {
    if (!showScannerModal) {
      stopCamera();
      return;
    }
    startCamera();
    return () => stopCamera();
  }, [showScannerModal]);

  const openScannerModal = () => {
    if (!selectedSession) return;
    setScannerError('');
    setScannerMessage('Starting camera...');
    setRecognizedCandidate(null);
    setShowScannerModal(true);
  };

  const closeScannerModal = () => {
    if (videoRef.current && videoRef.current.srcObject) {
      const tracks = videoRef.current.srcObject.getTracks();
      tracks.forEach(track => track.stop());
      videoRef.current.srcObject = null;
    }
    setShowScannerModal(false);
    setRecognizedCandidate(null);
    setScannerMessage('Starting camera...');
    setScannerError('');
  };

  const startCamera = async () => {
    if (!showScannerModal) return;

    if (!videoRef.current?.srcObject || videoRef.current.paused) {
      try {
        const stream = await navigator.mediaDevices.getUserMedia({ 
          video: { facingMode: 'user', width: { ideal: 640 }, height: { ideal: 480 } } 
        });
        if (videoRef.current) {
          videoRef.current.srcObject = stream;
          await videoRef.current.play();
        }
      } catch (error) {
        console.error('Unable to access camera:', error);
        setScannerError('Unable to access camera. Please check permissions.');
        setShowScannerModal(false);
        return;
      }
    }

    setScannerMessage('Scanning faces...');
    isScanningRef.current = true;
    startFrameLoop();
  };

  const stopCamera = () => {
    stopFrameLoop();
    isSendingFrameRef.current = false;
    isScanningRef.current = false;
  };

  const startFrameLoop = () => {
    if (!showScannerModal || !selectedSession) return;
    stopFrameLoop();
    scanIntervalRef.current = setInterval(captureAndSendFrame, 62);
  };

  const stopFrameLoop = () => {
    if (scanIntervalRef.current) {
      clearInterval(scanIntervalRef.current);
      scanIntervalRef.current = null;
    }
  };

  const shouldSkipStudent = (studentId) => {
    if (!studentId) return false;
    const lastSeen = skippedStudentsRef.current.get(studentId);
    if (!lastSeen) return false;
    if (Date.now() - lastSeen > SCAN_SKIP_DURATION_MS) {
      skippedStudentsRef.current.delete(studentId);
      return false;
    }
    return true;
  };

  const recordSkipForStudent = (studentId) => {
    if (!studentId) return;
    skippedStudentsRef.current.set(studentId, Date.now());
  };

  const captureAndSendFrame = async () => {
    if (!isScanningRef.current || recognizedCandidateRef.current) return;
    if (!selectedSession || !videoRef.current || !canvasRef.current) return;
    if (isSendingFrameRef.current) return;

    const video = videoRef.current;
    if (video.readyState < 2) return;

    const canvas = canvasRef.current;
    const context = canvas.getContext('2d');
    const srcW = video.videoWidth || 640;
    const srcH = video.videoHeight || 480;
    const targetW = Math.min(480, srcW);
    const scale = targetW / srcW;
    const targetH = Math.round(srcH * scale);
    canvas.width = targetW;
    canvas.height = targetH;
    context.drawImage(video, 0, 0, targetW, targetH);

    const imageData = canvas.toDataURL('image/jpeg', 0.5);
    isSendingFrameRef.current = true;
    const thisRequestId = ++lastResponseIdRef.current;

    try {
      const response = await fetch(`${API_BASE_URL}/api/attendance/sessions/${selectedSession.id}/scan`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ imageData })
      });

      if (!response.ok) throw new Error('Scanner request failed');

      const result = await response.json();
      if (thisRequestId === lastResponseIdRef.current) {
        handleScanResponse(result);
      }
    } catch (error) {
      console.error('Face scan error:', error);
      setScannerError('Unable to process camera feed');
    } finally {
      isSendingFrameRef.current = false;
    }
  };

  const handleScanResponse = (result) => {
    if (!result) return;

    setScannerError('');

    if (result.alreadyMarked) {
      setScannerMessage(result.message || 'Student already marked');
      if (result.student?.id) recordSkipForStudent(result.student.id);
      return;
    }

    if (!result.matched) {
      setScannerMessage(result.message || 'Scanning...');
      if (result.boundingBox) {
        drawOverlay(result.boundingBox, result.similarity, result.student);
      } else {
        clearOverlay();
      }
      return;
    }

    const student = result.student;
    if (!student || shouldSkipStudent(student.id)) return;

    setScannerMessage(result.message || 'Match found - please confirm');
    setRecognizedCandidate({
      student,
      similarity: result.similarity,
      rawSimilarity: result.rawSimilarity ?? null,
      margin: result.margin ?? null,
      recommendedStatus: result.recommendedStatus,
      recommendedCheckInTime: result.recommendedCheckInTime
    });

    if (result.boundingBox) {
      drawOverlay(result.boundingBox, result.similarity, student);
    }
  };

  const drawOverlay = (bbox, similarity, student) => {
    if (!overlayCanvasRef.current || !videoRef.current || !bbox) return;

    const overlay = overlayCanvasRef.current;
    const video = videoRef.current;
    const ctx = overlay.getContext('2d');

    const displayWidth = video.offsetWidth || video.videoWidth || 640;
    const displayHeight = video.offsetHeight || video.videoHeight || 480;
    overlay.width = displayWidth;
    overlay.height = displayHeight;

    ctx.clearRect(0, 0, overlay.width, overlay.height);

    const scaleX = bbox.originalWidth ? displayWidth / bbox.originalWidth : 1;
    const scaleY = bbox.originalHeight ? displayHeight / bbox.originalHeight : 1;

    const x = bbox.x * scaleX;
    const y = bbox.y * scaleY;
    const width = bbox.width * scaleX;
    const height = bbox.height * scaleY;

    ctx.strokeStyle = '#00ff00';
    ctx.lineWidth = 4;
    ctx.strokeRect(x, y, width, height);

    if (student) {
      const nameText = `${student.firstName || ''} ${student.lastName || ''}`.trim();
      const matchText = `${Math.round(similarity * 100)}%`;

      ctx.font = 'bold 16px sans-serif';
      const nameWidth = ctx.measureText(nameText).width + 12;

      ctx.fillStyle = 'rgba(0, 255, 0, 0.95)';
      ctx.fillRect(x, y - 26, nameWidth, 24);
      ctx.fillStyle = '#000';
      ctx.fillText(nameText, x + 6, y - 6);

      ctx.font = 'bold 14px sans-serif';
      const matchWidth = ctx.measureText(matchText).width + 12;

      ctx.fillStyle = 'rgba(0, 255, 0, 0.95)';
      ctx.fillRect(x, y + height + 2, matchWidth, 22);
      ctx.fillStyle = '#000';
      ctx.fillText(matchText, x + 6, y + height + 18);
    }
  };

  const clearOverlay = () => {
    if (!overlayCanvasRef.current) return;
    const ctx = overlayCanvasRef.current.getContext('2d');
    ctx.clearRect(0, 0, overlayCanvasRef.current.width, overlayCanvasRef.current.height);
  };

  const resetScannerForNextStudent = () => {
    setScannerError('');
    setScannerMessage('Scanning faces...');
    clearOverlay();
    isScanningRef.current = true;
    startFrameLoop();
  };

  const handleScannerAccept = async () => {
    if (!recognizedCandidate) return;

    const { student, recommendedStatus, recommendedCheckInTime, similarity } = recognizedCandidate;
    const status = recommendedStatus || 'PRESENT';
    const checkInTime = recommendedCheckInTime || new Date().toISOString();

    try {
      setScannerMessage(`Recording attendance for ${student.id}...`);
      await markAttendance(student.id, status, '', checkInTime, similarity);
      recordSkipForStudent(student.id);

      setRecognizedCandidate(null);
      setScannerMessage(`✓ ${student.id} marked as ${status}`);

      setTimeout(() => {
        resetScannerForNextStudent();
      }, 1500);
    } catch (error) {
      console.error('Failed to record attendance:', error);
      setRecognizedCandidate(null);
      setScannerError('Unable to record attendance');
      recordSkipForStudent(student.id);

      setTimeout(() => {
        resetScannerForNextStudent();
      }, 2000);
    }
  };

  const handleScannerReject = () => {
    if (!recognizedCandidate) return;
    recordSkipForStudent(recognizedCandidate.student.id);
    setRecognizedCandidate(null);
    resetScannerForNextStudent();
  };

  useEffect(() => {
    fetchSessions();
  }, [sectionId]);

  return (
    <div className="container">
      <div className="page-header">
        <h1>Mark Attendance - {sectionCode}</h1>
        <button onClick={() => navigate('/home')} className="btn btn-secondary-small">
          Back to Home
        </button>
      </div>

      {/* Tab Navigation */}
      <div className="tabs-container">
        <div className="tabs">
          <button
            className={`tab ${activeTab === 'active' ? 'active' : ''}`}
            onClick={() => setActiveTab('active')}
          >
            Active Sessions
          </button>
          <button
            className={`tab ${activeTab === 'archived' ? 'active' : ''}`}
            onClick={() => setActiveTab('archived')}
          >
            Archived Sessions
          </button>
        </div>
      </div>

      {loading ? (
        <div className="loading">Loading sessions...</div>
      ) : (
        <>
          <div className="panel-header">
            <h3>Attendance Sessions</h3>
            {activeTab === 'active' && (
              <button onClick={() => setShowSessionModal(true)} className="btn btn-primary-small">
                Create Session
              </button>
            )}
          </div>

          <div className="table-container">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Date</th>
                  <th>Time</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {filteredSessions.map(session => (
                  <tr key={session.id}>
                    <td>{new Date(session.sessionDate).toLocaleDateString('en-SG')}</td>
                    <td>{session.scheduledStartTime} - {session.scheduledEndTime}</td>
                    <td><span className={`status-badge status-${session.status?.toLowerCase()}`}>{session.status}</span></td>
                    <td>
                      <div className="action-buttons">
                        {session.status === 'ARCHIVED' ? (
                          <button
                            onClick={(e) => handleUnarchiveSession(session.id, e)}
                            className="btn btn-small btn-action"
                            title="Unarchive session to allow editing"
                          >
                            Unarchive
                          </button>
                        ) : (
                          <>
                            {session.status !== 'CANCELLED' && (
                              <button
                                onClick={() => openMarkAttendance(session)}
                                className="btn btn-small btn-action"
                              >
                                Mark Attendance
                              </button>
                            )}
                            {['COMPLETED', 'CLOSED', 'ENDED'].includes(session.status) && (
                              <button
                                onClick={(e) => handleArchiveSession(session.id, e)}
                                className="btn btn-small btn-action"
                                title="Archive session to lock it"
                              >
                                Archive
                              </button>
                            )}
                            {session.status === 'CANCELLED' && (
                              <>
                                <button
                                  onClick={(e) => handleReactivateSession(session.id, e)}
                                  className="btn btn-small btn-action"
                                  title="Reactivate session to allow attendance marking"
                                >
                                  Reactivate
                                </button>
                                <button
                                  onClick={(e) => handleArchiveSession(session.id, e)}
                                  className="btn btn-small btn-action"
                                  title="Archive session to lock it"
                                >
                                  Archive
                                </button>
                              </>
                            )}
                            {['SCHEDULED', 'IN_PROGRESS'].includes(session.status) && (
                              <button
                                onClick={(e) => handleCancelSession(session.id, e)}
                                className="btn btn-small btn-action"
                                title="Cancel session"
                              >
                                Cancel
                              </button>
                            )}
                          </>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}

      {/* Mark Attendance Modal */}
      {showMarkingModal && selectedSession && (
        <div className="modal-overlay" onClick={() => setShowMarkingModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '1200px', width: '95%' }}>
            <div className="modal-header">
              <div>
                <h2>Mark Attendance</h2>
                <p>Section {sectionCode} • {new Date(selectedSession.sessionDate).toLocaleDateString('en-SG')}</p>
              </div>
              <div className="modal-header-actions">
                <button onClick={openScannerModal} className="btn btn-small btn-action">
                  Record
                </button>
                <button onClick={() => setShowMarkingModal(false)} className="close-button">✕</button>
              </div>
            </div>

            <div className="modal-body">
              {/* Search & Filter */}
              <div className="filters-section">
                <div className="search-filter-bar">
                  <div className="search-block">
                    <label>Search</label>
                    <input
                      type="text"
                      value={markingSearchTerm}
                      onChange={e => setMarkingSearchTerm(e.target.value)}
                      className="search-input"
                      placeholder="Search by student ID or name"
                    />
                  </div>
                  <div className="filter-block">
                    <label>Status</label>
                    <select
                      value={statusFilterMarking}
                      onChange={e => setStatusFilterMarking(e.target.value)}
                      className="filter-select"
                    >
                      <option value="all">All Students</option>
                      <option value="unmarked">Unmarked</option>
                      <option value="PRESENT">Present</option>
                      <option value="LATE">Late</option>
                      <option value="ABSENT">Absent</option>
                    </select>
                  </div>
                </div>
              </div>

              {/* Students Table */}
              <div className="table-container">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Student ID</th>
                      <th>Name</th>
                      <th>Status</th>
                      <th>Notes</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredStudents.map(student => {
                      const record = attendanceRecords[student.id];
                      return (
                        <tr key={student.id}>
                          <td>{student.id}</td>
                          <td>{student.first_name} {student.last_name}</td>
                          <td>
                            {record ? (
                              <span className={`status-badge status-${record.status?.toLowerCase()}`}>
                                {record.status}
                              </span>
                            ) : (
                              <span style={{ color: '#9ca3af' }}>Not marked</span>
                            )}
                          </td>
                          <td>
                            <input
                              type="text"
                              value={record?.notes || ""}
                              onChange={e => markAttendance(student.id, record?.status || "PRESENT", e.target.value, record?.checkin_time)}
                              placeholder="Notes..."
                              className="form-input"
                            />
                          </td>
                          <td>
                            <div style={{ display: 'flex', gap: '0.5rem' }}>
                              <button
                                onClick={() => markAttendance(student.id, 'PRESENT', '', new Date().toISOString())}
                                className="btn btn-small btn-action"
                              >Present</button>
                              <button
                                onClick={() => markAttendance(student.id, 'ABSENT')}
                                className="btn btn-small btn-action"
                              >Absent</button>
                              <button
                                onClick={() => markAttendance(student.id, 'LATE', '', new Date().toISOString())}
                                className="btn btn-small btn-action"
                              >Late</button>
                            </div>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Scanner Modal */}
      {showScannerModal && (
        <div className="modal-overlay" onClick={closeScannerModal}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '1200px', width: '95%', maxHeight: '90vh' }}>
            <div className="modal-header">
              <div>
                <h2>Face Recognition</h2>
                <p style={{ color: '#6b7280', fontSize: '0.875rem' }}>{scannerMessage}</p>
              </div>
              <button onClick={closeScannerModal} className="close-button">✕</button>
            </div>

            <div className="modal-body" style={{ padding: 0 }}>
              <div style={{
                position: 'relative',
                width: '100%',
                height: 'calc(90vh - 120px)',
                minHeight: '500px',
                background: '#000'
              }}>
                <video ref={videoRef} autoPlay playsInline muted style={{
                  width: '100%',
                  height: '100%',
                  objectFit: 'contain'
                }} />
                <canvas ref={overlayCanvasRef} style={{
                  position: 'absolute',
                  top: 0,
                  left: 0,
                  width: '100%',
                  height: '100%',
                  pointerEvents: 'none'
                }} />
                <canvas ref={canvasRef} style={{ display: 'none' }} />
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Confirmation Modal */}
      {recognizedCandidate && showScannerModal && (
        <div className="modal-overlay" style={{
          position: 'fixed',
          zIndex: 9999,
          background: 'rgba(0, 0, 0, 0.7)'
        }} onClick={handleScannerReject}>
          <div style={{
            position: 'absolute',
            top: '50%',
            left: '50%',
            transform: 'translate(-50%, -50%)',
            background: '#fff',
            borderRadius: '14px',
            padding: '24px',
            maxWidth: '500px',
            width: '90vw',
            boxShadow: '0 10px 40px rgba(0,0,0,0.3)'
          }} onClick={(e) => e.stopPropagation()}>
            <h2 style={{ marginTop: 0, marginBottom: '16px', fontSize: '20px' }}>
              Confirm Attendance
            </h2>

            <div style={{
              background: '#fff',
              border: '1px solid #ededed',
              borderRadius: '10px',
              padding: '16px',
              marginBottom: '20px'
            }}>
              <div style={{ fontWeight: 600, marginBottom: 8 }}>
                ID: {recognizedCandidate.student.id}
                <span style={{ marginLeft: 8, fontSize: 12, background: '#f4f4f4', padding: '2px 6px', borderRadius: 4 }}>
                  {Math.round((recognizedCandidate.similarity || 0) * 100)}% match
                </span>
              </div>
              <div style={{ fontSize: '1.2em', fontWeight: 700, marginBottom: 4 }}>
                {recognizedCandidate.student.firstName} {recognizedCandidate.student.lastName}
              </div>
              <div style={{ fontSize: 14, color: '#555' }}>
                {recognizedCandidate.student.email}
              </div>
              <div style={{ fontSize: 12, color: '#6b7280', marginTop: 8 }}>
                Cosine: {recognizedCandidate.rawSimilarity !== null && recognizedCandidate.rawSimilarity !== undefined
                  ? recognizedCandidate.rawSimilarity.toFixed(3)
                  : '—'} | Margin: {recognizedCandidate.margin !== null && recognizedCandidate.margin !== undefined
                  ? recognizedCandidate.margin.toFixed(3)
                  : '—'}
              </div>
            </div>

            <div style={{ display: 'flex', gap: 12 }}>
              <button onClick={handleScannerAccept} style={{
                flex: 1,
                padding: '12px 0',
                fontWeight: 600,
                color: '#fff',
                background: '#000',
                border: 'none',
                borderRadius: 6,
                cursor: 'pointer'
              }}>Accept</button>
              <button onClick={handleScannerReject} style={{
                flex: 1,
                padding: '12px 0',
                fontWeight: 600,
                color: '#000',
                background: '#fff',
                border: '1px solid #000',
                borderRadius: 6,
                cursor: 'pointer'
              }}>Reject</button>
            </div>
          </div>
        </div>
      )}

      {/* Create Session Modal */}
      {showSessionModal && (
        <div className="modal-overlay" onClick={() => setShowSessionModal(false)}>
          <div className="modal-content" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h2>{editingSession ? "Edit Session" : "Create Session"}</h2>
              <button onClick={() => setShowSessionModal(false)} className="close-button">✕</button>
            </div>
            <div className="modal-body">
              <div className="form-group">
                <label>Date</label>
                <input
                  type="date"
                  value={sessionForm.session_date}
                  onChange={e => setSessionForm({ ...sessionForm, session_date: e.target.value })}
                  className="form-input"
                />
              </div>
              <div className="form-group">
                <label>Start Time</label>
                <input
                  type="time"
                  value={sessionForm.scheduled_start_time}
                  onChange={e => setSessionForm({ ...sessionForm, scheduled_start_time: e.target.value })}
                  className="form-input"
                />
              </div>
              <div className="form-group">
                <label>End Time</label>
                <input
                  type="time"
                  value={sessionForm.scheduled_end_time}
                  onChange={e => setSessionForm({ ...sessionForm, scheduled_end_time: e.target.value })}
                  className="form-input"
                />
              </div>
              <div className="form-group">
                <label>Notes</label>
                <textarea
                  value={sessionForm.notes}
                  onChange={e => setSessionForm({ ...sessionForm, notes: e.target.value })}
                  className="form-input"
                />
              </div>
              <button onClick={saveSession} className="btn btn-primary">
                {editingSession ? "Update Session" : "Create Session"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default SectionAttendance;