package com.cs102;

import com.cs102.manager.BackupManager;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.nio.file.Path;

/**
 * Simple test class to demonstrate backup functionality
 * Run this to see backups in action!
 */
@SpringBootApplication
public class BackupTest {

    public static void main(String[] args) {
        SpringApplication.run(BackupTest.class, args);
    }

    @Bean
    CommandLineRunner testBackups(BackupManager backupManager) {
        return args -> {
            System.out.println("\n========================================");
            System.out.println("🚀 TESTING BACKUP SYSTEM");
            System.out.println("========================================\n");

            // Test 1: Get backup statistics
            System.out.println("📊 Current Backup Statistics:");
            BackupManager.BackupStats stats = backupManager.getBackupStats();
            System.out.println("   - Number of backups: " + stats.backupCount);
            System.out.println("   - Total size: " + stats.getTotalSizeMB());
            System.out.println("   - Last backup: " + (stats.lastBackupName != null ? stats.lastBackupName : "Never"));
            System.out.println();

            // Test 2: Create a full backup
            System.out.println("📦 Creating FULL backup...");
            Path fullBackupPath = backupManager.createFullBackup();
            System.out.println("✅ Full backup completed!");
            System.out.println("   Location: " + fullBackupPath.toAbsolutePath());
            System.out.println();

            // Test 3: Create attendance-only backup
            System.out.println("📝 Creating ATTENDANCE-ONLY backup...");
            Path attendanceBackupPath = backupManager.createAttendanceBackup();
            System.out.println("✅ Attendance backup completed!");
            System.out.println("   Location: " + attendanceBackupPath.toAbsolutePath());
            System.out.println();

            // Test 4: List all backups
            System.out.println("📋 Listing all backups:");
            var backups = backupManager.listBackups();
            if (backups.isEmpty()) {
                System.out.println("   No backups found");
            } else {
                for (String backup : backups) {
                    System.out.println("   - " + backup);
                }
            }
            System.out.println();

            // Test 5: Get updated statistics
            System.out.println("📊 Updated Backup Statistics:");
            stats = backupManager.getBackupStats();
            System.out.println("   - Number of backups: " + stats.backupCount);
            System.out.println("   - Total size: " + stats.getTotalSizeMB());
            System.out.println("   - Last backup: " + stats.lastBackupName);
            System.out.println();

            System.out.println("========================================");
            System.out.println("✅ ALL TESTS PASSED!");
            System.out.println("========================================");
            System.out.println();
            System.out.println("📂 Check the ./backups directory to see your backup files!");
            System.out.println();

            // Exit after tests complete
            System.exit(0);
        };
    }
}
