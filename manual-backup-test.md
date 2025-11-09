# Manual Backup Test Guide

Since the automated test has JavaFX conflicts, let's test the backup system manually by running your actual application.

## Quick Test (Using Your App)

### Step 1: Build the project
```bash
cd /Users/yongzhuoyu/Documents/GitHub/CS102-
mvn clean install
```

### Step 2: Run your actual application
```bash
mvn spring-boot:run
```

### Step 3: When the app opens
1. Login as a **professor** (or student, doesn't matter for this test)
2. We'll add backup functionality to the UI next

---

## Even Simpler: Test via Terminal (Direct SQL Dump)

If you just want to verify backups work right now without the app:

### Option A: Manual pg_dump (Quick test)

```bash
# Export your database password
export PGPASSWORD="uvnxEJugdbewiJeS"

# Create backup directory
mkdir -p ./backups

# Create a manual SQL backup
pg_dump \
  --host=aws-1-ap-southeast-1.pooler.supabase.com \
  --port=5432 \
  --username=postgres.rcbqthvswbrafceqflbz \
  --dbname=postgres \
  --no-owner \
  --no-acl \
  --clean \
  --file=./backups/manual_backup_$(date +%Y%m%d_%H%M%S).sql

echo "✅ Backup created in ./backups/"
ls -lh ./backups/
```

### Option B: Verify GitHub Actions worked

1. Go to https://github.com/divsmoo/CS102-/actions
2. Click on your "Database Backup" workflow run
3. Download the artifact
4. Unzip and check the SQL file

---

## Next: Add Backup UI to Professor View

Once you verify backups work, we can add a nice UI in the professor dashboard.

See `BACKUP_INTEGRATION_EXAMPLE.java` for the code to add to ProfessorView.java.
