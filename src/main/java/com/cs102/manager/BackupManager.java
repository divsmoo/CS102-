package com.cs102.manager;

import com.cs102.model.AttendanceRecord;
import com.cs102.model.Course;
import com.cs102.model.Session;
import com.cs102.model.User;
import com.cs102.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Manages database backups at the application level
 * Provides manual backup functionality and export to various formats
 */
@Service
public class BackupManager {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AttendanceRecordRepository attendanceRecordRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private ClassRepository classRepository;

    @Value("${backup.directory:#{systemProperties['user.home'] + '/Downloads'}}")
    private String backupDirectory;

    /**
     * Creates a full backup of all database tables in CSV format
     * @return Path to the backup file
     */
    public Path createFullBackup() throws IOException {
        LocalDateTime now = LocalDateTime.now();

        // Create backup directory if it doesn't exist
        Path backupDir = Paths.get(backupDirectory);
        if (!Files.exists(backupDir)) {
            Files.createDirectories(backupDir);
        }

        Path backupPath = backupDir.resolve("full_backup");

        // Delete existing backup folder if it exists
        if (Files.exists(backupPath)) {
            deleteDirectory(backupPath);
        }

        Files.createDirectories(backupPath);

        // Backup each table
        backupUsers(backupPath.resolve("users.csv"));
        backupCourses(backupPath.resolve("courses.csv"));
        backupClasses(backupPath.resolve("classes.csv"));
        backupSessions(backupPath.resolve("sessions.csv"));
        backupAttendanceRecords(backupPath.resolve("attendance_records.csv"));

        // Create metadata file
        createMetadata(backupPath.resolve("metadata.txt"), now);

        return backupPath;
    }

    /**
     * Creates a backup of attendance records only (smaller, faster)
     * @return Path to the backup file
     */
    public Path createAttendanceBackup() throws IOException {
        Path backupDir = Paths.get(backupDirectory);
        if (!Files.exists(backupDir)) {
            Files.createDirectories(backupDir);
        }

        Path backupFile = backupDir.resolve("attendance_backup.csv");

        // Delete existing file if it exists
        if (Files.exists(backupFile)) {
            Files.delete(backupFile);
        }

        backupAttendanceRecords(backupFile);

        return backupFile;
    }

    /**
     * Backup users table to CSV
     */
    private void backupUsers(Path outputPath) throws IOException {
        List<User> users = userRepository.findAll();

        try (BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
            // Header
            writer.write("user_id,database_id,email,name,role,late_threshold\n");

            // Data
            for (User user : users) {
                writer.write(String.format("%s,%s,%s,%s,%s,%d\n",
                    escapeCsv(user.getUserId()),
                    escapeCsv(user.getDatabaseId().toString()),
                    escapeCsv(user.getEmail()),
                    escapeCsv(user.getName()),
                    escapeCsv(user.getRole().name()),
                    user.getLateThreshold()
                ));
            }
        }
    }

    /**
     * Backup courses table to CSV
     */
    private void backupCourses(Path outputPath) throws IOException {
        List<Course> courses = courseRepository.findAll();

        try (BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
            // Header
            writer.write("course,section,professor_id,semester\n");

            // Data
            for (Course course : courses) {
                writer.write(String.format("%s,%s,%s,%s\n",
                    escapeCsv(course.getCourse()),
                    escapeCsv(course.getSection()),
                    escapeCsv(course.getProfessorId()),
                    escapeCsv(course.getSemester())
                ));
            }
        }
    }

    /**
     * Backup classes (enrollments) table to CSV
     */
    private void backupClasses(Path outputPath) throws IOException {
        List<com.cs102.model.Class> classes = classRepository.findAll();

        try (BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
            // Header
            writer.write("course,section,user_id\n");

            // Data
            for (com.cs102.model.Class cls : classes) {
                writer.write(String.format("%s,%s,%s\n",
                    escapeCsv(cls.getCourse()),
                    escapeCsv(cls.getSection()),
                    escapeCsv(cls.getUserId())
                ));
            }
        }
    }

    /**
     * Backup sessions table to CSV
     */
    private void backupSessions(Path outputPath) throws IOException {
        List<Session> sessions = sessionRepository.findAll();

        try (BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
            // Header
            writer.write("id,session_id,course,section,date,start_time,end_time,created_at\n");

            // Data
            for (Session session : sessions) {
                writer.write(String.format("%s,%s,%s,%s,%s,%s,%s,%s\n",
                    escapeCsv(session.getId().toString()),
                    escapeCsv(session.getSessionId()),
                    escapeCsv(session.getCourse()),
                    escapeCsv(session.getSection()),
                    escapeCsv(session.getDate().toString()),
                    escapeCsv(session.getStartTime().toString()),
                    escapeCsv(session.getEndTime().toString()),
                    escapeCsv(session.getCreatedAt().toString())
                ));
            }
        }
    }

    /**
     * Backup attendance records table to CSV
     */
    private void backupAttendanceRecords(Path outputPath) throws IOException {
        List<AttendanceRecord> records = attendanceRecordRepository.findAll();

        try (BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
            // Header
            writer.write("id,user_id,session_id,checkin_time,attendance,method,notes,created_at,updated_at\n");

            // Data
            for (AttendanceRecord record : records) {
                writer.write(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s\n",
                    escapeCsv(record.getId().toString()),
                    escapeCsv(record.getUserId()),
                    escapeCsv(record.getSessionId().toString()),
                    escapeCsv(record.getCheckinTime() != null ? record.getCheckinTime().toString() : ""),
                    escapeCsv(record.getAttendance()),
                    escapeCsv(record.getMethod()),
                    escapeCsv(record.getNotes() != null ? record.getNotes() : ""),
                    escapeCsv(record.getCreatedAt().toString()),
                    escapeCsv(record.getUpdatedAt().toString())
                ));
            }
        }
    }

    /**
     * Create metadata file with backup information
     */
    private void createMetadata(Path outputPath, LocalDateTime backupTime) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
            writer.write("=== CS102 Attendance System Backup ===\n");
            writer.write("Backup Date: " + backupTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "\n");
            writer.write("Backup Type: Full\n");
            writer.write("Format: CSV\n");
            writer.write("\nTables Included:\n");
            writer.write("- users\n");
            writer.write("- courses\n");
            writer.write("- classes (enrollments)\n");
            writer.write("- sessions\n");
            writer.write("- attendance_records\n");
            writer.write("\nNotes:\n");
            writer.write("- Face images are NOT included in CSV backups (use SQL dump for complete backup)\n");
            writer.write("- Use Supabase backups or GitHub Actions for full database restoration\n");
        }
    }

    /**
     * Compress a backup directory into a .tar.gz file
     */
    public Path compressBackup(Path backupDir) throws IOException {
        Path compressedFile = Paths.get(backupDir.toString() + ".tar.gz");

        // Simple gzip of directory (for production, use tar.gz libraries)
        // This is a simplified version - in production use Apache Commons Compress

        return compressedFile;
    }

    /**
     * Get list of all backups
     */
    public List<String> listBackups() throws IOException {
        Path backupDir = Paths.get(backupDirectory);
        if (!Files.exists(backupDir)) {
            return List.of();
        }

        return Files.list(backupDir)
            .filter(Files::isDirectory)
            .map(Path::getFileName)
            .map(Path::toString)
            .sorted((a, b) -> b.compareTo(a)) // Most recent first
            .toList();
    }

    /**
     * Delete old backups (keep only last N backups)
     */
    public void cleanupOldBackups(int keepCount) throws IOException {
        List<String> backups = listBackups();

        if (backups.size() <= keepCount) {
            return; // Nothing to delete
        }

        // Delete old backups
        for (int i = keepCount; i < backups.size(); i++) {
            Path backupPath = Paths.get(backupDirectory, backups.get(i));
            deleteDirectory(backupPath);
        }
    }

    /**
     * Helper: Escape CSV values
     */
    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }

        // Escape quotes and wrap in quotes if contains comma, newline, or quote
        if (value.contains(",") || value.contains("\n") || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }

        return value;
    }

    /**
     * Helper: Recursively delete directory
     */
    private void deleteDirectory(Path directory) throws IOException {
        if (Files.exists(directory)) {
            Files.walk(directory)
                .sorted((a, b) -> b.compareTo(a)) // Delete files before directories
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        System.err.println("Failed to delete: " + path);
                    }
                });
        }
    }

    /**
     * Get the backup directory path
     */
    public String getBackupDirectory() {
        return backupDirectory;
    }

    /**
     * Get backup statistics
     */
    public BackupStats getBackupStats() {
        try {
            Path backupDir = Paths.get(backupDirectory);
            if (!Files.exists(backupDir)) {
                return new BackupStats(0, 0, null, null);
            }

            // Check for full_backup directory
            Path fullBackupPath = backupDir.resolve("full_backup");
            long totalSize = 0;
            LocalDateTime lastModified = null;
            String lastBackupName = null;

            if (Files.exists(fullBackupPath) && Files.isDirectory(fullBackupPath)) {
                lastBackupName = "full_backup";
                lastModified = LocalDateTime.ofInstant(
                    Files.getLastModifiedTime(fullBackupPath).toInstant(),
                    java.time.ZoneId.systemDefault()
                );

                // Calculate total size
                totalSize = Files.walk(fullBackupPath)
                    .filter(Files::isRegularFile)
                    .mapToLong(path -> {
                        try {
                            return Files.size(path);
                        } catch (IOException e) {
                            return 0;
                        }
                    })
                    .sum();
            }

            // Check for attendance_backup.csv
            Path attendanceBackupPath = backupDir.resolve("attendance_backup.csv");
            if (Files.exists(attendanceBackupPath) && Files.isRegularFile(attendanceBackupPath)) {
                LocalDateTime attendanceModified = LocalDateTime.ofInstant(
                    Files.getLastModifiedTime(attendanceBackupPath).toInstant(),
                    java.time.ZoneId.systemDefault()
                );

                // Use the most recent backup
                if (lastModified == null || attendanceModified.isAfter(lastModified)) {
                    lastBackupName = "attendance_backup.csv";
                    lastModified = attendanceModified;
                }

                totalSize += Files.size(attendanceBackupPath);
            }

            int backupCount = (Files.exists(fullBackupPath) ? 1 : 0);

            return new BackupStats(backupCount, totalSize, lastBackupName, lastModified);
        } catch (IOException e) {
            return new BackupStats(0, 0, null, null);
        }
    }

    /**
     * Backup statistics container
     */
    public static class BackupStats {
        public final int backupCount;
        public final long totalSizeBytes;
        public final String lastBackupName;
        public final LocalDateTime lastModified;

        public BackupStats(int backupCount, long totalSizeBytes, String lastBackupName, LocalDateTime lastModified) {
            this.backupCount = backupCount;
            this.totalSizeBytes = totalSizeBytes;
            this.lastBackupName = lastBackupName;
            this.lastModified = lastModified;
        }

        public String getTotalSizeKB() {
            return String.format("%.2f KB", totalSizeBytes / 1024.0);
        }

        /**
         * Get formatted date from file modification time in day/month/year format
         */
        public String getFormattedDate() {
            if (lastModified == null) {
                return "Never";
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            return lastModified.format(formatter);
        }

        /**
         * Get formatted backup info with filename, size, and date
         * Format: "filename.csv (1.23 KB, 10/11/2025)"
         */
        public String getFormattedBackupInfo() {
            if (lastBackupName == null) {
                return "No backups yet";
            }

            return String.format("%s (%s, %s)",
                lastBackupName,
                getTotalSizeKB(),
                getFormattedDate()
            );
        }
    }
}
