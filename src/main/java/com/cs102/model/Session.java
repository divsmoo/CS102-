package com.cs102.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sessions")
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Column(name = "session_id", length = 50, unique = true, nullable = false)
    private String sessionId;  // e.g., "CS102-A-2025-01-15"

    @Column(name = "course", length = 50, nullable = false)
    private String course;

    @Column(name = "section", length = 10, nullable = false)
    private String section;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Session() {
        this.createdAt = LocalDateTime.now();
        this.date = LocalDate.now();
    }

    public Session(String sessionId, String course, String section, LocalDate date, LocalTime startTime, LocalTime endTime) {
        this.sessionId = sessionId;
        this.course = course;
        this.section = section;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getCourse() {
        return course;
    }

    public void setCourse(String course) {
        this.course = course;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
