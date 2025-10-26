-- NEW Database Schema with Student ID as Primary Key
-- Run this SQL in Supabase SQL Editor

-- WARNING: This will DROP and recreate the profiles and face_images tables
-- Make sure to backup any existing data first!

-- 1. Drop existing tables (in correct order due to foreign keys)
DROP TABLE IF EXISTS face_images CASCADE;
DROP TABLE IF EXISTS profiles CASCADE;

-- 2. Create profiles table with student_id as primary key
CREATE TABLE profiles (
    student_id VARCHAR(20) PRIMARY KEY,  -- e.g., S12345
    database_id UUID UNIQUE NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    email TEXT UNIQUE NOT NULL,
    name TEXT NOT NULL,
    role TEXT NOT NULL CHECK (role IN ('STUDENT', 'PROFESSOR')),
    face_image BYTEA,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 3. Create face_images table with student_id as foreign key
CREATE TABLE face_images (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id VARCHAR(20) NOT NULL REFERENCES profiles(student_id) ON DELETE CASCADE,
    image_data BYTEA NOT NULL,
    image_number INTEGER,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT fk_student FOREIGN KEY (student_id) REFERENCES profiles(student_id) ON DELETE CASCADE
);

-- 4. Create indexes for faster lookups
CREATE INDEX idx_profiles_database_id ON profiles(database_id);
CREATE INDEX idx_profiles_email ON profiles(email);
CREATE INDEX idx_face_images_student_id ON face_images(student_id);

-- 5. Create trigger to update updated_at timestamp
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

-- 6. Verify the schema
SELECT
    table_name,
    column_name,
    data_type,
    character_maximum_length,
    is_nullable,
    column_default
FROM information_schema.columns
WHERE table_name IN ('profiles', 'face_images')
ORDER BY table_name, ordinal_position;

-- 7. Check constraints
SELECT
    tc.table_name,
    tc.constraint_name,
    tc.constraint_type,
    kcu.column_name,
    ccu.table_name AS foreign_table_name,
    ccu.column_name AS foreign_column_name
FROM information_schema.table_constraints AS tc
JOIN information_schema.key_column_usage AS kcu
    ON tc.constraint_name = kcu.constraint_name
LEFT JOIN information_schema.constraint_column_usage AS ccu
    ON ccu.constraint_name = tc.constraint_name
WHERE tc.table_name IN ('profiles', 'face_images')
ORDER BY tc.table_name, tc.constraint_type;
