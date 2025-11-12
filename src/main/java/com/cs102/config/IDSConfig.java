package com.cs102.config;

import com.cs102.service.IntrusionDetectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * IDS Configuration Helper
 * 
 * This class provides easy access to IDS configuration and settings.
 * Modify the constants below to adjust security thresholds.
 */
@Component
public class IDSConfig {

    @Autowired
    private IntrusionDetectionService idsService;

    // ==================== CONFIGURATION PARAMETERS ====================
    
    /**
     * Maximum failed login attempts before account lockout
     * Default: 5 attempts
     * Recommended range: 3-10
     */
    public static final int MAX_FAILED_ATTEMPTS = 5;

    /**
     * Duration of account lockout in minutes
     * Default: 15 minutes
     * Recommended range: 5-60
     */
    public static final int LOCKOUT_DURATION_MINUTES = 15;

    /**
     * Time window to count failed login attempts (in minutes)
     * Default: 10 minutes
     * Recommended range: 5-30
     */
    public static final int FAILED_ATTEMPT_WINDOW_MINUTES = 10;

    /**
     * Maximum registration attempts from same email in 5 minutes
     * Default: 3 attempts
     * Recommended range: 2-5
     */
    public static final int MAX_REGISTRATION_ATTEMPTS = 3;

    /**
     * Enable/disable SQL injection detection
     * Default: true (recommended to keep enabled)
     */
    public static final boolean ENABLE_SQL_INJECTION_DETECTION = true;

    /**
     * Enable/disable console logging of security events
     * Default: true
     */
    public static final boolean ENABLE_SECURITY_LOGGING = true;

    /**
     * Automatically send alerts for critical events
     * Default: false (set to true to enable email alerts)
     */
    public static final boolean AUTO_ALERT_CRITICAL_EVENTS = false;

    // ==================== SEVERITY THRESHOLDS ====================

    /**
     * Number of failed attempts to trigger HIGH severity warning
     * Default: 3 attempts
     */
    public static final int HIGH_SEVERITY_THRESHOLD = 3;

    /**
     * Number of failed attempts to trigger CRITICAL severity alert
     * Default: 5 attempts
     */
    public static final int CRITICAL_SEVERITY_THRESHOLD = 5;

    // ==================== GETTER METHODS ====================

    public IntrusionDetectionService getIdsService() {
        return idsService;
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Get configuration summary as String
     */
    public static String getConfigSummary() {
        return String.format(
            "=== IDS Configuration ===\n" +
            "Max Failed Attempts: %d\n" +
            "Lockout Duration: %d minutes\n" +
            "Failed Attempt Window: %d minutes\n" +
            "Max Registration Attempts: %d\n" +
            "SQL Injection Detection: %s\n" +
            "Security Logging: %s\n" +
            "Auto Alert Critical: %s\n" +
            "High Severity Threshold: %d\n" +
            "Critical Severity Threshold: %d",
            MAX_FAILED_ATTEMPTS,
            LOCKOUT_DURATION_MINUTES,
            FAILED_ATTEMPT_WINDOW_MINUTES,
            MAX_REGISTRATION_ATTEMPTS,
            ENABLE_SQL_INJECTION_DETECTION ? "Enabled" : "Disabled",
            ENABLE_SECURITY_LOGGING ? "Enabled" : "Disabled",
            AUTO_ALERT_CRITICAL_EVENTS ? "Enabled" : "Disabled",
            HIGH_SEVERITY_THRESHOLD,
            CRITICAL_SEVERITY_THRESHOLD
        );
    }

    /**
     * Print configuration to console
     */
    public static void printConfig() {
        System.out.println("\n" + getConfigSummary() + "\n");
    }
}
