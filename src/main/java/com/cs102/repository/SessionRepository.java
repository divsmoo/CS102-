package com.cs102.repository;

import com.cs102.model.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SessionRepository extends JpaRepository<Session, UUID> {

    // Find by session_id (e.g., "CS102-A-2025-01-15")
    Optional<Session> findBySessionId(String sessionId);

    // Find all sessions for a specific course and section
    List<Session> findByCourseAndSection(String course, String section);

    // Find all sessions for a specific course
    List<Session> findByCourse(String course);

    // Find sessions by date
    List<Session> findByDate(LocalDate date);

    // Find sessions by course, section, and date
    Optional<Session> findByCourseAndSectionAndDate(String course, String section, LocalDate date);

    // Delete by session_id
    void deleteBySessionId(String sessionId);

    // Check if session exists
    boolean existsBySessionId(String sessionId);
}
