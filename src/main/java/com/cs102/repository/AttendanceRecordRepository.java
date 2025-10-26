package com.cs102.repository;

import com.cs102.model.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, UUID> {

    // Find all attendance records for a specific student
    List<AttendanceRecord> findByUserId(String userId);

    // Find all attendance records for a specific session
    List<AttendanceRecord> findBySessionId(UUID sessionId);

    // Find a specific student's attendance for a specific session
    Optional<AttendanceRecord> findByUserIdAndSessionId(String userId, UUID sessionId);

    // Find all attendance records by status (Present, Late, Absent)
    List<AttendanceRecord> findByAttendance(String attendance);

    // Find attendance records for a session by status
    List<AttendanceRecord> findBySessionIdAndAttendance(UUID sessionId, String attendance);

    // Find attendance records by method (Auto, Manual)
    List<AttendanceRecord> findByMethod(String method);

    // Check if attendance record exists for a student in a session
    boolean existsByUserIdAndSessionId(String userId, UUID sessionId);

    // Delete attendance record for a specific student and session
    void deleteByUserIdAndSessionId(String userId, UUID sessionId);
}
