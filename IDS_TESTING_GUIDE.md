# 🔒 IDS Testing Guide - Complete Feature Testing

## 📋 Pre-Test Setup

### **1. Database Setup (REQUIRED for Session Anomaly Detection)**
Run this SQL in your Supabase SQL Editor:

```sql
-- Create user_sessions table
CREATE TABLE IF NOT EXISTS user_sessions (
    session_id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    login_time TIMESTAMP NOT NULL DEFAULT NOW(),
    logout_time TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    ip_address VARCHAR(50),
    device_info TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_user_sessions_email ON user_sessions(email);
CREATE INDEX IF NOT EXISTS idx_user_sessions_active ON user_sessions(is_active);

ALTER TABLE user_sessions ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view their own sessions"
    ON user_sessions FOR SELECT
    USING (auth.email() = email);

CREATE POLICY "System can insert sessions"
    ON user_sessions FOR INSERT
    WITH CHECK (true);

CREATE POLICY "System can update sessions"
    ON user_sessions FOR UPDATE
    USING (true);
```

### **2. Test Accounts**
Ensure you have these accounts:
- **Professor**: `prof8@gmail.com` / your password
- **Student**: Create a student account for testing

### **3. Open Security Dashboard**
1. Login as professor
2. Click **🔒 Security** button
3. Keep dashboard open to see real-time alerts

---

## 🧪 Test 1: Brute Force Attack Detection & Auto-Unlock

### **Objective**: Test failed login protection and automatic unlock

### **Steps:**

1. **Trigger Account Lockout:**
   ```
   ❌ Login with WRONG password - Attempt 1
   ❌ Login with WRONG password - Attempt 2
   ❌ Login with WRONG password - Attempt 3
   ❌ Login with WRONG password - Attempt 4
   ❌ Login with WRONG password - Attempt 5
   ```

2. **Expected Results:**
   - ✅ After 5th attempt: "Account locked" message
   - ✅ Console: `🔒 IDS Alert: ACCOUNT_LOCKED`
   - ✅ Console: `🔒 IDS Alert: BRUTE_FORCE_ATTACK` (if attempts were rapid)
   - ✅ Security Dashboard shows:
     - **HIGH** severity: `ACCOUNT_LOCKED`
     - **CRITICAL** severity: `BRUTE_FORCE_ATTACK` (if rapid)
   - ✅ Pop-up alert appears (HIGH severity)
   - ✅ Statistics: "Locked Accounts" count increases

3. **Test Auto-Unlock:**
   ```
   ⏰ Wait 15 minutes (or check console for unlock message)
   ✅ Login with CORRECT password
   ```

4. **Expected Results:**
   - ✅ Console (after 15 min): `🔒 IDS Alert: ACCOUNT_UNLOCKED`
   - ✅ Dashboard: New `ACCOUNT_UNLOCKED` event (LOW severity)
   - ✅ Login succeeds

### **Quick Test (Without Waiting):**
Check console output - you should see:
```
🔒 IDS Service initialized - Auto-unlock scheduler started
```
This confirms the scheduler is running every 1 minute to check for expired locks.

---

## 🧪 Test 2: SQL Injection Detection

### **Objective**: Test SQL injection attempt detection

### **Steps:**

1. **In Login Email Field, Enter:**
   ```
   admin' OR '1'='1
   ```
   Or:
   ```
   admin'; DROP TABLE users; --
   ```

2. **Expected Results:**
   - ✅ Console: `🔒 IDS Alert: SQL_INJECTION_ATTEMPT`
   - ✅ Dashboard: **CRITICAL** severity event
   - ✅ Pop-up alert appears
   - ✅ Login blocked
   - ✅ Statistics: "Critical Events" count increases

3. **Try Other SQL Keywords:**
   ```
   test@example.com' UNION SELECT * FROM users --
   admin' AND 1=1 --
   ```

---

## 🧪 Test 3: Face Anti-Spoofing Detection

### **Objective**: Test fake face detection during registration/attendance

### **Method A: Registration (Easiest)**

1. **Register New Student Account:**
   - Click "Register"
   - Fill in details
   - Choose "Student" role
   - Proceed to face capture

2. **Show Photo to Camera:**
   - Open a photo on your phone
   - Hold phone up to camera
   - Or: Show a photo on another screen

3. **Expected Results:**
   - ✅ Alert appears: "Fake Face Image Detected" (**ONCE only**)
   - ✅ Score shown: e.g., "46.7%"
   - ✅ Status: "⚠️ Fake face detected!"
   - ✅ Console: `⚠️ SPOOFING DETECTED`
   - ✅ Dashboard: **CRITICAL** event `FACE_SPOOFING_DETECTED`
   - ✅ Face NOT captured (continues trying)

4. **Use Real Camera:**
   - Remove photo
   - Look directly at camera
   - Should pass and capture face

### **Method B: Attendance Check-in**

1. **Student Login**
2. **Click "Mark Attendance"**
3. **During camera session:**
   - Show photo/screen to camera
   - Same spoofing detection triggers

### **Method C: Live Recognition (Professor)**

1. **Professor Login**
2. **Go to "Live Recognition"**
3. **Start recognition**
4. **Show photo to camera:**
   - Console: `⚠️ SPOOFING DETECTED during live recognition`
   - Face skipped, not recognized

---

## 🧪 Test 4: Session Anomaly Detection - Multiple Concurrent Sessions

### **Objective**: Test detection of 3+ simultaneous logins

### **Prerequisites**: 
- ✅ Run `user_sessions_table.sql` in Supabase (see Pre-Test Setup)

### **Steps:**

1. **Terminal 1:**
   ```bash
   cd /Users/sage/Downloads/SMU\ Y2S1/CS102\ Prog\ Fund\ II/cs102_project/CS102-
   mvn javafx:run
   ```
   - Login as `prof8@gmail.com`
   - Keep app running

2. **Terminal 2:**
   ```bash
   cd /Users/sage/Downloads/SMU\ Y2S1/CS102\ Prog\ Fund\ II/cs102_project/CS102-
   mvn javafx:run
   ```
   - Login as `prof8@gmail.com` again
   - Keep app running
   - **Session count: 2** ✅ (allowed)

3. **Terminal 3:**
   ```bash
   cd /Users/sage/Downloads/SMU\ Y2S1/CS102\ Prog\ Fund\ II/cs102_project/CS102-
   mvn javafx:run
   ```
   - Login as `prof8@gmail.com` third time
   - **Session count: 3** ⚠️ (exceeds limit)

4. **Expected Results (Terminal 3):**
   - ✅ Console: `✅ Session stored in database`
   - ✅ Console: `🔒 IDS Alert: MULTIPLE_CONCURRENT_SESSIONS`
   - ✅ Description: "3 active sessions (max allowed: 2)"
   - ✅ Dashboard (any terminal): **HIGH** severity event appears
   - ✅ Pop-up alert shows
   - ✅ Statistics: "Critical Events" increases (HIGH counts as critical)

### **Troubleshooting:**
- **If alert doesn't appear**: Check Supabase → user_sessions table exists
- **If "8 repositories found" in console**: ✅ Table detected correctly
- **If "7 repositories found"**: ❌ Table not detected, re-run SQL

---

## 🧪 Test 5: Unusual Login Time Detection

### **Objective**: Test detection of midnight-5AM logins

### **Method A: Natural Testing (Requires Late Night)**

1. **Login between 00:00 - 05:00 (Midnight to 5 AM)**

2. **Expected Results:**
   - ✅ Console: `🔒 IDS Alert: UNUSUAL_LOGIN_TIME`
   - ✅ Dashboard: **LOW** or **MEDIUM** severity
   - ✅ Description: "Late-night login detected: 03:00 on MONDAY"
   - ✅ Or: "Unusual login time detected: 03:15 (outside normal pattern)"

### **Method B: System Time Simulation (Advanced)**

**macOS:**
```bash
# Temporarily change system time
sudo date 110403002025  # Nov 4, 03:00 AM, 2025

# Run app and login
mvn javafx:run

# Restore time after test
sudo sntp -sS time.apple.com
```

**Note**: Changing system time affects your whole system. Use carefully!

---

## 🧪 Test 6: Login Pattern Learning & Anomaly Detection

### **Objective**: Test statistical pattern learning and deviation detection

### **Steps:**

1. **Establish Pattern (Need 5+ logins):**
   ```
   Login 1: 2:00 PM - Pattern building...
   Login 2: 2:30 PM - Pattern building...
   Login 3: 1:45 PM - Pattern building...
   Login 4: 2:15 PM - Pattern building...
   Login 5: 2:20 PM - ✅ Pattern established!
   ```
   
   **Pattern Detected**: "Afternoon person (12 PM - 6 PM)"

2. **Test Deviation (6+ hours difference):**
   ```
   Login 6: 10:00 PM (8 hours from average 2 PM)
   ```

3. **Expected Results:**
   - ✅ Console: `🔒 IDS Alert: ANOMALOUS_LOGIN_PATTERN`
   - ✅ Dashboard: **MEDIUM** severity
   - ✅ Description: "Anomalous login time: 22:00 (typical: ~14:00, deviation: 8 hours)"

### **Checking Your Pattern:**
In console, you'll see:
```
Pattern established: Average login time: ~14:00
```

Or check the database for your login history.

---

## 🧪 Test 7: Real-Time Dashboard Updates

### **Objective**: Verify live updates without manual refresh

### **Steps:**

1. **Open Dashboard in One Window**
2. **Login (or trigger any IDS event) in Another Window**
3. **Watch Dashboard:**
   - ✅ New event appears at TOP of table automatically
   - ✅ Statistics cards update immediately
   - ✅ Pop-up alert shows (for HIGH/CRITICAL)
   - ✅ No need to click "Refresh"

### **Test with Multiple Events:**
```
Window 1: Dashboard open
Window 2: Failed login × 5 → Account lock
Window 3: SQL injection attempt
Window 4: Face spoofing detection
```

All should appear in real-time in Window 1's dashboard.

---

## 🧪 Test 8: Export Reports

### **Objective**: Test CSV and formatted report exports

### **Steps:**

1. **Generate Some Events:**
   - Failed logins
   - Successful logins
   - Maybe a spoofing detection

2. **Open Security Dashboard**

3. **Test CSV Export:**
   - Click "📊 Export CSV"
   - Choose save location
   - Open CSV file
   - ✅ Verify: All columns present, data correct

4. **Test Formatted Report:**
   - Click "📄 Export Report"
   - Choose save location
   - Open text file
   - ✅ Verify: Header, statistics summary, events by severity

### **Expected CSV Format:**
```csv
Time,Severity,Event Type,Email,Description
2025-11-04 21:20:08,HIGH,MULTIPLE_CONCURRENT_SESSIONS,prof8@gmail.com,"Multiple concurrent sessions detected: 3 active sessions (max allowed: 2)"
```

### **Expected Report Format:**
```
=== SECURITY REPORT ===
Generated: 2025-11-04 21:30:15

SUMMARY STATISTICS:
Total Events: 13
Failed Logins: 5
Successful Logins: 8
Critical/High Events: 4
Locked Accounts: 1

EVENTS BY SEVERITY:
...
```

---

## 🧪 Test 9: Event Filtering & Sorting

### **Objective**: Test dashboard UI features

### **Steps:**

1. **Test Sorting:**
   - Click "Time" column header
   - ✅ Newest events at top (descending)
   - Click again
   - ✅ Oldest events at top (ascending)

2. **Test Description Column:**
   - Long descriptions should wrap
   - ✅ Full text visible (not truncated)
   - ✅ Row height expands automatically

3. **Test Color Coding:**
   - 🔴 **CRITICAL**: Red background
   - 🟠 **HIGH**: Orange background
   - 🟡 **MEDIUM**: Yellow background
   - 🟢 **LOW**: Green background

4. **Test Statistics:**
   - ✅ "Critical Events" includes both CRITICAL and HIGH
   - ✅ "Locked Accounts" counts unique emails, not events
   - ✅ Numbers update in real-time

---

## 📊 Quick Test Checklist

Use this checklist to verify all IDS features:

### **Security Events**
- [ ] Failed login recorded (LOW)
- [ ] Account locked after 5 failures (HIGH)
- [ ] Brute force detected for rapid attempts (CRITICAL)
- [ ] Auto-unlock after 15 minutes (LOW)
- [ ] SQL injection detected (CRITICAL)
- [ ] Face spoofing detected (CRITICAL)
- [ ] Multiple concurrent sessions (HIGH)
- [ ] Unusual login time (MEDIUM/LOW)
- [ ] Anomalous login pattern (MEDIUM)

### **Dashboard Features**
- [ ] Real-time event updates (no refresh needed)
- [ ] Pop-up alerts for HIGH/CRITICAL
- [ ] Statistics cards update live
- [ ] Table sorting works (newest first by default)
- [ ] Color coding by severity
- [ ] Full description text visible
- [ ] Export CSV works
- [ ] Export formatted report works

### **Database Integration**
- [ ] Events stored in security_events table
- [ ] Sessions stored in user_sessions table
- [ ] Concurrent session detection works across instances
- [ ] Pattern learning persists (in memory, resets on restart)

---

## 🐛 Troubleshooting

### **"No concurrent session alert"**
- ✅ Check: `user_sessions` table exists in Supabase
- ✅ Check: Console shows "8 JPA repositories" (not 7)
- ✅ Check: Each login is in a separate app instance (different terminal)

### **"Dashboard not updating"**
- ✅ Check: Listener registered (console: "✅ Security Dashboard listener registered")
- ✅ Check: Dashboard opened AFTER login
- ✅ Try: Close and reopen dashboard

### **"Face spoofing not detected"**
- ✅ Check: Camera works (not black screen)
- ✅ Check: Photo quality (too good might pass as real)
- ✅ Try: Use phone screen showing photo
- ✅ Check: Console for spoofing analysis score

### **"Auto-unlock not working"**
- ✅ Check: Console shows "Auto-unlock scheduler started"
- ✅ Wait: Full 15 minutes (not 14:59)
- ✅ Check: Account is actually locked (try login during lock)

---

## 📈 Expected Console Output Examples

### **Successful Test Run:**
```bash
🔒 IDS Service initialized - Auto-unlock scheduler started
Hibernate: insert into security_events ...
🔒 IDS Alert: [2025-11-04T21:20:08] LOW - SUCCESSFUL_LOGIN: Successful login (Email: prof8@gmail.com)
✅ Session stored in database: d6fdf4c0-32fd-4170-8af2-40425a0b4d91
🔒 IDS Alert: [2025-11-04T21:20:08] HIGH - MULTIPLE_CONCURRENT_SESSIONS: Multiple concurrent sessions detected: 3 active sessions
📱 Dashboard received alert: MULTIPLE_CONCURRENT_SESSIONS - prof8@gmail.com
✅ Event added to table
🚨 Showing pop-up alert
```

---

## 🎯 Testing Priority Order

**Quick Testing (5-10 minutes):**
1. ✅ Failed login × 5 → Account lock
2. ✅ SQL injection attempt
3. ✅ Dashboard opens and shows events
4. ✅ Export CSV/Report

**Medium Testing (20-30 minutes):**
5. ✅ Face spoofing detection
6. ✅ Multiple concurrent sessions (requires Supabase table)
7. ✅ Real-time dashboard updates
8. ✅ Wait 15 min for auto-unlock

**Full Testing (1+ hour):**
9. ✅ Login pattern learning (5+ logins over time)
10. ✅ Unusual login time (midnight-5AM)
11. ✅ Pattern deviation detection
12. ✅ All edge cases

---

## 🏆 Success Criteria

Your IDS is fully functional if:

✅ **Detection**: All 9 event types trigger correctly  
✅ **Storage**: Events saved to database  
✅ **Real-time**: Dashboard updates without refresh  
✅ **Alerts**: Pop-ups show for HIGH/CRITICAL  
✅ **Auto-actions**: Account unlock after 15 minutes  
✅ **Persistence**: Sessions tracked across instances (with Supabase table)  
✅ **Export**: CSV and reports generate correctly  
✅ **UI**: Color coding, sorting, filtering work  
✅ **Performance**: No lag, smooth operation  

---

## 📝 Test Results Template

Use this to document your testing:

```
Date: _____________
Tester: ___________

[ ] Test 1: Brute Force - PASSED / FAILED
    Notes: ________________________________

[ ] Test 2: SQL Injection - PASSED / FAILED
    Notes: ________________________________

[ ] Test 3: Face Spoofing - PASSED / FAILED
    Notes: ________________________________

[ ] Test 4: Concurrent Sessions - PASSED / FAILED
    Notes: ________________________________

[ ] Test 5: Unusual Login Time - PASSED / FAILED
    Notes: ________________________________

[ ] Test 6: Pattern Learning - PASSED / FAILED
    Notes: ________________________________

[ ] Test 7: Real-time Updates - PASSED / FAILED
    Notes: ________________________________

[ ] Test 8: Export Reports - PASSED / FAILED
    Notes: ________________________________

Overall Result: PASSED / FAILED
Recommendations: _________________________
```

---

**Happy Testing! 🔒🧪**

If you encounter any issues, check the console output and verify the Supabase table setup.
