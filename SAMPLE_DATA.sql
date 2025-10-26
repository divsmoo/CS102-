-- SAMPLE DATA FOR TESTING
-- This creates test data linked to professor user_id = 'P12345'
-- Run this AFTER running FINAL_SCHEMA.sql

-- ============================================
-- STEP 1: CREATE COURSES
-- ============================================
-- Professor P12345 teaches CS102 and CS103

INSERT INTO courses (course, section, professor_id, semester) VALUES
('CS102', 'A', '12345', '2025-Spring'),
('CS102', 'B', '12345', '2025-Spring'),
('CS103', 'A', '12345', '2025-Spring');

-- ============================================
-- STEP 2: CREATE STUDENT ENROLLMENTS
-- ============================================
-- Create 15 students enrolled in CS102-A
INSERT INTO classes (course, section, user_id) VALUES
('CS102', 'A', 's10001'),
('CS102', 'A', 's10002'),
('CS102', 'A', 's10003'),
('CS102', 'A', 's10004'),
('CS102', 'A', 's10005'),
('CS102', 'A', 's10006'),
('CS102', 'A', 's10007'),
('CS102', 'A', 's10008'),
('CS102', 'A', 's10009'),
('CS102', 'A', 's10010'),
('CS102', 'A', 's10011'),
('CS102', 'A', 's10012'),
('CS102', 'A', 's10013'),
('CS102', 'A', 's10014'),
('CS102', 'A', 's10015');

-- Create 10 students enrolled in CS102-B
INSERT INTO classes (course, section, user_id) VALUES
('CS102', 'B', 's10016'),
('CS102', 'B', 's10017'),
('CS102', 'B', 's10018'),
('CS102', 'B', 's10019'),
('CS102', 'B', 's10020'),
('CS102', 'B', 's10021'),
('CS102', 'B', 's10022'),
('CS102', 'B', 's10023'),
('CS102', 'B', 's10024'),
('CS102', 'B', 's10025');

-- Create 8 students enrolled in CS103-A
INSERT INTO classes (course, section, user_id) VALUES
('CS103', 'A', 's10001'),  -- Same student in multiple courses
('CS103', 'A', 's10002'),
('CS103', 'A', 's10026'),
('CS103', 'A', 's10027'),
('CS103', 'A', 's10028'),
('CS103', 'A', 's10029'),
('CS103', 'A', 's10030'),
('CS103', 'A', 's10031');

-- ============================================
-- STEP 3: CREATE STUDENT PROFILES
-- ============================================
-- NOTE: You'll need to create auth.users entries first via Supabase Auth UI
-- or by registering through the app. For testing, we'll create profiles without auth.

-- These students need to exist in auth.users first
-- For now, we'll skip this and just create the enrollments above
-- When you register students through the app, use these IDs

-- ============================================
-- STEP 4: CREATE SESSIONS FOR CS102-A
-- ============================================

INSERT INTO sessions (session_id, course, section, date, start_time, end_time) VALUES
('CS102-A-2025-01-15', 'CS102', 'A', '2025-01-15', '09:00:00', '10:30:00'),
('CS102-A-2025-01-17', 'CS102', 'A', '2025-01-17', '09:00:00', '10:30:00'),
('CS102-A-2025-01-22', 'CS102', 'A', '2025-01-22', '09:00:00', '10:30:00'),
('CS102-A-2025-01-24', 'CS102', 'A', '2025-01-24', '09:00:00', '10:30:00'),
('CS102-A-2025-01-29', 'CS102', 'A', '2025-01-29', '09:00:00', '10:30:00');

-- ============================================
-- STEP 5: CREATE SESSIONS FOR CS102-B
-- ============================================

INSERT INTO sessions (session_id, course, section, date, start_time, end_time) VALUES
('CS102-B-2025-01-16', 'CS102', 'B', '2025-01-16', '14:00:00', '15:30:00'),
('CS102-B-2025-01-18', 'CS102', 'B', '2025-01-18', '14:00:00', '15:30:00'),
('CS102-B-2025-01-23', 'CS102', 'B', '2025-01-23', '14:00:00', '15:30:00');

-- ============================================
-- STEP 6: CREATE SESSIONS FOR CS103-A
-- ============================================

INSERT INTO sessions (session_id, course, section, date, start_time, end_time) VALUES
('CS103-A-2025-01-15', 'CS103', 'A', '2025-01-15', '11:00:00', '12:30:00'),
('CS103-A-2025-01-17', 'CS103', 'A', '2025-01-17', '11:00:00', '12:30:00'),
('CS103-A-2025-01-22', 'CS103', 'A', '2025-01-22', '11:00:00', '12:30:00');

-- ============================================
-- STEP 7: SIMULATE ATTENDANCE FOR CS102-A SESSION 1
-- ============================================
-- The trigger will auto-create attendance records with status='Absent'
-- We'll update them to simulate students checking in

-- Get the session UUID for CS102-A-2025-01-15
DO $$
DECLARE
    session_uuid UUID;
BEGIN
    SELECT id INTO session_uuid FROM sessions WHERE session_id = 'CS102-A-2025-01-15';

    -- Students who were PRESENT (checked in before 09:00)
    UPDATE attendance_records
    SET checkin_time = '2025-01-15 08:55:00+00'
    WHERE session_id = session_uuid AND user_id IN ('s10001', 's10002', 's10003', 's10004', 's10005', 's10006', 's10007', 's10008', 's10009', 's10010');

    -- Students who were LATE (checked in 09:01 - 09:15)
    UPDATE attendance_records
    SET checkin_time = '2025-01-15 09:08:00+00'
    WHERE session_id = session_uuid AND user_id IN ('s10011', 's10012', 's10013');

    -- Students who were ABSENT (s10014, s10015 never checked in)
    -- No update needed, they remain Absent
END $$;

-- ============================================
-- STEP 8: SIMULATE ATTENDANCE FOR CS102-A SESSION 2
-- ============================================
DO $$
DECLARE
    session_uuid UUID;
BEGIN
    SELECT id INTO session_uuid FROM sessions WHERE session_id = 'CS102-A-2025-01-17';

    -- Present
    UPDATE attendance_records
    SET checkin_time = '2025-01-17 08:57:00+00'
    WHERE session_id = session_uuid AND user_id IN ('s10001', 's10002', 's10003', 's10004', 's10005', 's10006', 's10007', 's10008', 's10011');

    -- Late
    UPDATE attendance_records
    SET checkin_time = '2025-01-17 09:10:00+00'
    WHERE session_id = session_uuid AND user_id IN ('s10009', 's10010', 's10012', 's10014');

    -- Absent: s10013, s10015
END $$;

-- ============================================
-- STEP 9: SIMULATE ATTENDANCE FOR CS102-A SESSION 3
-- ============================================
DO $$
DECLARE
    session_uuid UUID;
BEGIN
    SELECT id INTO session_uuid FROM sessions WHERE session_id = 'CS102-A-2025-01-22';

    -- Present
    UPDATE attendance_records
    SET checkin_time = '2025-01-22 08:58:00+00'
    WHERE session_id = session_uuid AND user_id IN ('s10001', 's10003', 's10005', 's10007', 's10009', 's10011', 's10013', 's10015');

    -- Late
    UPDATE attendance_records
    SET checkin_time = '2025-01-22 09:05:00+00'
    WHERE session_id = session_uuid AND user_id IN ('s10002', 's10004', 's10006');

    -- Absent: s10008, s10010, s10012, s10014
END $$;

-- ============================================
-- STEP 10: SIMULATE ATTENDANCE FOR CS102-A SESSION 4
-- ============================================
DO $$
DECLARE
    session_uuid UUID;
BEGIN
    SELECT id INTO session_uuid FROM sessions WHERE session_id = 'CS102-A-2025-01-24';

    -- Present
    UPDATE attendance_records
    SET checkin_time = '2025-01-24 08:59:00+00'
    WHERE session_id = session_uuid AND user_id IN ('s10001', 's10002', 's10003', 's10004', 's10005', 's10006', 's10007', 's10008', 's10009', 's10010', 's10011', 's10012');

    -- Late
    UPDATE attendance_records
    SET checkin_time = '2025-01-24 09:12:00+00'
    WHERE session_id = session_uuid AND user_id IN ('s10013');

    -- Absent: s10014, s10015
END $$;

-- ============================================
-- STEP 11: SIMULATE ATTENDANCE FOR CS102-A SESSION 5
-- ============================================
DO $$
DECLARE
    session_uuid UUID;
BEGIN
    SELECT id INTO session_uuid FROM sessions WHERE session_id = 'CS102-A-2025-01-29';

    -- Present (everyone showed up!)
    UPDATE attendance_records
    SET checkin_time = '2025-01-29 08:55:00+00'
    WHERE session_id = session_uuid AND user_id IN ('s10001', 's10002', 's10003', 's10004', 's10005', 's10006', 's10007', 's10008', 's10009', 's10010', 's10011', 's10012', 's10013', 's10014', 's10015');
END $$;

-- ============================================
-- STEP 12: SIMULATE ATTENDANCE FOR CS102-B
-- ============================================
DO $$
DECLARE
    session_uuid UUID;
BEGIN
    -- Session 1
    SELECT id INTO session_uuid FROM sessions WHERE session_id = 'CS102-B-2025-01-16';
    UPDATE attendance_records
    SET checkin_time = '2025-01-16 13:58:00+00'
    WHERE session_id = session_uuid AND user_id IN ('s10016', 's10017', 's10018', 's10019', 's10020');

    UPDATE attendance_records
    SET checkin_time = '2025-01-16 14:07:00+00'
    WHERE session_id = session_uuid AND user_id IN ('s10021', 's10022');
    -- s10023, s10024, s10025 absent

    -- Session 2
    SELECT id INTO session_uuid FROM sessions WHERE session_id = 'CS102-B-2025-01-18';
    UPDATE attendance_records
    SET checkin_time = '2025-01-18 13:55:00+00'
    WHERE session_id = session_uuid AND user_id IN ('s10016', 's10017', 's10018', 's10019', 's10020', 's10021');

    UPDATE attendance_records
    SET checkin_time = '2025-01-18 14:10:00+00'
    WHERE session_id = session_uuid AND user_id IN ('s10024');
    -- s10022, s10023, s10025 absent

    -- Session 3
    SELECT id INTO session_uuid FROM sessions WHERE session_id = 'CS102-B-2025-01-23';
    UPDATE attendance_records
    SET checkin_time = '2025-01-23 13:59:00+00'
    WHERE session_id = session_uuid AND user_id IN ('s10016', 's10017', 's10018', 's10019', 's10020', 's10021', 's10022', 's10023', 's10024', 's10025');
END $$;

-- ============================================
-- STEP 13: SIMULATE ATTENDANCE FOR CS103-A
-- ============================================
DO $$
DECLARE
    session_uuid UUID;
BEGIN
    -- Session 1
    SELECT id INTO session_uuid FROM sessions WHERE session_id = 'CS103-A-2025-01-15';
    UPDATE attendance_records
    SET checkin_time = '2025-01-15 10:58:00+00'
    WHERE session_id = session_uuid AND user_id IN ('s10001', 's10002', 's10026', 's10027');

    UPDATE attendance_records
    SET checkin_time = '2025-01-15 11:08:00+00'
    WHERE session_id = session_uuid AND user_id IN ('s10028', 's10029');
    -- s10030, s10031 absent

    -- Session 2
    SELECT id INTO session_uuid FROM sessions WHERE session_id = 'CS103-A-2025-01-17';
    UPDATE attendance_records
    SET checkin_time = '2025-01-17 10:55:00+00'
    WHERE session_id = session_uuid AND user_id IN ('s10001', 's10002', 's10026', 's10027', 's10028', 's10029', 's10030');
    -- s10031 absent

    -- Session 3
    SELECT id INTO session_uuid FROM sessions WHERE session_id = 'CS103-A-2025-01-22';
    UPDATE attendance_records
    SET checkin_time = '2025-01-22 10:57:00+00'
    WHERE session_id = session_uuid AND user_id IN ('s10001', 's10002', 's10026', 's10027', 's10028', 's10029', 's10030', 's10031');
END $$;

-- ============================================
-- VERIFICATION QUERIES
-- ============================================

-- Check courses
SELECT * FROM courses WHERE professor_id = '12345';

-- Check enrollments for CS102-A
SELECT * FROM classes WHERE course = 'CS102' AND section = 'A';

-- Check sessions
SELECT * FROM sessions ORDER BY date, start_time;

-- Check attendance summary for CS102-A
SELECT
    ar.user_id,
    s.session_id,
    ar.attendance,
    ar.checkin_time,
    ar.method
FROM attendance_records ar
INNER JOIN sessions s ON ar.session_id = s.id
WHERE s.course = 'CS102' AND s.section = 'A'
ORDER BY ar.user_id, s.date;

-- Count attendance by status for CS102-A
SELECT
    ar.user_id,
    COUNT(CASE WHEN ar.attendance = 'Present' THEN 1 END) as total_present,
    COUNT(CASE WHEN ar.attendance = 'Late' THEN 1 END) as total_late,
    COUNT(CASE WHEN ar.attendance = 'Absent' THEN 1 END) as total_absent
FROM attendance_records ar
INNER JOIN sessions s ON ar.session_id = s.id
WHERE s.course = 'CS102' AND s.section = 'A'
GROUP BY ar.user_id
ORDER BY ar.user_id;

-- ============================================
-- SUCCESS MESSAGE
-- ============================================

DO $$
BEGIN
    RAISE NOTICE '✅ Sample data created successfully!';
    RAISE NOTICE '';
    RAISE NOTICE '📊 Summary:';
    RAISE NOTICE '- Professor: P12345';
    RAISE NOTICE '- Courses: CS102 (sections A, B), CS103 (section A)';
    RAISE NOTICE '- Students: 31 total (15 in CS102-A, 10 in CS102-B, 8 in CS103-A)';
    RAISE NOTICE '- Sessions: 11 total (5 for CS102-A, 3 for CS102-B, 3 for CS103-A)';
    RAISE NOTICE '- Attendance records: Auto-created by trigger';
    RAISE NOTICE '';
    RAISE NOTICE '⚠️  NOTE: Student profiles need to be created!';
    RAISE NOTICE 'Students s10001-s10031 need profiles in the profiles table.';
    RAISE NOTICE 'You can either:';
    RAISE NOTICE '1. Register them through the app (recommended)';
    RAISE NOTICE '2. Create fake profiles manually (see next section)';
END $$;

-- ============================================
-- OPTIONAL: CREATE FAKE STUDENT PROFILES
-- ============================================
-- WARNING: This creates profiles WITHOUT auth.users entries
-- This will cause login issues. Only use for testing the attendance table.

-- First, you need to create dummy auth.users entries (this won't work without proper Supabase Auth)
-- For now, we'll just show what the INSERT would look like:

/*
INSERT INTO profiles (user_id, database_id, email, name, role) VALUES
('s10001', gen_random_uuid(), 's10001@example.com', 'Alice Johnson', 'STUDENT'),
('s10002', gen_random_uuid(), 's10002@example.com', 'Bob Smith', 'STUDENT'),
('s10003', gen_random_uuid(), 's10003@example.com', 'Carol White', 'STUDENT'),
('s10004', gen_random_uuid(), 's10004@example.com', 'David Brown', 'STUDENT'),
('s10005', gen_random_uuid(), 's10005@example.com', 'Emma Davis', 'STUDENT'),
('s10006', gen_random_uuid(), 's10006@example.com', 'Frank Miller', 'STUDENT'),
('s10007', gen_random_uuid(), 's10007@example.com', 'Grace Wilson', 'STUDENT'),
('s10008', gen_random_uuid(), 's10008@example.com', 'Henry Moore', 'STUDENT'),
('s10009', gen_random_uuid(), 's10009@example.com', 'Ivy Taylor', 'STUDENT'),
('s10010', gen_random_uuid(), 's10010@example.com', 'Jack Anderson', 'STUDENT'),
('s10011', gen_random_uuid(), 's10011@example.com', 'Kate Thomas', 'STUDENT'),
('s10012', gen_random_uuid(), 's10012@example.com', 'Leo Jackson', 'STUDENT'),
('s10013', gen_random_uuid(), 's10013@example.com', 'Mia Harris', 'STUDENT'),
('s10014', gen_random_uuid(), 's10014@example.com', 'Noah Martin', 'STUDENT'),
('s10015', gen_random_uuid(), 's10015@example.com', 'Olivia Garcia', 'STUDENT'),
('s10016', gen_random_uuid(), 's10016@example.com', 'Paul Martinez', 'STUDENT'),
('s10017', gen_random_uuid(), 's10017@example.com', 'Quinn Robinson', 'STUDENT'),
('s10018', gen_random_uuid(), 's10018@example.com', 'Ruby Clark', 'STUDENT'),
('s10019', gen_random_uuid(), 's10019@example.com', 'Sam Rodriguez', 'STUDENT'),
('s10020', gen_random_uuid(), 's10020@example.com', 'Tina Lewis', 'STUDENT'),
('s10021', gen_random_uuid(), 's10021@example.com', 'Uma Lee', 'STUDENT'),
('s10022', gen_random_uuid(), 's10022@example.com', 'Victor Walker', 'STUDENT'),
('s10023', gen_random_uuid(), 's10023@example.com', 'Wendy Hall', 'STUDENT'),
('s10024', gen_random_uuid(), 's10024@example.com', 'Xander Allen', 'STUDENT'),
('s10025', gen_random_uuid(), 's10025@example.com', 'Yara Young', 'STUDENT'),
('s10026', gen_random_uuid(), 's10026@example.com', 'Zack King', 'STUDENT'),
('s10027', gen_random_uuid(), 's10027@example.com', 'Amy Wright', 'STUDENT'),
('s10028', gen_random_uuid(), 's10028@example.com', 'Ben Lopez', 'STUDENT'),
('s10029', gen_random_uuid(), 's10029@example.com', 'Chloe Hill', 'STUDENT'),
('s10030', gen_random_uuid(), 's10030@example.com', 'Dan Scott', 'STUDENT'),
('s10031', gen_random_uuid(), 's10031@example.com', 'Ella Green', 'STUDENT');
*/
