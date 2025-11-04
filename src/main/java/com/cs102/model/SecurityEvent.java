package com.cs102.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "security_events")
public class SecurityEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "event_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private SecurityEventType eventType;

    @Column(name = "severity", nullable = false)
    @Enumerated(EnumType.STRING)
    private Severity severity;

    @Column(name = "email")
    private String email;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "blocked")
    private boolean blocked;

    public SecurityEvent() {
        this.timestamp = LocalDateTime.now();
    }

    public SecurityEvent(SecurityEventType eventType, Severity severity, String email, String description) {
        this.eventType = eventType;
        this.severity = severity;
        this.email = email;
        this.description = description;
        this.timestamp = LocalDateTime.now();
        this.blocked = false;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public SecurityEventType getEventType() {
        return eventType;
    }

    public void setEventType(SecurityEventType eventType) {
        this.eventType = eventType;
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s - %s: %s (Email: %s, IP: %s, Blocked: %s)",
                timestamp, severity, eventType, description, email, ipAddress, blocked);
    }
}
