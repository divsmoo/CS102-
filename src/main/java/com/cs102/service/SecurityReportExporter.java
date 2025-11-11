package com.cs102.service;

import com.cs102.model.SecurityEvent;
import com.cs102.model.Severity;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Service for exporting security reports
 */
@Service
public class SecurityReportExporter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FILE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /**
     * Export security events to CSV file
     */
    public String exportToCSV(List<SecurityEvent> events, String filename) throws IOException {
        if (filename == null || filename.isEmpty()) {
            filename = "security_report_" + LocalDateTime.now().format(FILE_DATE_FORMATTER) + ".csv";
        }
        
        if (!filename.endsWith(".csv")) {
            filename += ".csv";
        }

        try (FileWriter writer = new FileWriter(filename)) {
            // Write CSV header
            writer.append("Timestamp,Severity,Event Type,Email,Description,IP Address,User Agent,Blocked\n");

            // Write data rows
            for (SecurityEvent event : events) {
                writer.append(escapeCSV(event.getTimestamp().format(DATE_FORMATTER))).append(",");
                writer.append(escapeCSV(event.getSeverity().toString())).append(",");
                writer.append(escapeCSV(event.getEventType().toString())).append(",");
                writer.append(escapeCSV(event.getEmail())).append(",");
                writer.append(escapeCSV(event.getDescription())).append(",");
                writer.append(escapeCSV(event.getIpAddress() != null ? event.getIpAddress() : "N/A")).append(",");
                writer.append(escapeCSV(event.getUserAgent() != null ? event.getUserAgent() : "N/A")).append(",");
                writer.append(event.isBlocked() ? "Yes" : "No").append("\n");
            }
        }

        return filename;
    }

    /**
     * Export security report with statistics to text file
     */
    public String exportToTextReport(List<SecurityEvent> events, Map<String, Object> statistics, String filename) throws IOException {
        if (filename == null || filename.isEmpty()) {
            filename = "security_report_" + LocalDateTime.now().format(FILE_DATE_FORMATTER) + ".txt";
        }
        
        if (!filename.endsWith(".txt")) {
            filename += ".txt";
        }

        try (FileWriter writer = new FileWriter(filename)) {
            // Write header
            writer.write("═══════════════════════════════════════════════════════════════\n");
            writer.write("        SECURITY REPORT - INTRUSION DETECTION SYSTEM\n");
            writer.write("═══════════════════════════════════════════════════════════════\n");
            writer.write("Generated: " + LocalDateTime.now().format(DATE_FORMATTER) + "\n");
            writer.write("Report Period: Last 24 Hours\n");
            writer.write("═══════════════════════════════════════════════════════════════\n\n");

            // Write statistics
            writer.write("SECURITY STATISTICS\n");
            writer.write("───────────────────────────────────────────────────────────────\n");
            writer.write(String.format("Total Events:        %s\n", statistics.get("totalEvents")));
            writer.write(String.format("Failed Logins:       %s\n", statistics.get("failedLogins")));
            writer.write(String.format("Successful Logins:   %s\n", statistics.get("successfulLogins")));
            writer.write(String.format("Critical Events:     %s\n", statistics.get("criticalEvents")));
            writer.write(String.format("Locked Accounts:     %s\n", statistics.get("lockedAccounts")));
            writer.write(String.format("Active Threats:      %s\n\n", statistics.get("activeThreats")));

            // Count events by severity
            long critical = events.stream().filter(e -> e.getSeverity() == Severity.CRITICAL).count();
            long high = events.stream().filter(e -> e.getSeverity() == Severity.HIGH).count();
            long medium = events.stream().filter(e -> e.getSeverity() == Severity.MEDIUM).count();
            long low = events.stream().filter(e -> e.getSeverity() == Severity.LOW).count();

            writer.write("EVENTS BY SEVERITY\n");
            writer.write("───────────────────────────────────────────────────────────────\n");
            writer.write(String.format("CRITICAL: %d events\n", critical));
            writer.write(String.format("HIGH:     %d events\n", high));
            writer.write(String.format("MEDIUM:   %d events\n", medium));
            writer.write(String.format("LOW:      %d events\n\n", low));

            // Write detailed events
            writer.write("DETAILED EVENT LOG\n");
            writer.write("═══════════════════════════════════════════════════════════════\n\n");

            for (SecurityEvent event : events) {
                writer.write(String.format("[%s] [%s] %s\n",
                    event.getTimestamp().format(DATE_FORMATTER),
                    event.getSeverity(),
                    event.getEventType()));
                writer.write(String.format("  Email: %s\n", event.getEmail()));
                writer.write(String.format("  Description: %s\n", event.getDescription()));
                if (event.getIpAddress() != null) {
                    writer.write(String.format("  IP Address: %s\n", event.getIpAddress()));
                }
                if (event.isBlocked()) {
                    writer.write("  Status: BLOCKED\n");
                }
                writer.write("\n");
            }

            writer.write("═══════════════════════════════════════════════════════════════\n");
            writer.write("                      END OF REPORT\n");
            writer.write("═══════════════════════════════════════════════════════════════\n");
        }

        return filename;
    }

    /**
     * Export summary report (statistics only)
     */
    public String exportSummaryReport(Map<String, Object> statistics, String filename) throws IOException {
        if (filename == null || filename.isEmpty()) {
            filename = "security_summary_" + LocalDateTime.now().format(FILE_DATE_FORMATTER) + ".txt";
        }
        
        if (!filename.endsWith(".txt")) {
            filename += ".txt";
        }

        try (FileWriter writer = new FileWriter(filename)) {
            writer.write("═══════════════════════════════════════════════════════════════\n");
            writer.write("           SECURITY SUMMARY - 24 HOUR OVERVIEW\n");
            writer.write("═══════════════════════════════════════════════════════════════\n");
            writer.write("Generated: " + LocalDateTime.now().format(DATE_FORMATTER) + "\n\n");

            writer.write(String.format("📊 Total Events:        %s\n", statistics.get("totalEvents")));
            writer.write(String.format("❌ Failed Logins:       %s\n", statistics.get("failedLogins")));
            writer.write(String.format("✅ Successful Logins:   %s\n", statistics.get("successfulLogins")));
            writer.write(String.format("🚨 Critical Events:     %s\n", statistics.get("criticalEvents")));
            writer.write(String.format("🔒 Locked Accounts:     %s\n", statistics.get("lockedAccounts")));
            writer.write(String.format("⚠️  Active Threats:      %s\n", statistics.get("activeThreats")));

            writer.write("\n═══════════════════════════════════════════════════════════════\n");
        }

        return filename;
    }

    /**
     * Escape CSV special characters
     */
    private String escapeCSV(String value) {
        if (value == null) {
            return "";
        }
        
        // If value contains comma, quote, or newline, wrap in quotes and escape quotes
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        
        return value;
    }
}
