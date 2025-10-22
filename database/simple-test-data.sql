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
-- CS102-01 enrollments
((SELECT id FROM sections WHERE section_code = 'CS102-01'), (SELECT id FROM users WHERE email = 'student1@smu.edu.sg'), true, CURRENT_TIMESTAMP),
((SELECT id FROM sections WHERE section_code = 'CS102-01'), (SELECT id FROM users WHERE email = 'student2@smu.edu.sg'), true, CURRENT_TIMESTAMP),

-- CS102-02 enrollments
((SELECT id FROM sections WHERE section_code = 'CS102-02'), (SELECT id FROM users WHERE email = 'student2@smu.edu.sg'), true, CURRENT_TIMESTAMP),
((SELECT id FROM sections WHERE section_code = 'CS102-02'), (SELECT id FROM users WHERE email = 'student3@smu.edu.sg'), true, CURRENT_TIMESTAMP),

-- CS102-03 enrollments
((SELECT id FROM sections WHERE section_code = 'CS102-03'), (SELECT id FROM users WHERE email = 'student1@smu.edu.sg'), true, CURRENT_TIMESTAMP),
((SELECT id FROM sections WHERE section_code = 'CS102-03'), (SELECT id FROM users WHERE email = 'student3@smu.edu.sg'), true, CURRENT_TIMESTAMP),

-- CS201 enrollments
((SELECT id FROM sections WHERE section_code = 'CS201-01'), (SELECT id FROM users WHERE email = 'student1@smu.edu.sg'), true, CURRENT_TIMESTAMP),
((SELECT id FROM sections WHERE section_code = 'CS201-02'), (SELECT id FROM users WHERE email = 'student2@smu.edu.sg'), true, CURRENT_TIMESTAMP),

-- CS301 enrollments
((SELECT id FROM sections WHERE section_code = 'CS301-01'), (SELECT id FROM users WHERE email = 'student1@smu.edu.sg'), true, CURRENT_TIMESTAMP),
((SELECT id FROM sections WHERE section_code = 'CS301-01'), (SELECT id FROM users WHERE email = 'student2@smu.edu.sg'), true, CURRENT_TIMESTAMP);

-- =====================================================
-- 7. ATTENDANCE SESSIONS
-- =====================================================

INSERT INTO attendance_sessions (section_id, session_date, scheduled_start_time, scheduled_end_time, status, notes, created_at) VALUES
-- CS102-01 sessions
((SELECT id FROM sections WHERE section_code = 'CS102-01'), DATE '2024-09-02', '09:00:00', '10:30:00', 'COMPLETED', 'Week 1 lecture', CURRENT_TIMESTAMP),
((SELECT id FROM sections WHERE section_code = 'CS102-01'), DATE '2024-09-09', '09:00:00', '10:30:00', 'COMPLETED', 'Week 2 lecture', CURRENT_TIMESTAMP),
((SELECT id FROM sections WHERE section_code = 'CS102-01'), DATE '2024-09-16', '09:00:00', '10:30:00', 'COMPLETED', 'Week 3 lecture', CURRENT_TIMESTAMP),

-- CS102-02 sessions
((SELECT id FROM sections WHERE section_code = 'CS102-02'), DATE '2024-09-03', '13:00:00', '14:30:00', 'COMPLETED', 'Week 1 lab', CURRENT_TIMESTAMP),
((SELECT id FROM sections WHERE section_code = 'CS102-02'), DATE '2024-09-10', '13:00:00', '14:30:00', 'COMPLETED', 'Week 2 lab', CURRENT_TIMESTAMP),

-- CS102-03 sessions
((SELECT id FROM sections WHERE section_code = 'CS102-03'), DATE '2024-09-06', '15:00:00', '16:30:00', 'COMPLETED', 'Week 1 tutorial', CURRENT_TIMESTAMP);

-- =====================================================
-- 8. ATTENDANCE RECORDS
-- =====================================================

INSERT INTO attendance_records (session_id, user_id, status, checkin_time, checkout_time, notes, created_at) VALUES
-- CS102-01 attendance
((SELECT id FROM attendance_sessions WHERE session_date = DATE '2024-09-02' AND section_id = (SELECT id FROM sections WHERE section_code = 'CS102-01')),
 (SELECT id FROM users WHERE email = 'student1@smu.edu.sg'), 'PRESENT', '2024-09-02 08:55:00', '2024-09-02 10:32:00', 'Arrived early to assist as TA.', CURRENT_TIMESTAMP),

((SELECT id FROM attendance_sessions WHERE session_date = DATE '2024-09-02' AND section_id = (SELECT id FROM sections WHERE section_code = 'CS102-01')),
 (SELECT id FROM users WHERE email = 'student2@smu.edu.sg'), 'PRESENT', '2024-09-02 09:05:00', '2024-09-02 10:30:00', NULL, CURRENT_TIMESTAMP),

((SELECT id FROM attendance_sessions WHERE session_date = DATE '2024-09-09' AND section_id = (SELECT id FROM sections WHERE section_code = 'CS102-01')),
 (SELECT id FROM users WHERE email = 'student1@smu.edu.sg'), 'PRESENT', '2024-09-09 08:52:00', '2024-09-09 10:31:00', 'Led lab check-ins as TA.', CURRENT_TIMESTAMP),

((SELECT id FROM attendance_sessions WHERE session_date = DATE '2024-09-09' AND section_id = (SELECT id FROM sections WHERE section_code = 'CS102-01')),
 (SELECT id FROM users WHERE email = 'student2@smu.edu.sg'), 'ABSENT', NULL, NULL, 'Notified instructor of illness.', CURRENT_TIMESTAMP),

-- CS102-02 attendance
((SELECT id FROM attendance_sessions WHERE session_date = DATE '2024-09-03' AND section_id = (SELECT id FROM sections WHERE section_code = 'CS102-02')),
 (SELECT id FROM users WHERE email = 'student3@smu.edu.sg'), 'PRESENT', '2024-09-03 12:58:00', '2024-09-03 14:28:00', 'Assisted with lab setup as TA.', CURRENT_TIMESTAMP),

((SELECT id FROM attendance_sessions WHERE session_date = DATE '2024-09-03' AND section_id = (SELECT id FROM sections WHERE section_code = 'CS102-02')),
 (SELECT id FROM users WHERE email = 'student2@smu.edu.sg'), 'PRESENT', '2024-09-03 13:02:00', '2024-09-03 14:30:00', NULL, CURRENT_TIMESTAMP),

-- CS102-03 attendance
((SELECT id FROM attendance_sessions WHERE session_date = DATE '2024-09-06' AND section_id = (SELECT id FROM sections WHERE section_code = 'CS102-03')),
 (SELECT id FROM users WHERE email = 'student1@smu.edu.sg'), 'PRESENT', '2024-09-06 14:55:00', '2024-09-06 16:32:00', 'Led tutorial session as TA.', CURRENT_TIMESTAMP),

((SELECT id FROM attendance_sessions WHERE session_date = DATE '2024-09-06' AND section_id = (SELECT id FROM sections WHERE section_code = 'CS102-03')),
 (SELECT id FROM users WHERE email = 'student3@smu.edu.sg'), 'LATE', '2024-09-06 15:10:00', '2024-09-06 16:30:00', 'Traffic delay', CURRENT_TIMESTAMP);

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
