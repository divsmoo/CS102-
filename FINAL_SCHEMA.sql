-- FINAL DATABASE SCHEMA - Complete Setup with RLS
-- Run this ENTIRE script in Supabase SQL Editor
-- This will drop existing tables and create fresh ones

-- ============================================
-- STEP 1: DROP EXISTING TABLES (Clean Slate)
-- ============================================

DROP TABLE IF EXISTS attendance_records CASCADE;
DROP TABLE IF EXISTS sessions CASCADE;
DROP TABLE IF EXISTS classes CASCADE;
DROP TABLE IF EXISTS courses CASCADE;
DROP TABLE IF EXISTS face_images CASCADE;
DROP TABLE IF EXISTS profiles CASCADE;

-- ============================================
-- STEP 2: CREATE PROFILES TABLE
-- ============================================

CREATE TABLE profiles (
    user_id VARCHAR(20) PRIMARY KEY,  -- e.g., S12345, P67890
    database_id UUID UNIQUE NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    email TEXT UNIQUE NOT NULL,
    name TEXT NOT NULL,
    role TEXT NOT NULL CHECK (role IN ('STUDENT', 'PROFESSOR')),
    face_image BYTEA,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create indexes for faster lookups
CREATE INDEX idx_profiles_database_id ON profiles(database_id);
CREATE INDEX idx_profiles_email ON profiles(email);
CREATE INDEX idx_profiles_role ON profiles(role);

-- ============================================
-- STEP 3: CREATE FACE_IMAGES TABLE
-- ============================================

CREATE TABLE face_images (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(20) NOT NULL REFERENCES profiles(user_id) ON DELETE CASCADE,
    image_data BYTEA NOT NULL,
    image_number INTEGER,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create index for faster lookups
CREATE INDEX idx_face_images_user_id ON face_images(user_id);

-- ============================================
-- STEP 4: CREATE COURSES TABLE (Course Definitions)
-- ============================================

CREATE TABLE courses (
    course VARCHAR(50) NOT NULL,
    section VARCHAR(10) NOT NULL,
    professor_id VARCHAR(20) REFERENCES profiles(user_id) ON DELETE SET NULL,
    semester VARCHAR(20),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    PRIMARY KEY (course, section)
);

-- Create index for faster lookups
CREATE INDEX idx_courses_professor ON courses(professor_id);

-- ============================================
-- STEP 5: CREATE CLASSES TABLE (Enrollments)
-- ============================================

CREATE TABLE classes (
    course VARCHAR(50) NOT NULL,
    section VARCHAR(10) NOT NULL,
    user_id VARCHAR(20) NOT NULL REFERENCES profiles(user_id) ON DELETE CASCADE,
    enrolled_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    PRIMARY KEY (course, section, user_id),
    FOREIGN KEY (course, section) REFERENCES courses(course, section) ON DELETE CASCADE
);

-- Create index for faster lookups
CREATE INDEX idx_classes_user_id ON classes(user_id);
CREATE INDEX idx_classes_course_section ON classes(course, section);

-- ============================================
-- STEP 6: CREATE SESSIONS TABLE
-- ============================================

CREATE TABLE sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id VARCHAR(50) UNIQUE NOT NULL,  -- e.g., CS102-A-2025-01-15
    course VARCHAR(50) NOT NULL,
    section VARCHAR(10) NOT NULL,
    date DATE NOT NULL DEFAULT CURRENT_DATE,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    FOREIGN KEY (course, section) REFERENCES courses(course, section) ON DELETE CASCADE
);

-- Create indexes
CREATE INDEX idx_sessions_session_id ON sessions(session_id);
CREATE INDEX idx_sessions_course_section ON sessions(course, section);
CREATE INDEX idx_sessions_date ON sessions(date);

-- ============================================
-- STEP 7: CREATE ATTENDANCE_RECORDS TABLE
-- ============================================

CREATE TABLE attendance_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(20) NOT NULL REFERENCES profiles(user_id) ON DELETE CASCADE,
    checkin_time TIMESTAMP WITH TIME ZONE,
    attendance VARCHAR(10) NOT NULL DEFAULT 'Absent' CHECK (attendance IN ('Present', 'Late', 'Absent')),
    session_id UUID NOT NULL REFERENCES sessions(id) ON DELETE CASCADE,
    method VARCHAR(10) NOT NULL DEFAULT 'Manual' CHECK (method IN ('Auto', 'Manual')),
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT unique_user_session UNIQUE (user_id, session_id)
);

-- Create indexes
CREATE INDEX idx_attendance_user_id ON attendance_records(user_id);
CREATE INDEX idx_attendance_session_id ON attendance_records(session_id);
CREATE INDEX idx_attendance_checkin_time ON attendance_records(checkin_time);
CREATE INDEX idx_attendance_attendance ON attendance_records(attendance);

-- ============================================
-- STEP 8: CREATE TRIGGERS FOR ATTENDANCE AUTO-UPDATE
-- ============================================

-- Trigger function: Auto-update attendance based on checkin_time
CREATE OR REPLACE FUNCTION update_attendance_status()
RETURNS TRIGGER AS $$
DECLARE
    session_start_time TIMESTAMP WITH TIME ZONE;
    session_end_time TIMESTAMP WITH TIME ZONE;
    late_threshold TIMESTAMP WITH TIME ZONE;
BEGIN
    -- Get session start and end times
    SELECT
        (s.date + s.start_time) AT TIME ZONE 'UTC',
        (s.date + s.end_time) AT TIME ZONE 'UTC'
    INTO session_start_time, session_end_time
    FROM sessions s
    WHERE s.id = NEW.session_id;

    -- Calculate late threshold (start_time + 15 minutes)
    late_threshold := session_start_time + INTERVAL '15 minutes';

    -- Only update if checkin_time is provided
    IF NEW.checkin_time IS NOT NULL THEN
        -- Check if checked in on time
        IF NEW.checkin_time <= session_start_time THEN
            NEW.attendance := 'Present';
            NEW.method := 'Auto';

        -- Check if late (within 15 minutes)
        ELSIF NEW.checkin_time > session_start_time AND NEW.checkin_time <= late_threshold THEN
            NEW.attendance := 'Late';
            NEW.method := 'Auto';

        -- Check if too late (after late threshold but before end)
        ELSIF NEW.checkin_time > late_threshold AND NEW.checkin_time <= session_end_time THEN
            NEW.attendance := 'Late';
            NEW.method := 'Auto';

        -- Checked in after class ended
        ELSE
            NEW.attendance := 'Absent';
            NEW.method := 'Auto';
            NEW.notes := COALESCE(NEW.notes || '; ', '') || 'Checked in after class ended';
        END IF;
    END IF;

    NEW.updated_at := NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger on INSERT and UPDATE
CREATE TRIGGER auto_update_attendance_on_checkin
    BEFORE INSERT OR UPDATE OF checkin_time
    ON attendance_records
    FOR EACH ROW
    EXECUTE FUNCTION update_attendance_status();

-- ============================================
-- STEP 9: CREATE FUNCTION TO AUTO-MARK ABSENCES
-- ============================================

-- Function to mark students absent if they didn't check in by end_time
CREATE OR REPLACE FUNCTION mark_absent_after_session_end()
RETURNS void AS $$
BEGIN
    -- Update attendance to Absent for students who never checked in
    -- and the session has ended
    UPDATE attendance_records ar
    SET
        attendance = 'Absent',
        method = 'Auto',
        notes = COALESCE(notes || '; ', '') || 'Auto-marked absent after session ended',
        updated_at = NOW()
    FROM sessions s
    WHERE
        ar.session_id = s.id
        AND ar.checkin_time IS NULL
        AND ar.attendance != 'Absent'
        AND (s.date + s.end_time) AT TIME ZONE 'UTC' < NOW();
END;
$$ LANGUAGE plpgsql;

-- Function to mark students late if they didn't check in by start_time + 15 mins
CREATE OR REPLACE FUNCTION mark_late_after_threshold()
RETURNS void AS $$
BEGIN
    -- Update attendance to Late for students who haven't checked in
    -- and it's past start_time + 15 minutes but before end_time
    UPDATE attendance_records ar
    SET
        attendance = 'Late',
        method = 'Auto',
        notes = COALESCE(notes || '; ', '') || 'Auto-marked late (no check-in by 15 min mark)',
        updated_at = NOW()
    FROM sessions s
    WHERE
        ar.session_id = s.id
        AND ar.checkin_time IS NULL
        AND ar.attendance = 'Absent'
        AND (s.date + s.start_time + INTERVAL '15 minutes') AT TIME ZONE 'UTC' < NOW()
        AND (s.date + s.end_time) AT TIME ZONE 'UTC' > NOW();
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- STEP 10: CREATE FUNCTION TO INITIALIZE ATTENDANCE
-- ============================================

-- Function to create attendance records for all enrolled students when a session is created
CREATE OR REPLACE FUNCTION initialize_attendance_for_session()
RETURNS TRIGGER AS $$
BEGIN
    -- Insert attendance records for all students enrolled in this course/section
    INSERT INTO attendance_records (user_id, session_id, attendance, method)
    SELECT
        c.user_id,
        NEW.id,
        'Absent',
        'Auto'
    FROM classes c
    INNER JOIN profiles p ON c.user_id = p.user_id
    WHERE
        c.course = NEW.course
        AND c.section = NEW.section
        AND p.role = 'STUDENT'
    ON CONFLICT (user_id, session_id) DO NOTHING;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger to auto-initialize attendance when session is created
CREATE TRIGGER auto_initialize_attendance
    AFTER INSERT ON sessions
    FOR EACH ROW
    EXECUTE FUNCTION initialize_attendance_for_session();

-- ============================================
-- STEP 11: CREATE UPDATED_AT TRIGGER
-- ============================================

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_profiles_updated_at
    BEFORE UPDATE ON profiles
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_attendance_updated_at
    BEFORE UPDATE ON attendance_records
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================
-- STEP 12: ENABLE ROW LEVEL SECURITY (RLS)
-- ============================================

-- Enable RLS on all tables
ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE face_images ENABLE ROW LEVEL SECURITY;
ALTER TABLE courses ENABLE ROW LEVEL SECURITY;
ALTER TABLE classes ENABLE ROW LEVEL SECURITY;
ALTER TABLE sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE attendance_records ENABLE ROW LEVEL SECURITY;

-- ============================================
-- STEP 13: CREATE RLS POLICIES
-- ============================================

-- PROFILES TABLE POLICIES
CREATE POLICY "Service role full access on profiles"
    ON profiles FOR ALL TO service_role USING (true) WITH CHECK (true);

CREATE POLICY "Authenticated users can read all profiles"
    ON profiles FOR SELECT TO authenticated USING (true);

CREATE POLICY "Users can update own profile"
    ON profiles FOR UPDATE TO authenticated
    USING (database_id = auth.uid())
    WITH CHECK (database_id = auth.uid());

CREATE POLICY "Users can insert own profile"
    ON profiles FOR INSERT TO authenticated
    WITH CHECK (database_id = auth.uid());

-- FACE_IMAGES TABLE POLICIES
CREATE POLICY "Service role full access on face_images"
    ON face_images FOR ALL TO service_role USING (true) WITH CHECK (true);

CREATE POLICY "Users can read own face images"
    ON face_images FOR SELECT TO authenticated
    USING (user_id IN (SELECT user_id FROM profiles WHERE database_id = auth.uid()));

CREATE POLICY "Users can insert own face images"
    ON face_images FOR INSERT TO authenticated
    WITH CHECK (user_id IN (SELECT user_id FROM profiles WHERE database_id = auth.uid()));

CREATE POLICY "Professors can read all face images"
    ON face_images FOR SELECT TO authenticated
    USING (EXISTS (SELECT 1 FROM profiles WHERE database_id = auth.uid() AND role = 'PROFESSOR'));

-- COURSES TABLE POLICIES
CREATE POLICY "Service role full access on courses"
    ON courses FOR ALL TO service_role USING (true) WITH CHECK (true);

CREATE POLICY "All authenticated users can read courses"
    ON courses FOR SELECT TO authenticated USING (true);

CREATE POLICY "Professors can manage courses"
    ON courses FOR ALL TO authenticated
    USING (EXISTS (SELECT 1 FROM profiles WHERE database_id = auth.uid() AND role = 'PROFESSOR'))
    WITH CHECK (EXISTS (SELECT 1 FROM profiles WHERE database_id = auth.uid() AND role = 'PROFESSOR'));

-- CLASSES TABLE POLICIES
CREATE POLICY "Service role full access on classes"
    ON classes FOR ALL TO service_role USING (true) WITH CHECK (true);

CREATE POLICY "Students can read own enrollments"
    ON classes FOR SELECT TO authenticated
    USING (user_id IN (SELECT user_id FROM profiles WHERE database_id = auth.uid()));

CREATE POLICY "Professors can read all enrollments"
    ON classes FOR SELECT TO authenticated
    USING (EXISTS (SELECT 1 FROM profiles WHERE database_id = auth.uid() AND role = 'PROFESSOR'));

CREATE POLICY "Professors can manage enrollments"
    ON classes FOR ALL TO authenticated
    USING (EXISTS (SELECT 1 FROM profiles WHERE database_id = auth.uid() AND role = 'PROFESSOR'))
    WITH CHECK (EXISTS (SELECT 1 FROM profiles WHERE database_id = auth.uid() AND role = 'PROFESSOR'));

-- SESSIONS TABLE POLICIES
CREATE POLICY "Service role full access on sessions"
    ON sessions FOR ALL TO service_role USING (true) WITH CHECK (true);

CREATE POLICY "Students can read sessions for their classes"
    ON sessions FOR SELECT TO authenticated
    USING (
        EXISTS (
            SELECT 1 FROM classes c
            INNER JOIN profiles p ON c.user_id = p.user_id
            WHERE p.database_id = auth.uid()
              AND c.course = sessions.course
              AND c.section = sessions.section
        )
    );

CREATE POLICY "Professors can read all sessions"
    ON sessions FOR SELECT TO authenticated
    USING (EXISTS (SELECT 1 FROM profiles WHERE database_id = auth.uid() AND role = 'PROFESSOR'));

CREATE POLICY "Professors can manage sessions"
    ON sessions FOR ALL TO authenticated
    USING (EXISTS (SELECT 1 FROM profiles WHERE database_id = auth.uid() AND role = 'PROFESSOR'))
    WITH CHECK (EXISTS (SELECT 1 FROM profiles WHERE database_id = auth.uid() AND role = 'PROFESSOR'));

-- ATTENDANCE_RECORDS TABLE POLICIES
CREATE POLICY "Service role full access on attendance_records"
    ON attendance_records FOR ALL TO service_role USING (true) WITH CHECK (true);

CREATE POLICY "Students can read own attendance"
    ON attendance_records FOR SELECT TO authenticated
    USING (user_id IN (SELECT user_id FROM profiles WHERE database_id = auth.uid()));

CREATE POLICY "Students can update own attendance (check-in)"
    ON attendance_records FOR UPDATE TO authenticated
    USING (user_id IN (SELECT user_id FROM profiles WHERE database_id = auth.uid()))
    WITH CHECK (user_id IN (SELECT user_id FROM profiles WHERE database_id = auth.uid()));

CREATE POLICY "Professors can read all attendance"
    ON attendance_records FOR SELECT TO authenticated
    USING (EXISTS (SELECT 1 FROM profiles WHERE database_id = auth.uid() AND role = 'PROFESSOR'));

CREATE POLICY "Professors can manage all attendance"
    ON attendance_records FOR ALL TO authenticated
    USING (EXISTS (SELECT 1 FROM profiles WHERE database_id = auth.uid() AND role = 'PROFESSOR'))
    WITH CHECK (EXISTS (SELECT 1 FROM profiles WHERE database_id = auth.uid() AND role = 'PROFESSOR'));

-- ============================================
-- STEP 14: CREATE HELPER VIEWS
-- ============================================

-- View for attendance summary
CREATE OR REPLACE VIEW attendance_summary AS
SELECT
    ar.user_id,
    p.name,
    p.email,
    s.session_id,
    s.course,
    s.section,
    s.date,
    s.start_time,
    s.end_time,
    ar.attendance,
    ar.checkin_time,
    ar.method,
    ar.notes
FROM attendance_records ar
INNER JOIN profiles p ON ar.user_id = p.user_id
INNER JOIN sessions s ON ar.session_id = s.id
ORDER BY s.date DESC, s.start_time DESC, p.name;

-- ============================================
-- STEP 15: VERIFY SCHEMA
-- ============================================

-- Show all tables
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
  AND table_type = 'BASE TABLE'
ORDER BY table_name;

-- Show profiles structure
SELECT column_name, data_type, character_maximum_length, is_nullable
FROM information_schema.columns
WHERE table_name = 'profiles'
ORDER BY ordinal_position;

-- Show attendance_records structure
SELECT column_name, data_type, character_maximum_length, is_nullable
FROM information_schema.columns
WHERE table_name = 'attendance_records'
ORDER BY ordinal_position;

-- Show all triggers
SELECT
    trigger_name,
    event_object_table,
    action_timing,
    event_manipulation
FROM information_schema.triggers
WHERE trigger_schema = 'public'
ORDER BY event_object_table, trigger_name;

-- ============================================
-- SUCCESS MESSAGE
-- ============================================

DO $$
BEGIN
    RAISE NOTICE '✅ Database schema created successfully!';
    RAISE NOTICE 'Tables: profiles, face_images, courses, classes, sessions, attendance_records';
    RAISE NOTICE 'Triggers: Auto-attendance marking based on check-in times';
    RAISE NOTICE 'RLS: Enabled on all tables';
    RAISE NOTICE '';
    RAISE NOTICE '📝 Workflow:';
    RAISE NOTICE '1. Professor creates a course: INSERT INTO courses (course, section, professor_id) ...';
    RAISE NOTICE '2. Students enroll: INSERT INTO classes (course, section, user_id) ...';
    RAISE NOTICE '3. Professor creates session: INSERT INTO sessions (session_id, course, section, date, start_time, end_time) ...';
    RAISE NOTICE '4. Trigger auto-creates attendance records for all enrolled students';
    RAISE NOTICE '';
    RAISE NOTICE '🔧 Schedule these functions:';
    RAISE NOTICE '- SELECT mark_late_after_threshold(); -- every 15 mins';
    RAISE NOTICE '- SELECT mark_absent_after_session_end(); -- every hour';
END $$;
