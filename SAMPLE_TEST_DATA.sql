-- SAMPLE TEST DATA for Student Attendance View
-- Run this in Supabase SQL Editor to test the attendance view
-- This creates a professor, course, enrolls you, creates sessions, and attendance records

-- ============================================
-- STEP 1: Create a Professor
-- ============================================

-- First, we need to create a professor account
-- Note: Replace the database_id with a valid UUID from your auth.users table
-- or use a random UUID for testing

INSERT INTO profiles (user_id, database_id, email, name, role)
VALUES (
    'P00001',
    gen_random_uuid(),  -- This will create a random UUID
    'professor@test.com',
    'Dr. Smith',
    'PROFESSOR'
) ON CONFLICT (user_id) DO NOTHING;

-- ============================================
-- STEP 2: Create a Course
-- ============================================

INSERT INTO courses (course, section, professor_id, semester)
VALUES
    ('CS102', 'A', 'P00001', 'Fall 2024'),
    ('CS201', 'B', 'P00001', 'Fall 2024'),
    ('MATH101', 'A', 'P00001', 'Fall 2024')
ON CONFLICT (course, section) DO NOTHING;

-- ============================================
-- STEP 3: Enroll the Student (YOU!)
-- ============================================

-- Replace 'S12345' with YOUR actual student ID from registration
-- You can find it by running: SELECT user_id FROM profiles WHERE email = 'zhuoyu@gmail.com';

DO $$
DECLARE
    student_id VARCHAR(20);
BEGIN
    -- Get your student ID based on your email
    SELECT user_id INTO student_id
    FROM profiles
    WHERE email = 'zhuoyu@gmail.com'  -- Change this to YOUR email
    LIMIT 1;

    IF student_id IS NOT NULL THEN
        -- Enroll you in the courses
        INSERT INTO classes (course, section, user_id)
        VALUES
            ('CS102', 'A', student_id),
            ('CS201', 'B', student_id),
            ('MATH101', 'A', student_id)
        ON CONFLICT DO NOTHING;

        RAISE NOTICE 'Student % enrolled in courses', student_id;
    ELSE
        RAISE NOTICE 'Student not found with that email!';
    END IF;
END $$;

-- ============================================
-- STEP 4: Create Sessions (13 weeks for CS102)
-- ============================================

-- Create 13 weekly sessions for CS102-A (starting from today and going back)
INSERT INTO sessions (session_id, course, section, date, start_time, end_time)
VALUES
    ('CS102-A-2024-10-29', 'CS102', 'A', '2024-10-29', '09:00:00', '10:30:00'),
    ('CS102-A-2024-10-22', 'CS102', 'A', '2024-10-22', '09:00:00', '10:30:00'),
    ('CS102-A-2024-10-15', 'CS102', 'A', '2024-10-15', '09:00:00', '10:30:00'),
    ('CS102-A-2024-10-08', 'CS102', 'A', '2024-10-08', '09:00:00', '10:30:00'),
    ('CS102-A-2024-10-01', 'CS102', 'A', '2024-10-01', '09:00:00', '10:30:00'),
    ('CS102-A-2024-09-24', 'CS102', 'A', '2024-09-24', '09:00:00', '10:30:00'),
    ('CS102-A-2024-09-17', 'CS102', 'A', '2024-09-17', '09:00:00', '10:30:00'),
    ('CS102-A-2024-09-10', 'CS102', 'A', '2024-09-10', '09:00:00', '10:30:00'),
    ('CS102-A-2024-09-03', 'CS102', 'A', '2024-09-03', '09:00:00', '10:30:00'),
    ('CS102-A-2024-08-27', 'CS102', 'A', '2024-08-27', '09:00:00', '10:30:00'),
    ('CS102-A-2024-08-20', 'CS102', 'A', '2024-08-20', '09:00:00', '10:30:00'),
    ('CS102-A-2024-08-13', 'CS102', 'A', '2024-08-13', '09:00:00', '10:30:00'),
    ('CS102-A-2024-08-06', 'CS102', 'A', '2024-08-06', '09:00:00', '10:30:00')
ON CONFLICT (session_id) DO NOTHING;

-- Create some sessions for CS201-B
INSERT INTO sessions (session_id, course, section, date, start_time, end_time)
VALUES
    ('CS201-B-2024-10-28', 'CS201', 'B', '2024-10-28', '14:00:00', '15:30:00'),
    ('CS201-B-2024-10-21', 'CS201', 'B', '2024-10-21', '14:00:00', '15:30:00'),
    ('CS201-B-2024-10-14', 'CS201', 'B', '2024-10-14', '14:00:00', '15:30:00')
ON CONFLICT (session_id) DO NOTHING;

-- ============================================
-- STEP 5: Manually Update Some Attendance Records
-- ============================================

-- The trigger should have auto-created attendance records
-- Now let's manually update some to show variety (Present, Late, Absent)

DO $$
DECLARE
    student_id VARCHAR(20);
    session_ids UUID[];
BEGIN
    -- Get your student ID
    SELECT user_id INTO student_id
    FROM profiles
    WHERE email = 'zhuoyu@gmail.com'  -- Change this to YOUR email
    LIMIT 1;

    -- Get all session IDs for CS102-A
    SELECT array_agg(id) INTO session_ids
    FROM sessions
    WHERE course = 'CS102' AND section = 'A';

    IF student_id IS NOT NULL AND session_ids IS NOT NULL THEN
        -- Mark some as Present (with check-in time)
        UPDATE attendance_records
        SET
            attendance = 'Present',
            checkin_time = (SELECT date + start_time FROM sessions WHERE id = session_id),
            method = 'Auto'
        WHERE user_id = student_id
          AND session_id = session_ids[1];

        UPDATE attendance_records
        SET
            attendance = 'Present',
            checkin_time = (SELECT date + start_time FROM sessions WHERE id = session_id),
            method = 'Auto'
        WHERE user_id = student_id
          AND session_id = session_ids[2];

        UPDATE attendance_records
        SET
            attendance = 'Present',
            checkin_time = (SELECT date + start_time FROM sessions WHERE id = session_id),
            method = 'Auto'
        WHERE user_id = student_id
          AND session_id = session_ids[3];

        -- Mark some as Late
        UPDATE attendance_records
        SET
            attendance = 'Late',
            checkin_time = (SELECT date + start_time + INTERVAL '10 minutes' FROM sessions WHERE id = session_id),
            method = 'Auto'
        WHERE user_id = student_id
          AND session_id = session_ids[4];

        UPDATE attendance_records
        SET
            attendance = 'Late',
            checkin_time = (SELECT date + start_time + INTERVAL '12 minutes' FROM sessions WHERE id = session_id),
            method = 'Auto'
        WHERE user_id = student_id
          AND session_id = session_ids[5];

        -- Leave the rest as Absent (default)

        RAISE NOTICE 'Updated attendance records for student %', student_id;
    ELSE
        RAISE NOTICE 'Could not find student or sessions';
    END IF;
END $$;

-- ============================================
-- STEP 6: Verify Data
-- ============================================

-- Check your enrollments
SELECT
    c.course,
    c.section,
    c.user_id,
    p.name,
    p.email
FROM classes c
JOIN profiles p ON c.user_id = p.user_id
WHERE p.email = 'zhuoyu@gmail.com';  -- Change to YOUR email

-- Check your attendance records for CS102
SELECT
    s.session_id,
    s.date,
    ar.attendance,
    ar.checkin_time,
    ar.method
FROM attendance_records ar
JOIN sessions s ON ar.session_id = s.id
JOIN profiles p ON ar.user_id = p.user_id
WHERE p.email = 'zhuoyu@gmail.com'  -- Change to YOUR email
  AND s.course = 'CS102'
ORDER BY s.date;

-- ============================================
-- SUCCESS MESSAGE
-- ============================================

DO $$
BEGIN
    RAISE NOTICE '✅ Sample data created successfully!';
    RAISE NOTICE '';
    RAISE NOTICE '📚 Courses created: CS102-A, CS201-B, MATH101-A';
    RAISE NOTICE '👨‍🎓 Student enrolled in all 3 courses';
    RAISE NOTICE '📅 13 sessions created for CS102-A';
    RAISE NOTICE '✓ Some attendance marked as Present/Late/Absent';
    RAISE NOTICE '';
    RAISE NOTICE '🎯 Now go back to the app and click "View My Attendance"!';
    RAISE NOTICE '   Select "CS102" from the dropdown to see your attendance.';
END $$;
