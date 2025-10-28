package com.cs102.repository;

import com.cs102.model.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
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

    // Find sessions by course, section and time range (for sessions that fall within or overlap with the time range)
    @Query("SELECT s FROM Session s WHERE s.course = :course AND s.section = :section " +
           "AND s.date >= :startDate AND s.date <= :endDate " +
           "AND ((s.startTime >= :startTime AND s.startTime <= :endTime) " +
           "OR (s.endTime >= :startTime AND s.endTime <= :endTime) " +
           "OR (s.startTime <= :startTime AND s.endTime >= :endTime))")
    List<Session> findByCourseAndSectionAndTimeRange(
        @Param("course") String course,
        @Param("section") String section,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("startTime") LocalTime startTime,
        @Param("endTime") LocalTime endTime
    );
}
