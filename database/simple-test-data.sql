-- Smart Attendance System simplified test data
-- Updated to support new schema for local testing

-- Clear existing data and reset identity counters
TRUNCATE TABLE
    attendance_records,
    attendance_sessions,
    ta_assignments,
    section_assignments,
    section_enrollments,
    sections,
    courses,
    users
RESTART IDENTITY CASCADE;

-- 1. USERS
-- =====================================================

INSERT INTO users (
    id,
    email,
    first_name,
    last_name,
    enabled,
    is_student,
    is_instructor,
    is_ta,
    created_at,
    updated_at
) VALUES
('S0000001', 'student1@smu.edu.sg', 'Student', '1', true, true, false, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('S0000002', 'student2@smu.edu.sg', 'Student', '2', true, true, false, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('S0000003', 'student3@smu.edu.sg', 'Student', '3', true, true, false, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- =====================================================
-- 2. COURSES
-- =====================================================

INSERT INTO courses (code, title, description, created_at) VALUES
('CS102', 'Programming Fundamentals II', 'Core Java programming with labs and recitations.', CURRENT_TIMESTAMP),
('CS201', 'Data Structures and Algorithms', 'Advanced data structures and algorithm analysis.', CURRENT_TIMESTAMP),
('CS301', 'Database Systems', 'Relational database design and SQL.', CURRENT_TIMESTAMP);

-- =====================================================
-- 3. SECTIONS (with updated schedule columns)
-- =====================================================

INSERT INTO sections (course_id, section_code, year, semester, meeting_day, start_time, end_time, location, max_capacity, day_of_week, schedule, created_at) VALUES
-- CS102 Sections - Different terms
((SELECT id FROM courses WHERE code = 'CS102' LIMIT 1), 'CS102-01', 2025, 1, 1, '09:00:00', '10:30:00', 'Room 102', 40, 'Monday', 'Monday 09:00-10:30', CURRENT_TIMESTAMP),
((SELECT id FROM courses WHERE code = 'CS102' LIMIT 1), 'CS102-01', 2025, 2, 3, '13:00:00', '14:30:00', 'Room 202', 35, 'Wednesday', 'Wednesday 13:00-14:30', CURRENT_TIMESTAMP),
((SELECT id FROM courses WHERE code = 'CS102' LIMIT 1), 'CS102-02', 2025, 1, 5, '15:00:00', '16:30:00', 'Room 301', 30, 'Friday', 'Friday 15:00-16:30', CURRENT_TIMESTAMP),

-- CS201 Sections - Different terms
((SELECT id FROM courses WHERE code = 'CS201' LIMIT 1), 'CS201-01', 2025, 1, 2, '10:00:00', '11:30:00', 'Room 105', 45, 'Tuesday', 'Tuesday 10:00-11:30', CURRENT_TIMESTAMP),
((SELECT id FROM courses WHERE code = 'CS201' LIMIT 1), 'CS201-01', 2026, 1, 4, '14:00:00', '15:30:00', 'Room 205', 40, 'Thursday', 'Thursday 14:00-15:30', CURRENT_TIMESTAMP),

-- CS301 Sections
((SELECT id FROM courses WHERE code = 'CS301' LIMIT 1), 'CS301-01', 2025, 2, 1, '11:00:00', '12:30:00', 'Room 103', 35, 'Monday', 'Monday 11:00-12:30', CURRENT_TIMESTAMP);

-- =====================================================
-- 4. SECTION ASSIGNMENTS (legacy system - keep for compatibility)
-- =====================================================

-- No instructors in test data - sections available for assignment

-- =====================================================
-- 5. TA ASSIGNMENTS (intentionally left empty for manual TA onboarding tests)
-- =====================================================

-- =====================================================
-- 6. SECTION ENROLLMENTS
-- =====================================================

INSERT INTO section_enrollments (section_id, user_id, is_active, enrolled_at) VALUES
-- CS102-01 (2025 S1) enrollments
((SELECT id FROM sections WHERE section_code = 'CS102-01' AND year = 2025 AND semester = 1 LIMIT 1), (SELECT id FROM users WHERE email = 'student1@smu.edu.sg'), true, CURRENT_TIMESTAMP),
((SELECT id FROM sections WHERE section_code = 'CS102-01' AND year = 2025 AND semester = 1 LIMIT 1), (SELECT id FROM users WHERE email = 'student2@smu.edu.sg'), true, CURRENT_TIMESTAMP),

-- CS102-01 (2025 S2) enrollments
((SELECT id FROM sections WHERE section_code = 'CS102-01' AND year = 2025 AND semester = 2 LIMIT 1), (SELECT id FROM users WHERE email = 'student2@smu.edu.sg'), true, CURRENT_TIMESTAMP),
((SELECT id FROM sections WHERE section_code = 'CS102-01' AND year = 2025 AND semester = 2 LIMIT 1), (SELECT id FROM users WHERE email = 'student3@smu.edu.sg'), true, CURRENT_TIMESTAMP),

-- CS102-02 (2025 S1) enrollments
((SELECT id FROM sections WHERE section_code = 'CS102-02' AND year = 2025 AND semester = 1 LIMIT 1), (SELECT id FROM users WHERE email = 'student1@smu.edu.sg'), true, CURRENT_TIMESTAMP),
((SELECT id FROM sections WHERE section_code = 'CS102-02' AND year = 2025 AND semester = 1 LIMIT 1), (SELECT id FROM users WHERE email = 'student3@smu.edu.sg'), true, CURRENT_TIMESTAMP),

-- CS201-01 enrollments
((SELECT id FROM sections WHERE section_code = 'CS201-01' AND year = 2025 AND semester = 1 LIMIT 1), (SELECT id FROM users WHERE email = 'student1@smu.edu.sg'), true, CURRENT_TIMESTAMP),
((SELECT id FROM sections WHERE section_code = 'CS201-01' AND year = 2026 AND semester = 1 LIMIT 1), (SELECT id FROM users WHERE email = 'student2@smu.edu.sg'), true, CURRENT_TIMESTAMP),

-- CS301-01 enrollments
((SELECT id FROM sections WHERE section_code = 'CS301-01' AND year = 2025 AND semester = 2 LIMIT 1), (SELECT id FROM users WHERE email = 'student1@smu.edu.sg'), true, CURRENT_TIMESTAMP),
((SELECT id FROM sections WHERE section_code = 'CS301-01' AND year = 2025 AND semester = 2 LIMIT 1), (SELECT id FROM users WHERE email = 'student2@smu.edu.sg'), true, CURRENT_TIMESTAMP);

-- =====================================================
-- 7. ATTENDANCE SESSIONS
-- =====================================================

INSERT INTO attendance_sessions (section_id, session_date, scheduled_start_time, scheduled_end_time, status, notes, created_at) VALUES
-- CS102-01 (2025 S1) sessions - Mix of past and upcoming
((SELECT id FROM sections WHERE section_code = 'CS102-01' AND year = 2025 AND semester = 1 LIMIT 1), DATE '2025-10-07', '09:00:00', '10:30:00', 'COMPLETED', 'Week 1 lecture', CURRENT_TIMESTAMP),
((SELECT id FROM sections WHERE section_code = 'CS102-01' AND year = 2025 AND semester = 1 LIMIT 1), DATE '2025-10-14', '09:00:00', '10:30:00', 'COMPLETED', 'Week 2 lecture', CURRENT_TIMESTAMP),
((SELECT id FROM sections WHERE section_code = 'CS102-01' AND year = 2025 AND semester = 1 LIMIT 1), DATE '2025-10-21', '09:00:00', '10:30:00', 'IN_PROGRESS', 'Week 3 lecture', CURRENT_TIMESTAMP),
((SELECT id FROM sections WHERE section_code = 'CS102-01' AND year = 2025 AND semester = 1 LIMIT 1), DATE '2025-10-28', '09:00:00', '10:30:00', 'SCHEDULED', 'Week 4 lecture', CURRENT_TIMESTAMP),
((SELECT id FROM sections WHERE section_code = 'CS102-01' AND year = 2025 AND semester = 1 LIMIT 1), DATE '2025-11-04', '09:00:00', '10:30:00', 'SCHEDULED', 'Week 5 lecture', CURRENT_TIMESTAMP),

-- CS102-02 (2025 S1) sessions
((SELECT id FROM sections WHERE section_code = 'CS102-02' AND year = 2025 AND semester = 1 LIMIT 1), DATE '2025-10-10', '15:00:00', '16:30:00', 'COMPLETED', 'Week 1 lab', CURRENT_TIMESTAMP),
((SELECT id FROM sections WHERE section_code = 'CS102-02' AND year = 2025 AND semester = 1 LIMIT 1), DATE '2025-10-17', '15:00:00', '16:30:00', 'COMPLETED', 'Week 2 lab', CURRENT_TIMESTAMP),
((SELECT id FROM sections WHERE section_code = 'CS102-02' AND year = 2025 AND semester = 1 LIMIT 1), DATE '2025-10-24', '15:00:00', '16:30:00', 'SCHEDULED', 'Week 3 lab', CURRENT_TIMESTAMP),
((SELECT id FROM sections WHERE section_code = 'CS102-02' AND year = 2025 AND semester = 1 LIMIT 1), DATE '2025-10-31', '15:00:00', '16:30:00', 'SCHEDULED', 'Week 4 lab', CURRENT_TIMESTAMP),

-- CS102-01 (2025 S2) sessions
((SELECT id FROM sections WHERE section_code = 'CS102-01' AND year = 2025 AND semester = 2 LIMIT 1), DATE '2025-06-04', '13:00:00', '14:30:00', 'COMPLETED', 'Week 1 lecture', CURRENT_TIMESTAMP),
((SELECT id FROM sections WHERE section_code = 'CS102-01' AND year = 2025 AND semester = 2 LIMIT 1), DATE '2025-06-11', '13:00:00', '14:30:00', 'COMPLETED', 'Week 2 lecture', CURRENT_TIMESTAMP);

-- =====================================================
-- 8. ATTENDANCE RECORDS
-- =====================================================

INSERT INTO attendance_records (session_id, user_id, status, checkin_time, checkout_time, notes, created_at) VALUES
-- CS102-01 (2025 S1) Week 1 attendance (Oct 7 - COMPLETED)
((SELECT id FROM attendance_sessions WHERE session_date = DATE '2025-10-07' LIMIT 1),
 (SELECT id FROM users WHERE email = 'student1@smu.edu.sg'), 'PRESENT', '2025-10-07 08:55:00', '2025-10-07 10:32:00', NULL, CURRENT_TIMESTAMP),

((SELECT id FROM attendance_sessions WHERE session_date = DATE '2025-10-07' LIMIT 1),
 (SELECT id FROM users WHERE email = 'student2@smu.edu.sg'), 'PRESENT', '2025-10-07 09:02:00', '2025-10-07 10:30:00', NULL, CURRENT_TIMESTAMP),

-- CS102-01 (2025 S1) Week 2 attendance (Oct 14 - COMPLETED)
((SELECT id FROM attendance_sessions WHERE session_date = DATE '2025-10-14' LIMIT 1),
 (SELECT id FROM users WHERE email = 'student1@smu.edu.sg'), 'PRESENT', '2025-10-14 08:58:00', '2025-10-14 10:31:00', NULL, CURRENT_TIMESTAMP),

((SELECT id FROM attendance_sessions WHERE session_date = DATE '2025-10-14' LIMIT 1),
 (SELECT id FROM users WHERE email = 'student2@smu.edu.sg'), 'LATE', '2025-10-14 09:18:00', '2025-10-14 10:30:00', 'Traffic delay', CURRENT_TIMESTAMP),

-- CS102-01 (2025 S1) Week 3 attendance (Oct 21 - IN_PROGRESS, partial attendance)
((SELECT id FROM attendance_sessions WHERE session_date = DATE '2025-10-21' LIMIT 1),
 (SELECT id FROM users WHERE email = 'student1@smu.edu.sg'), 'PRESENT', '2025-10-21 08:56:00', NULL, NULL, CURRENT_TIMESTAMP),

-- CS102-02 (2025 S1) Week 1 attendance (Oct 10 - COMPLETED)
((SELECT id FROM attendance_sessions WHERE session_date = DATE '2025-10-10' LIMIT 1),
 (SELECT id FROM users WHERE email = 'student1@smu.edu.sg'), 'PRESENT', '2025-10-10 14:58:00', '2025-10-10 16:28:00', NULL, CURRENT_TIMESTAMP),

((SELECT id FROM attendance_sessions WHERE session_date = DATE '2025-10-10' LIMIT 1),
 (SELECT id FROM users WHERE email = 'student3@smu.edu.sg'), 'PRESENT', '2025-10-10 15:02:00', '2025-10-10 16:30:00', NULL, CURRENT_TIMESTAMP),

-- CS102-02 (2025 S1) Week 2 attendance (Oct 17 - COMPLETED)
((SELECT id FROM attendance_sessions WHERE session_date = DATE '2025-10-17' LIMIT 1),
 (SELECT id FROM users WHERE email = 'student1@smu.edu.sg'), 'LATE', '2025-10-17 15:17:00', '2025-10-17 16:28:00', 'Appointment ran late', CURRENT_TIMESTAMP),

((SELECT id FROM attendance_sessions WHERE session_date = DATE '2025-10-17' LIMIT 1),
 (SELECT id FROM users WHERE email = 'student3@smu.edu.sg'), 'ABSENT', NULL, NULL, 'Medical certificate provided', CURRENT_TIMESTAMP),

-- CS102-01 (2025 S2) Past sessions (June - COMPLETED)
((SELECT id FROM attendance_sessions WHERE session_date = DATE '2025-06-04' LIMIT 1),
 (SELECT id FROM users WHERE email = 'student2@smu.edu.sg'), 'PRESENT', '2025-06-04 12:58:00', '2025-06-04 14:32:00', NULL, CURRENT_TIMESTAMP),

((SELECT id FROM attendance_sessions WHERE session_date = DATE '2025-06-04' LIMIT 1),
 (SELECT id FROM users WHERE email = 'student3@smu.edu.sg'), 'PRESENT', '2025-06-04 13:01:00', '2025-06-04 14:30:00', NULL, CURRENT_TIMESTAMP),

((SELECT id FROM attendance_sessions WHERE session_date = DATE '2025-06-11' LIMIT 1),
 (SELECT id FROM users WHERE email = 'student2@smu.edu.sg'), 'PRESENT', '2025-06-11 12:55:00', '2025-06-11 14:28:00', NULL, CURRENT_TIMESTAMP),

((SELECT id FROM attendance_sessions WHERE session_date = DATE '2025-06-11' LIMIT 1),
 (SELECT id FROM users WHERE email = 'student3@smu.edu.sg'), 'LATE', '2025-06-11 13:16:00', '2025-06-11 14:30:00', 'Bus was late', CURRENT_TIMESTAMP);

-- =====================================================
-- VERIFICATION
-- =====================================================

DO $$
BEGIN
    RAISE NOTICE 'Simplified test data loaded successfully!';
    RAISE NOTICE '';
    RAISE NOTICE '=== USERS SUMMARY ===';
    RAISE NOTICE '- Total users: %', (SELECT COUNT(*) FROM users);
    RAISE NOTICE '- Students: %', (SELECT COUNT(*) FROM users WHERE is_student = true);
    RAISE NOTICE '- Instructors: %', (SELECT COUNT(*) FROM users WHERE is_instructor = true);
    RAISE NOTICE '- Teaching Assistants: %', (SELECT COUNT(*) FROM users WHERE is_ta = true);
    RAISE NOTICE '';
    RAISE NOTICE '=== COURSES & SECTIONS ===';
    RAISE NOTICE '- Total courses: %', (SELECT COUNT(*) FROM courses);
    RAISE NOTICE '- Total sections: %', (SELECT COUNT(*) FROM sections);
    RAISE NOTICE '';
    RAISE NOTICE '=== ASSIGNMENTS ===';
    RAISE NOTICE '- TA assignments (new system): %', (SELECT COUNT(*) FROM ta_assignments);
    RAISE NOTICE '- Section assignments (legacy): %', (SELECT COUNT(*) FROM section_assignments);
    RAISE NOTICE '- Section enrollments: %', (SELECT COUNT(*) FROM section_enrollments);
    RAISE NOTICE '';
    RAISE NOTICE '=== ATTENDANCE ===';
    RAISE NOTICE '- Attendance sessions: %', (SELECT COUNT(*) FROM attendance_sessions);
    RAISE NOTICE '- Attendance records: %', (SELECT COUNT(*) FROM attendance_records);
END $$;
