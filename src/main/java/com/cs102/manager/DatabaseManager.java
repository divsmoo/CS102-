package com.cs102.manager;

import com.cs102.model.AttendanceRecord;
import com.cs102.model.Session;
import com.cs102.model.User;
import com.cs102.model.UserRole;
import com.cs102.repository.AttendanceRecordRepository;
import com.cs102.repository.SessionRepository;
import com.cs102.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class DatabaseManager {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AttendanceRecordRepository attendanceRecordRepository;

    @Autowired
    private SessionRepository sessionRepository;

    // ========== User Management ==========

    public User saveUser(User user) {
        return userRepository.save(user);
    }

    public Optional<User> findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findUserById(UUID id) {
        return userRepository.findById(id);
    }

    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    public List<User> findUsersByRole(UserRole role) {
        return userRepository.findByRole(role);
    }

    public boolean userExistsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public void deleteUser(User user) {
        userRepository.delete(user);
    }

    // ========== Attendance Management ==========

    public AttendanceRecord saveAttendanceRecord(AttendanceRecord record) {
        return attendanceRecordRepository.save(record);
    }

    public List<AttendanceRecord> findAttendanceByUser(User user) {
        return attendanceRecordRepository.findByUser(user);
    }

    public List<AttendanceRecord> findAttendanceByUserAndDateRange(User user, LocalDateTime start, LocalDateTime end) {
        return attendanceRecordRepository.findByUserAndCheckInTimeBetween(user, start, end);
    }

    public List<AttendanceRecord> findAllAttendanceInDateRange(LocalDateTime start, LocalDateTime end) {
        return attendanceRecordRepository.findByCheckInTimeBetween(start, end);
    }

    public List<AttendanceRecord> findAllAttendanceRecords() {
        return attendanceRecordRepository.findAll();
    }

    public void deleteAttendanceRecord(AttendanceRecord record) {
        attendanceRecordRepository.delete(record);
    }

    // ========== Session Management ==========

    public Session saveSession(Session session) {
        return sessionRepository.save(session);
    }

    public Optional<Session> findSessionBySessionId(String sessionId) {
        return sessionRepository.findBySessionId(sessionId);
    }

    public Optional<Session> findActiveSessionByUser(User user) {
        return sessionRepository.findByUserAndActiveTrue(user);
    }

    public void deleteSession(Session session) {
        sessionRepository.delete(session);
    }

    public void deleteSessionBySessionId(String sessionId) {
        sessionRepository.deleteBySessionId(sessionId);
    }
}

