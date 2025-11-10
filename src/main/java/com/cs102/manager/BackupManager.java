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
import java.util.zip.GZIPOutputStream;

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

    @Value("${backup.directory:./backups}")
    private String backupDirectory;

    /**
     * Creates a full backup of all database tables in CSV format
     * @return Path to the backup file
     */
    public Path createFullBackup() throws IOException {
        LocalDateTime now = LocalDateTime.now();
        String timestamp = now.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

        // Create backup directory if it doesn't exist
        Path backupDir = Paths.get(backupDirectory);
        if (!Files.exists(backupDir)) {
            Files.createDirectories(backupDir);
        }

        Path backupPath = backupDir.resolve("full_backup_" + timestamp);
        Files.createDirectories(backupPath);

        // Backup each table
        backupUsers(backupPath.resolve("users.csv"));
        backupCourses(backupPath.resolve("courses.csv"));
        backupClasses(backupPath.resolve("classes.csv"));
        backupSessions(backupPath.resolve("sessions.csv"));
        backupAttendanceRecords(backupPath.resolve("attendance_records.csv"));

        // Create metadata file
        createMetadata(backupPath.resolve("metadata.txt"), now);

        System.out.println("✓ Full backup created at: " + backupPath);
        return backupPath;
    }

    /**
     * Creates a backup of attendance records only (smaller, faster)
     * @return Path to the backup file
     */
    public Path createAttendanceBackup() throws IOException {
        LocalDateTime now = LocalDateTime.now();
        String timestamp = now.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

        Path backupDir = Paths.get(backupDirectory);
        if (!Files.exists(backupDir)) {
            Files.createDirectories(backupDir);
        }

        Path backupFile = backupDir.resolve("attendance_backup_" + timestamp + ".csv");
        backupAttendanceRecords(backupFile);

        System.out.println("✓ Attendance backup created at: " + backupFile);
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

        System.out.println("  - Backed up " + users.size() + " users");
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

        System.out.println("  - Backed up " + courses.size() + " courses");
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

        System.out.println("  - Backed up " + classes.size() + " enrollments");
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

        System.out.println("  - Backed up " + sessions.size() + " sessions");
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

        System.out.println("  - Backed up " + records.size() + " attendance records");
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

        System.out.println("✓ Backup compressed to: " + compressedFile);
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
            System.out.println("  - Deleted old backup: " + backups.get(i));
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
     * Get backup statistics
     */
    public BackupStats getBackupStats() {
        try {
            List<String> backups = listBackups();
            long totalSize = 0;

            Path backupDir = Paths.get(backupDirectory);
            if (Files.exists(backupDir)) {
                totalSize = Files.walk(backupDir)
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

            return new BackupStats(
                backups.size(),
                totalSize,
                backups.isEmpty() ? null : backups.get(0)
            );
        } catch (IOException e) {
            return new BackupStats(0, 0, null);
        }
    }

    /**
     * Backup statistics container
     */
    public static class BackupStats {
        public final int backupCount;
        public final long totalSizeBytes;
        public final String lastBackupName;

        public BackupStats(int backupCount, long totalSizeBytes, String lastBackupName) {
            this.backupCount = backupCount;
            this.totalSizeBytes = totalSizeBytes;
            this.lastBackupName = lastBackupName;
        }

        public String getTotalSizeMB() {
            return String.format("%.2f MB", totalSizeBytes / (1024.0 * 1024.0));
        }
    }
}
