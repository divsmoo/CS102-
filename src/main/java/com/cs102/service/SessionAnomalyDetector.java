package com.cs102.service;

import com.cs102.model.SecurityEventType;
import com.cs102.model.Severity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for detecting session anomalies and suspicious login patterns
 */
@Service
public class SessionAnomalyDetector {

    @Autowired
    private IntrusionDetectionService idsService;

    // Track active sessions per user
    private final Map<String, Set<String>> activeSessions = new ConcurrentHashMap<>();
    
    // Track login time patterns (email -> list of login hours)
    private final Map<String, LoginPattern> loginPatterns = new ConcurrentHashMap<>();
    
    // Configuration
    private static final int MAX_CONCURRENT_SESSIONS = 2;
    private static final int UNUSUAL_HOUR_START = 0;  // Midnight
    private static final int UNUSUAL_HOUR_END = 5;    // 5 AM
    private static final int MIN_LOGINS_FOR_PATTERN = 5; // Need 5 logins to establish pattern
    
    /**
     * Check for session anomalies when user logs in
     */
    public void checkLoginAnomaly(String email, String sessionId) {
        // Check for multiple concurrent sessions
        checkConcurrentSessions(email, sessionId);
        
        // Check for unusual login time
        checkUnusualLoginTime(email);
        
        // Update login pattern
        updateLoginPattern(email);
    }
    
    /**
     * Detect if user has too many concurrent sessions
     */
    private void checkConcurrentSessions(String email, String sessionId) {
        activeSessions.putIfAbsent(email, new HashSet<>());
        Set<String> sessions = activeSessions.get(email);
        sessions.add(sessionId);
        
        if (sessions.size() > MAX_CONCURRENT_SESSIONS) {
            idsService.logSecurityEvent(
                SecurityEventType.MULTIPLE_CONCURRENT_SESSIONS,
                Severity.HIGH,
                email,
                String.format("Multiple concurrent sessions detected: %d active sessions (max allowed: %d)", 
                    sessions.size(), MAX_CONCURRENT_SESSIONS)
            );
        }
    }
    
    /**
     * Check if login time is unusual based on historical patterns
     */
    private void checkUnusualLoginTime(String email) {
        LocalDateTime now = LocalDateTime.now();
        int currentHour = now.getHour();
        DayOfWeek currentDay = now.getDayOfWeek();
        
        // Check if logging in during unusual hours (midnight to 5 AM)
        if (currentHour >= UNUSUAL_HOUR_START && currentHour < UNUSUAL_HOUR_END) {
            // Check if user has a pattern of logging in during these hours
            LoginPattern pattern = loginPatterns.get(email);
            
            if (pattern != null && pattern.hasEstablishedPattern()) {
                // User has a pattern - check if night logins are normal for them
                if (!pattern.isNormalLoginTime(currentHour)) {
                    idsService.logSecurityEvent(
                        SecurityEventType.UNUSUAL_LOGIN_TIME,
                        Severity.MEDIUM,
                        email,
                        String.format("Unusual login time detected: %02d:%02d on %s (outside normal pattern)", 
                            currentHour, now.getMinute(), currentDay)
                    );
                }
            } else {
                // No pattern established yet, but still flag late-night login
                idsService.logSecurityEvent(
                    SecurityEventType.UNUSUAL_LOGIN_TIME,
                    Severity.LOW,
                    email,
                    String.format("Late-night login detected: %02d:%02d on %s", 
                        currentHour, now.getMinute(), currentDay)
                );
            }
        } else if (loginPatterns.containsKey(email)) {
            // Check against established pattern
            LoginPattern pattern = loginPatterns.get(email);
            if (pattern.hasEstablishedPattern() && !pattern.isNormalLoginTime(currentHour)) {
                // Significant deviation from normal pattern
                int avgHour = pattern.getAverageLoginHour();
                int deviation = Math.abs(currentHour - avgHour);
                
                if (deviation >= 6) { // 6+ hours difference
                    idsService.logSecurityEvent(
                        SecurityEventType.ANOMALOUS_LOGIN_PATTERN,
                        Severity.MEDIUM,
                        email,
                        String.format("Anomalous login time: %02d:%02d (typical: ~%02d:00, deviation: %d hours)", 
                            currentHour, now.getMinute(), avgHour, deviation)
                    );
                }
            }
        }
    }
    
    /**
     * Update login time pattern for user
     */
    private void updateLoginPattern(String email) {
        loginPatterns.putIfAbsent(email, new LoginPattern());
        LoginPattern pattern = loginPatterns.get(email);
        pattern.recordLogin(LocalDateTime.now());
    }
    
    /**
     * Remove session when user logs out
     */
    public void removeSession(String email, String sessionId) {
        Set<String> sessions = activeSessions.get(email);
        if (sessions != null) {
            sessions.remove(sessionId);
            if (sessions.isEmpty()) {
                activeSessions.remove(email);
            }
        }
    }
    
    /**
     * Get number of active sessions for user
     */
    public int getActiveSessionCount(String email) {
        Set<String> sessions = activeSessions.get(email);
        return sessions != null ? sessions.size() : 0;
    }
    
    /**
     * Get login pattern summary for user
     */
    public String getLoginPatternSummary(String email) {
        LoginPattern pattern = loginPatterns.get(email);
        if (pattern == null || !pattern.hasEstablishedPattern()) {
            return "No pattern established yet (need " + MIN_LOGINS_FOR_PATTERN + " logins)";
        }
        
        return String.format("Average login time: ~%02d:00, Total logins: %d, Pattern: %s",
            pattern.getAverageLoginHour(),
            pattern.getLoginCount(),
            pattern.getPatternDescription()
        );
    }
    
    /**
     * Inner class to track login patterns
     */
    private class LoginPattern {
        private final Map<Integer, Integer> hourFrequency = new HashMap<>();
        private int totalLogins = 0;
        
        public void recordLogin(LocalDateTime loginTime) {
            int hour = loginTime.getHour();
            hourFrequency.put(hour, hourFrequency.getOrDefault(hour, 0) + 1);
            totalLogins++;
        }
        
        public boolean hasEstablishedPattern() {
            return totalLogins >= MIN_LOGINS_FOR_PATTERN;
        }
        
        public boolean isNormalLoginTime(int hour) {
            if (!hasEstablishedPattern()) {
                return true; // No pattern yet, all times acceptable
            }
            
            // Check if this hour has been used before (within 2 hour window)
            for (int i = hour - 2; i <= hour + 2; i++) {
                int checkHour = (i + 24) % 24; // Handle wrap-around
                if (hourFrequency.getOrDefault(checkHour, 0) > 0) {
                    return true;
                }
            }
            
            return false;
        }
        
        public int getAverageLoginHour() {
            if (totalLogins == 0) return 12; // Default noon
            
            int weightedSum = 0;
            for (Map.Entry<Integer, Integer> entry : hourFrequency.entrySet()) {
                weightedSum += entry.getKey() * entry.getValue();
            }
            
            return weightedSum / totalLogins;
        }
        
        public int getLoginCount() {
            return totalLogins;
        }
        
        public String getPatternDescription() {
            if (!hasEstablishedPattern()) {
                return "Establishing...";
            }
            
            int avgHour = getAverageLoginHour();
            
            if (avgHour >= 6 && avgHour < 12) {
                return "Morning person (6 AM - 12 PM)";
            } else if (avgHour >= 12 && avgHour < 18) {
                return "Afternoon person (12 PM - 6 PM)";
            } else if (avgHour >= 18 && avgHour < 23) {
                return "Evening person (6 PM - 11 PM)";
            } else {
                return "Night owl (11 PM - 6 AM)";
            }
        }
    }
}
