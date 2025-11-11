package com.cs102.repository;

import com.cs102.model.SecurityEvent;
import com.cs102.model.SecurityEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface SecurityEventRepository extends JpaRepository<SecurityEvent, UUID> {
    
    // Find all events for a specific email
    List<SecurityEvent> findByEmail(String email);
    
    // Find events by type
    List<SecurityEvent> findByEventType(SecurityEventType eventType);
    
    // Find failed login attempts for an email within a time window
    List<SecurityEvent> findByEmailAndEventTypeAndTimestampAfter(
        String email, 
        SecurityEventType eventType, 
        LocalDateTime timestamp
    );
    
    // Find all blocked events
    List<SecurityEvent> findByBlocked(boolean blocked);
    
    // Find recent events (last 24 hours)
    List<SecurityEvent> findByTimestampAfter(LocalDateTime timestamp);
    
    // Count failed login attempts for an email in a time window
    long countByEmailAndEventTypeAndTimestampAfter(
        String email, 
        SecurityEventType eventType, 
        LocalDateTime timestamp
    );
}
