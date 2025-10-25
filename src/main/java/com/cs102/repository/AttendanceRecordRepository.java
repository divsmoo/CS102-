package com.cs102.repository;

import com.cs102.model.AttendanceRecord;
import com.cs102.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {

    List<AttendanceRecord> findByUser(User user);

    List<AttendanceRecord> findByUserAndCheckInTimeBetween(User user, LocalDateTime start, LocalDateTime end);

    List<AttendanceRecord> findByCheckInTimeBetween(LocalDateTime start, LocalDateTime end);
}
