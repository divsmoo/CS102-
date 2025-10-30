-- SAMPLE TEST DATA for Student Attendance View
-- Run this in Supabase SQL Editor to test the attendance view
-- This creates courses, enrolls you, creates sessions, and attendance records

-- ============================================
-- STEP 1: Find your user_id
-- ============================================
-- Run this first to see your user_id
SELECT user_id, name, email, database_id FROM profiles WHERE email = 'yongzhuoyu@gmail.com';

-- You should see something like: user_id = '01487477'
-- Copy that user_id and replace 'YOUR_USER_ID' in steps below

-- ============================================
-- STEP 2: Create a test professor
-- ============================================
-- We'll use an existing auth user or skip if professor already exists

-- First check if professor exists
DO $$
DECLARE
    prof_exists BOOLEAN;
    random_auth_user_id UUID;
BEGIN
    -- Check if professor already exists (using correct ID with 4 zeros)
    SELECT EXISTS(SELECT 1 FROM profiles WHERE user_id = 'P0001') INTO prof_exists;

    IF NOT prof_exists THEN
        -- Get any existing auth user ID (could be yours) to use as foreign key
        SELECT id INTO random_auth_user_id FROM auth.users LIMIT 1;

        IF random_auth_user_id IS NOT NULL THEN
            -- Create professor with existing auth user ID
            INSERT INTO profiles (user_id, database_id, email, name, role)
            VALUES ('P0001', random_auth_user_id, 'professor@test.com', 'Dr. Smith', 'PROFESSOR')
            ON CONFLICT (database_id) DO NOTHING;

            RAISE NOTICE 'Created professor P0001';
        ELSE
            RAISE NOTICE 'No auth users found - skipping professor creation';
        END IF;
    ELSE
        RAISE NOTICE 'Professor P0001 already exists';
    END IF;
END $$;

-- ============================================
-- STEP 3: Create some courses
-- ============================================
INSERT INTO courses (course, section, professor_id, semester)
VALUES
    ('CS102', 'G1', 'P0001', '2024-2025 Term 1'),
    ('MATH101', 'G2', 'P0001', '2024-2025 Term 1'),
    ('CS201', 'G3', 'P0001', '2024-2025 Term 2')
ON CONFLICT (course, section) DO NOTHING;

-- ============================================
-- STEP 4: Enroll yourself in these courses
-- ============================================
-- IMPORTANT: Replace '01487477' with YOUR actual user_id from Step 1
INSERT INTO classes (course, section, user_id)
VALUES
    ('CS102', 'G1', '01487477'),    -- ⚠️ Replace with YOUR user_id
    ('MATH101', 'G2', '01487477'),  -- ⚠️ Replace with YOUR user_id
    ('CS201', 'G3', '01487477')     -- ⚠️ Replace with YOUR user_id
ON CONFLICT DO NOTHING;

-- ============================================
-- STEP 5: Create 13 sessions for CS102-G1
-- ============================================
INSERT INTO sessions (session_id, course, section, date, start_time, end_time)
VALUES
    ('CS102-G1-2024-10-29', 'CS102', 'G1', '2024-10-29', '09:00:00', '12:00:00'),
    ('CS102-G1-2024-10-22', 'CS102', 'G1', '2024-10-22', '09:00:00', '12:00:00'),
    ('CS102-G1-2024-10-15', 'CS102', 'G1', '2024-10-15', '09:00:00', '12:00:00'),
    ('CS102-G1-2024-10-08', 'CS102', 'G1', '2024-10-08', '09:00:00', '12:00:00'),
    ('CS102-G1-2024-10-01', 'CS102', 'G1', '2024-10-01', '09:00:00', '12:00:00'),
    ('CS102-G1-2024-09-24', 'CS102', 'G1', '2024-09-24', '09:00:00', '12:00:00'),
    ('CS102-G1-2024-09-17', 'CS102', 'G1', '2024-09-17', '09:00:00', '12:00:00'),
    ('CS102-G1-2024-09-10', 'CS102', 'G1', '2024-09-10', '09:00:00', '12:00:00'),
    ('CS102-G1-2024-09-03', 'CS102', 'G1', '2024-09-03', '09:00:00', '12:00:00'),
    ('CS102-G1-2024-08-27', 'CS102', 'G1', '2024-08-27', '09:00:00', '12:00:00'),
    ('CS102-G1-2024-08-20', 'CS102', 'G1', '2024-08-20', '09:00:00', '12:00:00'),
    ('CS102-G1-2024-08-13', 'CS102', 'G1', '2024-08-13', '09:00:00', '12:00:00'),
    ('CS102-G1-2024-08-06', 'CS102', 'G1', '2024-08-06', '09:00:00', '12:00:00')
ON CONFLICT (session_id) DO NOTHING;

-- Create a few sessions for MATH101-G2
INSERT INTO sessions (session_id, course, section, date, start_time, end_time)
VALUES
    ('MATH101-G2-2024-10-28', 'MATH101', 'G2', '2024-10-28', '14:00:00', '17:00:00'),
    ('MATH101-G2-2024-10-21', 'MATH101', 'G2', '2024-10-21', '14:00:00', '17:00:00'),
    ('MATH101-G2-2024-10-14', 'MATH101', 'G2', '2024-10-14', '14:00:00', '17:00:00')
ON CONFLICT (session_id) DO NOTHING;

-- ============================================
-- STEP 6: Create attendance records
-- ============================================
-- IMPORTANT: Replace '01487477' with YOUR actual user_id
DO $$
DECLARE
    session_ids UUID[];
    student_id VARCHAR(20) := '01487477'; -- ⚠️ Replace with YOUR user_id
BEGIN
    -- Get all session IDs for CS102-G1 ordered by date
    SELECT array_agg(id ORDER BY date) INTO session_ids
    FROM sessions
    WHERE course = 'CS102' AND section = 'G1';

    -- Create attendance records for all 13 sessions
    FOR i IN 1..13 LOOP
        IF session_ids[i] IS NOT NULL THEN
            INSERT INTO attendance_records (user_id, session_id, attendance, method, checkin_time)
            VALUES (
                student_id,
                session_ids[i],
                -- Week 1-3: Present, Week 4-5: Late, Week 6-13: Absent
                CASE
                    WHEN i <= 3 THEN 'Present'
                    WHEN i <= 5 THEN 'Late'
                    ELSE 'Absent'
                END,
                'Manual',
                -- Add check-in time for Present and Late statuses
                CASE
                    WHEN i <= 5 THEN (SELECT date + start_time FROM sessions WHERE id = session_ids[i])
                    ELSE NULL
                END
            )
            ON CONFLICT (user_id, session_id) DO NOTHING;
        END IF;
    END LOOP;

    RAISE NOTICE 'Created attendance records for % sessions', array_length(session_ids, 1);
END $$;

-- ============================================
-- STEP 7: Verify the data was created
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
WHERE c.user_id = '01487477';  -- ⚠️ Replace with YOUR user_id

-- Check your attendance records for CS102
SELECT
    s.session_id,
    s.date,
    s.start_time,
    s.end_time,
    ar.attendance,
    ar.checkin_time,
    ar.method
FROM attendance_records ar
JOIN sessions s ON ar.session_id = s.id
WHERE ar.user_id = '01487477'  -- ⚠️ Replace with YOUR user_id
  AND s.course = 'CS102'
ORDER BY s.date;

-- ============================================
-- SUCCESS MESSAGE
-- ============================================
DO $$
BEGIN
    RAISE NOTICE '✅ Sample data creation complete!';
    RAISE NOTICE '';
    RAISE NOTICE '📚 Courses created: CS102-G1, MATH101-G2, CS201-G3';
    RAISE NOTICE '   - CS102-G1 and MATH101-G2: 2024-2025 Term 1';
    RAISE NOTICE '   - CS201-G3: 2024-2025 Term 2';
    RAISE NOTICE '👨‍🎓 Student enrolled in all courses';
    RAISE NOTICE '📅 13 sessions created for CS102-G1 (3 hours each: 09:00-12:00)';
    RAISE NOTICE '📊 Attendance pattern:';
    RAISE NOTICE '   - Week 1-3: Present (P) - Green';
    RAISE NOTICE '   - Week 4-5: Late (L) - Yellow';
    RAISE NOTICE '   - Week 6-13: Absent (A) - Red';
    RAISE NOTICE '';
    RAISE NOTICE '🎯 Now go back to your app and:';
    RAISE NOTICE '   1. Click "View My Attendance" button';
    RAISE NOTICE '   2. Select "CS102" from the dropdown';
    RAISE NOTICE '   3. You should see your attendance table with colors!';
    RAISE NOTICE '';
    RAISE NOTICE '⚠️  REMINDER: Make sure you replaced all instances of';
    RAISE NOTICE '   "01487477" with YOUR actual user_id throughout this script!';
END $$;
