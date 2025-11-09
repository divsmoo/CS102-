#!/bin/bash

echo "========================================="
echo "🧪 Testing Backup System"
echo "========================================="
echo ""

# Compile the project
echo "📦 Compiling project..."
mvn clean compile -q

# Create a simple Java test runner
cat > /tmp/TestBackup.java << 'EOF'
import com.cs102.manager.BackupManager;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

public class TestBackup {
    public static void main(String[] args) throws Exception {
        // Disable JavaFX
        System.setProperty("java.awt.headless", "true");

        // Start minimal Spring context
        ConfigurableApplicationContext context = SpringApplication.run(com.cs102.Application.class, args);

        // Get BackupManager
        BackupManager backupManager = context.getBean(BackupManager.class);

        System.out.println("\n========================================");
        System.out.println("🚀 TESTING BACKUP SYSTEM");
        System.out.println("========================================\n");

        // Test 1: Stats
        System.out.println("📊 Current Backup Statistics:");
        BackupManager.BackupStats stats = backupManager.getBackupStats();
        System.out.println("   - Number of backups: " + stats.backupCount);
        System.out.println("   - Total size: " + stats.getTotalSizeMB());
        System.out.println("   - Last backup: " + (stats.lastBackupName != null ? stats.lastBackupName : "Never"));
        System.out.println();

        // Test 2: Full backup
        System.out.println("📦 Creating FULL backup...");
        var fullBackupPath = backupManager.createFullBackup();
        System.out.println("✅ Full backup completed!");
        System.out.println("   Location: " + fullBackupPath.toAbsolutePath());
        System.out.println();

        // Test 3: Attendance backup
        System.out.println("📝 Creating ATTENDANCE-ONLY backup...");
        var attendanceBackupPath = backupManager.createAttendanceBackup();
        System.out.println("✅ Attendance backup completed!");
        System.out.println("   Location: " + attendanceBackupPath.toAbsolutePath());
        System.out.println();

        // Test 4: List backups
        System.out.println("📋 Listing all backups:");
        var backups = backupManager.listBackups();
        for (String backup : backups) {
            System.out.println("   - " + backup);
        }
        System.out.println();

        // Updated stats
        stats = backupManager.getBackupStats();
        System.out.println("📊 Updated Statistics:");
        System.out.println("   - Number of backups: " + stats.backupCount);
        System.out.println("   - Total size: " + stats.getTotalSizeMB());
        System.out.println();

        System.out.println("========================================");
        System.out.println("✅ ALL TESTS PASSED!");
        System.out.println("========================================");
        System.out.println();
        System.out.println("📂 Check the ./backups directory:");
        System.out.println("   ls -lh ./backups");

        context.close();
        System.exit(0);
    }
}
EOF

# Run test
echo "▶️  Running backup tests..."
echo ""
mvn exec:java -Dexec.mainClass="TestBackup" -Dexec.classpathScope=runtime -q

echo ""
echo "📂 Your backups are in:"
ls -lh ./backups/ 2>/dev/null || echo "   (backups directory not created yet)"
