# Complete Attendance System Guide

## ✅ Schema Overview

Your database now has a complete attendance tracking system with automatic status updates!

---

## 📊 Database Tables

### 1. **profiles** (Users)
```sql
user_id        VARCHAR(20) PRIMARY KEY  -- S12345, P67890
database_id    UUID                     -- Links to Supabase auth
email          TEXT
name           TEXT
role           TEXT                     -- STUDENT or PROFESSOR
face_image     BYTEA
```

### 2. **face_images** (15 images per user)
```sql
id            UUID PRIMARY KEY
user_id       VARCHAR(20) → profiles
image_data    BYTEA
image_number  INTEGER  -- 1 to 15
```

### 3. **classes** (Course Enrollments)
**Composite Primary Key:** (course, section, user_id)
```sql
course        VARCHAR(50)    -- e.g., "CS102"
section       VARCHAR(10)    -- e.g., "A"
user_id       VARCHAR(20)    -- Student's user_id
enrolled_at   TIMESTAMP
```

**Example:**
```
| course | section | user_id | enrolled_at         |
|--------|---------|---------|---------------------|
| CS102  | A       | S12345  | 2025-01-15 09:00:00 |
| CS102  | A       | S67890  | 2025-01-15 09:00:00 |
| CS102  | B       | S11111  | 2025-01-15 09:00:00 |
```

### 4. **sessions** (Class Meetings)
```sql
id           UUID PRIMARY KEY
session_id   VARCHAR(50) UNIQUE  -- "CS102-A-2025-01-15"
course       VARCHAR(50)
section      VARCHAR(10)
date         DATE               -- 2025-01-15
start_time   TIME               -- 09:00:00
end_time     TIME               -- 10:30:00
```

**Note:** Sessions can be created independently of enrollments. When students enroll in a class, attendance records are automatically created for existing sessions.

**Example:**
```
| session_id        | course | section | date       | start_time | end_time |
|-------------------|--------|---------|------------|------------|----------|
| CS102-A-2025-01-15| CS102  | A       | 2025-01-15 | 09:00:00   | 10:30:00 |
```

### 5. **attendance_records** (Who Attended What)
```sql
id            UUID PRIMARY KEY
user_id       VARCHAR(20) → profiles      -- Student
session_id    UUID → sessions             -- Which class
checkin_time  TIMESTAMP                   -- When they checked in
attendance    VARCHAR(10)                 -- Present, Late, Absent
method        VARCHAR(10)                 -- Auto, Manual
notes         TEXT
```

**Unique Constraint:** (user_id, session_id) - One record per student per session

---

## 🔄 Automatic Attendance Logic

### Scenario 1: Student Checks In On Time
```
Session: CS102-A-2025-01-15
Start Time: 09:00:00
Student checks in: 08:55:00

Result:
✅ attendance = "Present"
✅ method = "Auto"
```

### Scenario 2: Student Checks In Late (Within 15 mins)
```
Session: CS102-A-2025-01-15
Start Time: 09:00:00
Late Threshold: 09:15:00
Student checks in: 09:10:00

Result:
⚠️ attendance = "Late"
⚠️ method = "Auto"
```

### Scenario 3: Student Checks In After 15 Mins
```
Session: CS102-A-2025-01-15
Start Time: 09:00:00
Late Threshold: 09:15:00
Student checks in: 09:20:00

Result:
⚠️ attendance = "Late"
⚠️ method = "Auto"
```

### Scenario 4: Student Checks In After Class Ends
```
Session: CS102-A-2025-01-15
End Time: 10:30:00
Student checks in: 10:35:00

Result:
❌ attendance = "Absent"
❌ method = "Auto"
❌ notes = "Checked in after class ended"
```

### Scenario 5: Background Job Marks Late
```
Session: CS102-A-2025-01-15
Start Time: 09:00:00
Current Time: 09:16:00
Student S12345 has NOT checked in

Background job runs: mark_late_after_threshold()

Result:
⚠️ attendance = "Late" (auto-updated from "Absent")
⚠️ method = "Auto"
⚠️ notes = "Auto-marked late (no check-in by 15 min mark)"
```

### Scenario 6: Background Job Marks Absent
```
Session: CS102-A-2025-01-15
End Time: 10:30:00
Current Time: 10:31:00
Student S67890 has NEVER checked in

Background job runs: mark_absent_after_session_end()

Result:
❌ attendance = "Absent"
❌ method = "Auto"
❌ notes = "Auto-marked absent after session ended"
```

---

## 🚀 How It Works

### Step 1: Enroll Students in Class
```sql
INSERT INTO classes (course, section, user_id)
VALUES
    ('CS102', 'A', 'S12345'),
    ('CS102', 'A', 'S67890'),
    ('CS102', 'A', 'S11111');
```

### Step 2: Create a Session
```sql
INSERT INTO sessions (session_id, course, section, date, start_time, end_time)
VALUES ('CS102-A-2025-01-15', 'CS102', 'A', '2025-01-15', '09:00:00', '10:30:00');
```

**✨ Automatic Trigger Fires:**
- Creates attendance records for ALL enrolled students
- Sets attendance = 'Absent', method = 'Auto'

```
| user_id | session_id (UUID) | attendance | method | checkin_time |
|---------|-------------------|------------|--------|--------------|
| S12345  | xxx-xxx-xxx       | Absent     | Auto   | NULL         |
| S67890  | xxx-xxx-xxx       | Absent     | Auto   | NULL         |
| S11111  | xxx-xxx-xxx       | Absent     | Auto   | NULL         |
```

### Step 3: Student Checks In
```sql
UPDATE attendance_records
SET checkin_time = NOW()
WHERE user_id = 'S12345'
  AND session_id = 'xxx-xxx-xxx';
```

**✨ Automatic Trigger Fires:**
- Compares checkin_time with session start_time
- Updates attendance status accordingly

```
| user_id | attendance | method | checkin_time        |
|---------|------------|--------|---------------------|
| S12345  | Present    | Auto   | 2025-01-15 08:55:00 |
```

### Step 4: Background Jobs Run

**Every 15 minutes:** Mark students late
```sql
SELECT mark_late_after_threshold();
```

**Every hour:** Mark students absent
```sql
SELECT mark_absent_after_session_end();
```

---

## 🎯 Example Workflow

### Morning of Class

**8:50 AM - Session Created**
```sql
INSERT INTO sessions (session_id, course, section, date, start_time, end_time)
VALUES ('CS102-A-2025-01-15', 'CS102', 'A', '2025-01-15', '09:00:00', '10:30:00');
```

Result: 3 attendance records created (all "Absent")

**8:55 AM - Student S12345 Checks In**
```sql
UPDATE attendance_records
SET checkin_time = '2025-01-15 08:55:00'
WHERE user_id = 'S12345' AND session_id = (SELECT id FROM sessions WHERE session_id = 'CS102-A-2025-01-15');
```

Result: S12345 marked "Present"

**9:10 AM - Student S67890 Checks In**
```sql
UPDATE attendance_records
SET checkin_time = '2025-01-15 09:10:00'
WHERE user_id = 'S67890' AND session_id = (SELECT id FROM sessions WHERE session_id = 'CS102-A-2025-01-15');
```

Result: S67890 marked "Late"

**9:15 AM - Background Job Runs**
```sql
SELECT mark_late_after_threshold();
```

Result: S11111 (who didn't check in) is STILL marked "Absent" (will be changed to "Late" only if they don't check in)

Actually, let me fix this - the function marks them late even without check-in:

**9:16 AM - Background Job Marks Late**
```sql
SELECT mark_late_after_threshold();
```

Result: S11111 marked "Late" (no check-in by 15 min mark)

**10:31 AM - Class Ended, Background Job Runs**
```sql
SELECT mark_absent_after_session_end();
```

Result: Anyone still "Late" with no check-in is marked "Absent"

**Final Status:**
```
| user_id | attendance | checkin_time        | method | notes |
|---------|------------|---------------------|--------|-------|
| S12345  | Present    | 2025-01-15 08:55:00 | Auto   |       |
| S67890  | Late       | 2025-01-15 09:10:00 | Auto   |       |
| S11111  | Late       | NULL                | Auto   | Auto-marked late (no check-in by 15 min mark) |
```

---

## 🔐 Row Level Security (RLS)

### Students Can:
- ✅ Read their own attendance records
- ✅ Update their own attendance (check-in)
- ❌ See other students' attendance

### Professors Can:
- ✅ Read ALL attendance records
- ✅ Update ANY attendance (manual marking)
- ✅ Create sessions
- ✅ Manage enrollments

### Backend (Service Role) Can:
- ✅ Everything (bypasses RLS)

---

## 📝 Useful Queries

### View All Attendance for a Session
```sql
SELECT * FROM attendance_summary
WHERE session_id = 'CS102-A-2025-01-15'
ORDER BY name;
```

### Count Attendance Statistics
```sql
SELECT
    attendance,
    COUNT(*) as count
FROM attendance_records
WHERE session_id = (SELECT id FROM sessions WHERE session_id = 'CS102-A-2025-01-15')
GROUP BY attendance;
```

### Find Students Who Are Often Late
```sql
SELECT
    user_id,
    COUNT(*) as late_count
FROM attendance_records
WHERE attendance = 'Late'
GROUP BY user_id
HAVING COUNT(*) > 3
ORDER BY late_count DESC;
```

### Today's Sessions
```sql
SELECT * FROM sessions
WHERE date = CURRENT_DATE
ORDER BY start_time;
```

---

## ⚙️ Scheduling Background Jobs

You need to schedule these functions to run automatically:

### Option 1: Supabase Edge Functions (Recommended)
Create a cron job in Supabase Dashboard

### Option 2: pg_cron Extension
```sql
-- Mark late every 15 minutes
SELECT cron.schedule(
    'mark-late-students',
    '*/15 * * * *',
    'SELECT mark_late_after_threshold();'
);

-- Mark absent every hour
SELECT cron.schedule(
    'mark-absent-students',
    '0 * * * *',
    'SELECT mark_absent_after_session_end();'
);
```

### Option 3: External Cron Job
Run from your server:
```bash
*/15 * * * * psql -c "SELECT mark_late_after_threshold();"
0 * * * * psql -c "SELECT mark_absent_after_session_end();"
```

---

## 🧪 Testing the System

### Test 1: Create Course and Enroll Students
```sql
-- Enroll students
INSERT INTO classes (course, section, user_id) VALUES
('CS102', 'A', 'S12345'),
('CS102', 'A', 'S67890');

-- Create session
INSERT INTO sessions (session_id, course, section, date, start_time, end_time)
VALUES ('CS102-A-2025-01-15', 'CS102', 'A', '2025-01-15', '09:00:00', '10:30:00');

-- Check attendance records were auto-created
SELECT * FROM attendance_records;
```

### Test 2: Simulate Check-Ins
```sql
-- Student checks in on time
UPDATE attendance_records
SET checkin_time = '2025-01-15 08:55:00'
WHERE user_id = 'S12345';

-- Student checks in late
UPDATE attendance_records
SET checkin_time = '2025-01-15 09:10:00'
WHERE user_id = 'S67890';

-- Verify status
SELECT user_id, attendance, method FROM attendance_records;
```

### Test 3: Manual Professor Override
```sql
-- Professor manually marks student present
UPDATE attendance_records
SET
    attendance = 'Present',
    method = 'Manual',
    notes = 'Excused absence - was in hospital'
WHERE user_id = 'S11111';
```

---

## 📚 Summary

1. **Enrollment:** Students enrolled in classes (course + section)
2. **Session Created:** Attendance records auto-created for all enrolled students
3. **Check-In:** Students check in → attendance auto-updated based on time
4. **Background Jobs:** Auto-mark late (15 min) and absent (end time)
5. **Manual Override:** Professors can manually adjust any attendance

**All automatic! Just let students check in and the system handles the rest.** 🎉
