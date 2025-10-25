package com.cs102.manager;

import com.cs102.model.AttendanceRecord;
import com.cs102.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
public class AttendanceManager {

    @Autowired
    private DatabaseManager databaseManager;

    /**
     * Check in a user - creates a new attendance record with current timestamp
     * @param user The user checking in
     * @return Optional<AttendanceRecord> containing the created record if successful
     */
    public Optional<AttendanceRecord> checkIn(User user) {
        try {
            // Check if user already has an active check-in today (no check-out yet)
            Optional<AttendanceRecord> activeRecord = findActiveTodayRecord(user);
            if (activeRecord.isPresent()) {
                System.err.println("User already has an active check-in today");
                return Optional.empty();
            }

            // Create new attendance record
            LocalDateTime now = LocalDateTime.now();
            String status = determineStatus(now);

            AttendanceRecord record = new AttendanceRecord(user, now, status);
            databaseManager.saveAttendanceRecord(record);

            System.out.println("User " + user.getName() + " checked in at " + now + " with status: " + status);
            return Optional.of(record);
        } catch (Exception e) {
            System.err.println("Error during check-in: " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }

    /**
     * Check out a user - updates the most recent attendance record with check-out time
     * @param user The user checking out
     * @return Optional<AttendanceRecord> containing the updated record if successful
     */
    public Optional<AttendanceRecord> checkOut(User user) {
        try {
            // Find active record (checked in but not checked out)
            Optional<AttendanceRecord> activeRecord = findActiveTodayRecord(user);

            if (activeRecord.isEmpty()) {
                System.err.println("No active check-in found for user");
                return Optional.empty();
            }

            // Update with check-out time
            AttendanceRecord record = activeRecord.get();
            LocalDateTime now = LocalDateTime.now();
            record.setCheckOutTime(now);
            databaseManager.saveAttendanceRecord(record);

            System.out.println("User " + user.getName() + " checked out at " + now);
            return Optional.of(record);
        } catch (Exception e) {
            System.err.println("Error during check-out: " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }

    /**
     * Find active attendance record for today (checked in but not checked out)
     * @param user The user to check
     * @return Optional<AttendanceRecord> if found
     */
    private Optional<AttendanceRecord> findActiveTodayRecord(User user) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        List<AttendanceRecord> todayRecords = databaseManager.findAttendanceByUserAndDateRange(
            user, startOfDay, endOfDay
        );

        // Find record with no check-out time
        return todayRecords.stream()
            .filter(record -> record.getCheckOutTime() == null)
            .findFirst();
    }

    /**
     * Get all attendance records for a user
     * @param user The user
     * @return List of attendance records
     */
    public List<AttendanceRecord> getUserAttendanceHistory(User user) {
        return databaseManager.findAttendanceByUser(user);
    }

    /**
     * Get attendance records for a user within a date range
     * @param user The user
     * @param startDate Start date
     * @param endDate End date
     * @return List of attendance records
     */
    public List<AttendanceRecord> getUserAttendanceByDateRange(User user, LocalDateTime startDate, LocalDateTime endDate) {
        return databaseManager.findAttendanceByUserAndDateRange(user, startDate, endDate);
    }

    /**
     * Check if user has already checked in today
     * @param user The user to check
     * @return true if user has an active check-in today
     */
    public boolean hasCheckedInToday(User user) {
        return findActiveTodayRecord(user).isPresent();
    }

    /**
     * Determine attendance status based on check-in time
     * Can be customized based on business rules (e.g., late threshold)
     * @param checkInTime The check-in time
     * @return Status string (PRESENT, LATE, etc.)
     */
    private String determineStatus(LocalDateTime checkInTime) {
        // Simple implementation - always PRESENT
        // TODO: Add logic for LATE, EXCUSED based on session start time
        return "PRESENT";
    }
}
