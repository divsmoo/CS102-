# Fix: "Hibernate transaction: Unable to commit against JDBC Connection; bad SQL grammar"

## The Problem

You're getting this error because the database tables don't exist yet. The application is trying to save data to tables that haven't been created.

---

## ✅ SOLUTION - Follow These Steps

### Step 1: Open Supabase SQL Editor

1. Go to: https://rcbqthvswbrafceqflbz.supabase.co
2. Click **"SQL Editor"** in the left sidebar (looks like a `</>` icon)
3. Click **"New Query"** button

### Step 2: Copy and Run the Schema

1. Open the file: **`FINAL_SCHEMA.sql`** in your project folder
2. **Copy the ENTIRE contents** of that file
3. **Paste** it into the Supabase SQL Editor
4. Click **"Run"** (or press Ctrl+Enter / Cmd+Enter)

### Step 3: Verify Success

You should see output like:
```
✅ Database schema created successfully!
Tables created: profiles, face_images, attendance_records, sessions
```

And you should see a list of tables and their structures.

### Step 4: Restart Your Application

```bash
mvn javafx:run
```

---

## What the Schema Creates

The SQL script creates these tables:

### 1. **profiles** table
- `student_id` (VARCHAR) - Primary key (e.g., S12345)
- `database_id` (UUID) - Links to Supabase auth.users
- `email` (TEXT)
- `name` (TEXT)
- `role` (TEXT) - STUDENT or PROFESSOR
- `face_image` (BYTEA)

### 2. **face_images** table
- `id` (UUID) - Primary key
- `student_id` (VARCHAR) - Foreign key to profiles
- `image_data` (BYTEA) - Preprocessed face image
- `image_number` (INTEGER) - 1 to 15

### 3. **attendance_records** table (for future use)
- Tracks student check-ins and check-outs

### 4. **sessions** table (for future use)
- Manages user sessions

---

## Alternative: Copy-Paste Directly

If you can't open the file, here's the key part to run:

```sql
-- Drop existing tables
DROP TABLE IF EXISTS face_images CASCADE;
DROP TABLE IF EXISTS profiles CASCADE;

-- Create profiles table
CREATE TABLE profiles (
    student_id VARCHAR(20) PRIMARY KEY,
    database_id UUID UNIQUE NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    email TEXT UNIQUE NOT NULL,
    name TEXT NOT NULL,
    role TEXT NOT NULL CHECK (role IN ('STUDENT', 'PROFESSOR')),
    face_image BYTEA,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create face_images table
CREATE TABLE face_images (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id VARCHAR(20) NOT NULL REFERENCES profiles(student_id) ON DELETE CASCADE,
    image_data BYTEA NOT NULL,
    image_number INTEGER,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create indexes
CREATE INDEX idx_profiles_database_id ON profiles(database_id);
CREATE INDEX idx_profiles_email ON profiles(email);
CREATE INDEX idx_face_images_student_id ON face_images(student_id);
```

---

## Troubleshooting

### Error: "relation auth.users does not exist"

**Problem**: Supabase auth schema is not accessible

**Solution**: Make sure you're running this in Supabase SQL Editor, not a local PostgreSQL. Supabase has the auth schema built-in.

### Error: "permission denied"

**Problem**: Not enough permissions

**Solution**: Make sure you're logged in to Supabase with the project owner account.

### Still Getting SQL Grammar Error After Running Schema

**Problem**: Application not connecting to database

**Solution**: Check your `application.properties` file:

```properties
spring.datasource.url=jdbc:postgresql://aws-1-ap-southeast-1.pooler.supabase.com:6543/postgres
spring.datasource.username=postgres.rcbqthvswbrafceqflbz
spring.datasource.password=uvnxEJugdbewiJeS
```

Make sure these values are correct.

---

## Quick Test After Schema Creation

Run this query in Supabase to verify tables exist:

```sql
-- Check if tables exist
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
  AND table_name IN ('profiles', 'face_images')
ORDER BY table_name;
```

Should return:
```
face_images
profiles
```

---

## After Schema is Created

1. ✅ Run application: `mvn javafx:run`
2. ✅ Click "Register"
3. ✅ Fill in:
   - Student ID: S12345
   - Name: Test User
   - Email: test@example.com
   - Password: password123
   - Role: STUDENT
4. ✅ Camera captures 15 images
5. ✅ User created successfully!

---

## Need More Help?

If you're still getting errors after running the schema:

1. **Check console output** - Look for specific error messages
2. **Verify Supabase connection** - Test with:
   ```bash
   nc -zv aws-1-ap-southeast-1.pooler.supabase.com 6543
   ```
3. **Check Supabase Dashboard** - Make sure the database is running
4. **Share the error** - Copy the full error message from console

---

## Summary

**The error means**: Tables don't exist yet

**The solution**: Run `FINAL_SCHEMA.sql` in Supabase SQL Editor

**Next step**: Restart application and test registration

That's it! Once you run the schema, the error should be gone.
