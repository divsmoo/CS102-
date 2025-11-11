package com.cs102.service;

import com.cs102.model.SecurityEvent;
import com.cs102.model.SecurityEventType;
import com.cs102.model.Severity;
import com.cs102.repository.SecurityEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class IntrusionDetectionService {

    @Autowired
    private SecurityEventRepository securityEventRepository;
    
    // Background scheduler for auto-unlock
    private ScheduledExecutorService scheduler;
    
    // Alert listeners for real-time notifications
    private final List<SecurityAlertListener> alertListeners = new ArrayList<>();

    // Configuration
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_DURATION_MINUTES = 15;
    private static final int FAILED_ATTEMPT_WINDOW_MINUTES = 10;
    
    // In-memory tracking for locked accounts
    private final Map<String, LocalDateTime> lockedAccounts = new ConcurrentHashMap<>();
    private final Map<String, Integer> failedAttempts = new ConcurrentHashMap<>();

    /**
     * Initialize the IDS service - start auto-unlock scheduler
     */
    @PostConstruct
    public void init() {
        scheduler = Executors.newScheduledThreadPool(1);
        // Check for expired lockouts every minute
        scheduler.scheduleAtFixedRate(this::checkAndUnlockExpiredAccounts, 1, 1, TimeUnit.MINUTES);
        System.out.println("🔒 IDS Service initialized - Auto-unlock scheduler started");
    }

    /**
     * Cleanup when service is destroyed
     */
    @PreDestroy
    public void cleanup() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            System.out.println("🔒 IDS Service shutdown - Auto-unlock scheduler stopped");
        }
    }

    /**
     * Check and unlock accounts whose lockout period has expired
     */
    private void checkAndUnlockExpiredAccounts() {
        List<String> accountsToUnlock = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        for (Map.Entry<String, LocalDateTime> entry : lockedAccounts.entrySet()) {
            LocalDateTime lockoutTime = entry.getValue();
            if (now.isAfter(lockoutTime.plusMinutes(LOCKOUT_DURATION_MINUTES))) {
                accountsToUnlock.add(entry.getKey());
            }
        }
        
        for (String email : accountsToUnlock) {
            unlockAccount(email);
        }
    }

    /**
     * Register a listener for real-time security alerts
     */
    public void addSecurityAlertListener(SecurityAlertListener listener) {
        if (listener != null && !alertListeners.contains(listener)) {
            alertListeners.add(listener);
        }
    }

    /**
     * Remove a security alert listener
     */
    public void removeSecurityAlertListener(SecurityAlertListener listener) {
        alertListeners.remove(listener);
    }

    /**
     * Notify all listeners of a security event
     */
    private void notifyAlertListeners(SecurityEvent event) {
        for (SecurityAlertListener listener : alertListeners) {
            try {
                listener.onSecurityAlert(event);
            } catch (Exception e) {
                System.err.println("Error notifying alert listener: " + e.getMessage());
            }
        }
    }

    /**
     * Log a security event
     */
    public void logSecurityEvent(SecurityEventType eventType, Severity severity, String email, String description) {
        SecurityEvent event = new SecurityEvent(eventType, severity, email, description);
        securityEventRepository.save(event);
        
        System.out.println("🔒 IDS Alert: " + event);
        
        // Notify listeners for real-time alerts
        notifyAlertListeners(event);
    }

    /**
     * Check if account is locked due to failed login attempts
     */
    public boolean isAccountLocked(String email) {
        LocalDateTime lockoutTime = lockedAccounts.get(email);
        
        if (lockoutTime != null) {
            // Check if lockout period has expired
            if (LocalDateTime.now().isAfter(lockoutTime.plusMinutes(LOCKOUT_DURATION_MINUTES))) {
                unlockAccount(email);
                return false;
            }
            return true;
        }
        
        return false;
    }

    /**
     * Record a failed login attempt
     */
    public void recordFailedLogin(String email) {
        // Check recent failed attempts from database
        LocalDateTime timeWindow = LocalDateTime.now().minusMinutes(FAILED_ATTEMPT_WINDOW_MINUTES);
        long recentFailures = securityEventRepository.countByEmailAndEventTypeAndTimestampAfter(
            email, 
            SecurityEventType.FAILED_LOGIN, 
            timeWindow
        );

        // Log the failed attempt
        logSecurityEvent(
            SecurityEventType.FAILED_LOGIN, 
            Severity.MEDIUM, 
            email, 
            "Failed login attempt"
        );

        // Update in-memory counter
        int attempts = failedAttempts.getOrDefault(email, 0) + 1;
        failedAttempts.put(email, attempts);

        // Check if we should lock the account
        if (recentFailures >= MAX_FAILED_ATTEMPTS - 1) {
            lockAccount(email);
            logSecurityEvent(
                SecurityEventType.BRUTE_FORCE_ATTACK, 
                Severity.CRITICAL, 
                email, 
                String.format("Brute force attack detected: %d failed attempts in %d minutes", 
                    recentFailures + 1, FAILED_ATTEMPT_WINDOW_MINUTES)
            );
        } else if (recentFailures >= 3) {
            logSecurityEvent(
                SecurityEventType.FAILED_LOGIN, 
                Severity.HIGH, 
                email, 
                String.format("Multiple failed login attempts: %d in %d minutes", 
                    recentFailures + 1, FAILED_ATTEMPT_WINDOW_MINUTES)
            );
        }
    }

    /**
     * Record a successful login
     */
    public void recordSuccessfulLogin(String email) {
        // Clear failed attempts
        failedAttempts.remove(email);
        
        logSecurityEvent(
            SecurityEventType.SUCCESSFUL_LOGIN, 
            Severity.LOW, 
            email, 
            "Successful login"
        );
    }

    /**
     * Lock an account
     */
    public void lockAccount(String email) {
        lockedAccounts.put(email, LocalDateTime.now());
        
        logSecurityEvent(
            SecurityEventType.ACCOUNT_LOCKED, 
            Severity.CRITICAL, 
            email, 
            String.format("Account locked due to too many failed login attempts. Lockout duration: %d minutes", 
                LOCKOUT_DURATION_MINUTES)
        );
    }

    /**
     * Unlock an account
     */
    public void unlockAccount(String email) {
        lockedAccounts.remove(email);
        failedAttempts.remove(email);
        
        logSecurityEvent(
            SecurityEventType.ACCOUNT_UNLOCKED, 
            Severity.LOW, 
            email, 
            "Account unlocked after lockout period expired"
        );
    }

    /**
     * Get remaining lockout time in minutes
     */
    public long getRemainingLockoutTime(String email) {
        LocalDateTime lockoutTime = lockedAccounts.get(email);
        if (lockoutTime == null) {
            return 0;
        }
        
        LocalDateTime unlockTime = lockoutTime.plusMinutes(LOCKOUT_DURATION_MINUTES);
        long minutesRemaining = java.time.Duration.between(LocalDateTime.now(), unlockTime).toMinutes();
        
        return Math.max(0, minutesRemaining);
    }

    /**
     * Detect suspicious registration patterns
     */
    public boolean detectSuspiciousRegistration(String email) {
        // Check if there are multiple registration attempts
        LocalDateTime timeWindow = LocalDateTime.now().minusMinutes(5);
        long recentRegistrations = securityEventRepository.countByEmailAndEventTypeAndTimestampAfter(
            email, 
            SecurityEventType.SUSPICIOUS_REGISTRATION, 
            timeWindow
        );

        if (recentRegistrations > 3) {
            logSecurityEvent(
                SecurityEventType.MULTIPLE_REGISTRATIONS, 
                Severity.HIGH, 
                email, 
                "Multiple registration attempts detected in short time period"
            );
            return true;
        }

        return false;
    }

    /**
     * Validate input for SQL injection attempts
     */
    public boolean detectSQLInjection(String input) {
        if (input == null) {
            return false;
        }

        // Common SQL injection patterns
        String[] sqlPatterns = {
            "(?i).*('|(\\-\\-)|(;)|(\\|\\|)|(\\*)).*",
            "(?i).*(union|select|insert|update|delete|drop|create|alter|exec|execute|script|javascript).*"
        };

        for (String pattern : sqlPatterns) {
            if (input.matches(pattern)) {
                logSecurityEvent(
                    SecurityEventType.SQL_INJECTION_ATTEMPT, 
                    Severity.CRITICAL, 
                    "N/A", 
                    "SQL injection attempt detected: " + input.substring(0, Math.min(100, input.length()))
                );
                return true;
            }
        }

        return false;
    }

    /**
     * Get all recent security events
     */
    public List<SecurityEvent> getRecentEvents(int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return securityEventRepository.findByTimestampAfter(since);
    }

    /**
     * Get events for a specific email
     */
    public List<SecurityEvent> getEventsForEmail(String email) {
        return securityEventRepository.findByEmail(email);
    }

    /**
     * Get all critical events
     */
    public List<SecurityEvent> getCriticalEvents() {
        return securityEventRepository.findByBlocked(true);
    }

    /**
     * Get security statistics
     */
    public Map<String, Object> getSecurityStatistics() {
        LocalDateTime last24Hours = LocalDateTime.now().minusHours(24);
        List<SecurityEvent> recentEvents = securityEventRepository.findByTimestampAfter(last24Hours);

        long failedLogins = recentEvents.stream()
            .filter(e -> e.getEventType() == SecurityEventType.FAILED_LOGIN)
            .count();

        long successfulLogins = recentEvents.stream()
            .filter(e -> e.getEventType() == SecurityEventType.SUCCESSFUL_LOGIN)
            .count();

        long criticalEvents = recentEvents.stream()
            .filter(e -> e.getSeverity() == Severity.CRITICAL || e.getSeverity() == Severity.HIGH)
            .count();

        long lockedAccountsCount = recentEvents.stream()
            .filter(e -> e.getEventType() == SecurityEventType.ACCOUNT_LOCKED)
            .map(SecurityEvent::getEmail)
            .distinct()
            .count();

        return Map.of(
            "totalEvents", recentEvents.size(),
            "failedLogins", failedLogins,
            "successfulLogins", successfulLogins,
            "criticalEvents", criticalEvents,
            "lockedAccounts", lockedAccountsCount,
            "activeThreats", criticalEvents
        );
    }
}
