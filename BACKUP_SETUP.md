# Database Backup Setup Guide

## Overview

This project implements a **multi-layer backup strategy** for the CS102 Student Attendance System database.

---

## Layer 1: Supabase Automatic Backups

**Status:** Already enabled by default

**What it provides:**
- Daily automated backups
- 7-day retention (free tier)
- Point-in-time recovery (paid plans)

**How to restore:**
1. Go to Supabase Dashboard → Your Project
2. Click **Database** → **Backups**
3. Select backup date
4. Click **Restore**

---

## Layer 2: GitHub Actions Automated Backups

**What it does:**
- Runs daily at 2 AM UTC (10 AM Singapore time)
- Creates SQL dump of entire database
- Compresses with gzip
- Stores as GitHub artifact for 30 days
- Can be extended to upload to cloud storage

### Setup Instructions:

#### 1. Add GitHub Secrets

Go to your repository → Settings → Secrets and variables → Actions → New repository secret

Add these secrets:

| Secret Name | Value | Example |
|-------------|-------|---------|
| `DB_HOST` | Your Supabase host | `aws-0-us-west-1.pooler.supabase.com` |
| `DB_USERNAME` | Database username | `postgres.abcdefghijk` |
| `DB_PASSWORD` | Database password | Your password |

#### 2. Enable GitHub Actions

- Go to **Actions** tab in your repository
- Click "I understand my workflows, go ahead and enable them"

#### 3. Test the Backup

- Go to **Actions** tab
- Click on "Database Backup" workflow
- Click "Run workflow" → "Run workflow"
- Wait for completion (should take 1-2 minutes)
- Check **Artifacts** section for the backup file

#### 4. Schedule is Automatic

The workflow will now run daily at 2 AM UTC automatically.

### How to Download a Backup:

1. Go to **Actions** tab
2. Click on a completed "Database Backup" workflow run
3. Scroll to **Artifacts** section
4. Download the `database-backup-YYYY-MM-DD` file
5. Unzip: `gunzip backup_*.sql.gz`
6. Restore: `psql -h <host> -U <user> -d postgres -f backup_*.sql`

---

## Layer 3: Application-Level Backups (Java)

**Coming soon:** See `BackupManager.java` for programmatic backups

Features:
- Manual backup trigger from professor dashboard
- Selective table backups (attendance only, users only, etc.)
- Export to CSV/JSON formats
- Local file storage

---

## Layer 4: Off-Site Cloud Storage (Optional)

Uncomment the S3 upload section in `.github/workflows/database-backup.yml` to enable:

### AWS S3 Setup:
1. Create S3 bucket: `cs102-attendance-backups`
2. Add AWS credentials to GitHub secrets:
   - `AWS_ACCESS_KEY_ID`
   - `AWS_SECRET_ACCESS_KEY`
3. Uncomment lines 47-58 in the workflow file

### Alternative: Google Drive Backup
Use `rclone` to sync backups to Google Drive (see documentation)

---

## Backup Retention Policy

| Layer | Frequency | Retention | Location |
|-------|-----------|-----------|----------|
| Supabase Built-in | Daily | 7 days | Supabase cloud |
| GitHub Actions | Daily | 30 days | GitHub artifacts |
| S3 (optional) | Daily | 90 days | AWS S3 |
| Local (manual) | On-demand | Indefinite | Your computer |

---

## Disaster Recovery Procedure

### Scenario 1: Accidental Data Deletion (< 7 days ago)

**Use Supabase built-in backup:**

1. Log into Supabase Dashboard
2. Database → Backups
3. Select date before deletion
4. Click "Restore"
5. Verify data is restored

### Scenario 2: Need older backup (7-30 days ago)

**Use GitHub Actions backup:**

1. Go to GitHub → Actions → Database Backup
2. Find the workflow run from desired date
3. Download artifact
4. Unzip the SQL file
5. Connect to Supabase:
   ```bash
   psql "postgresql://[USERNAME]:[PASSWORD]@[HOST]:6543/postgres"
   ```
6. Drop existing tables:
   ```sql
   DROP SCHEMA public CASCADE;
   CREATE SCHEMA public;
   ```
7. Restore from backup:
   ```bash
   psql "postgresql://[USERNAME]:[PASSWORD]@[HOST]:6543/postgres" < backup_file.sql
   ```

### Scenario 3: Complete Database Loss

1. Create new Supabase project
2. Update connection strings in `application.properties`
3. Restore from latest backup (GitHub Actions or S3)
4. Run application to verify

---

## Testing Your Backups

**Critical: Test backups regularly!**

Monthly test procedure:
1. Download latest backup from GitHub Actions
2. Create test Supabase project (or local PostgreSQL)
3. Restore backup
4. Verify all tables have data
5. Test application connectivity

---

## Monitoring & Alerts

### GitHub Actions Failure Notification

If backup fails, you'll receive:
- Email notification (if enabled in GitHub settings)
- Red X in Actions tab

### Custom Alerts (Optional)

Add Slack webhook or email notification to workflow:
```yaml
- name: Send Slack notification
  if: failure()
  run: |
    curl -X POST -H 'Content-type: application/json' \
      --data '{"text":"Database backup failed!"}' \
      ${{ secrets.SLACK_WEBHOOK_URL }}
```

---

## Security Considerations

1. **Never commit database credentials** to the repository
2. **Use GitHub Secrets** for all sensitive data
3. **Encrypt backups** if storing publicly accessible locations
4. **Limit access** to backup artifacts (private repository only)
5. **Rotate credentials** if they're ever exposed

---

## Estimated Costs

| Service | Free Tier | Paid (if needed) |
|---------|-----------|------------------|
| Supabase Backups | 7 days | $25/month (Pro plan) |
| GitHub Actions | 2,000 minutes/month | $0.008/minute |
| AWS S3 Storage | 5GB free (1 year) | $0.023/GB/month |
| Total Monthly Cost | **$0** | ~$25-50 (if using all paid features) |

For your project size (~100MB database), you can stay **completely free** using GitHub Actions + free tier Supabase.

---

## Next Steps

- [ ] Add GitHub secrets (DB_HOST, DB_USERNAME, DB_PASSWORD)
- [ ] Enable GitHub Actions
- [ ] Run first test backup manually
- [ ] Verify backup artifact downloads correctly
- [ ] Set calendar reminder to test restore monthly
- [ ] (Optional) Set up S3 or Google Drive for long-term storage
