package com.cs102.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Model class to represent a single row in the student attendance table
 * Contains student info and their attendance status for each week (1-13)
 */
public class StudentAttendanceData {

    private final StringProperty studentName;
    private final StringProperty studentId;
    private final StringProperty[] weeklyAttendance; // Array of 13 weeks

    public StudentAttendanceData(String studentName, String studentId) {
        this.studentName = new SimpleStringProperty(studentName);
        this.studentId = new SimpleStringProperty(studentId);
        this.weeklyAttendance = new StringProperty[13];

        // Initialize all weeks with empty strings
        for (int i = 0; i < 13; i++) {
            this.weeklyAttendance[i] = new SimpleStringProperty("-");
        }
    }

    // Getters for TableView columns
    public StringProperty studentNameProperty() {
        return studentName;
    }

    public String getStudentName() {
        return studentName.get();
    }

    public void setStudentName(String name) {
        this.studentName.set(name);
    }

    public StringProperty studentIdProperty() {
        return studentId;
    }

    public String getStudentId() {
        return studentId.get();
    }

    public void setStudentId(String id) {
        this.studentId.set(id);
    }

    // Week attendance getters and setters
    public StringProperty weekProperty(int weekNumber) {
        if (weekNumber < 1 || weekNumber > 13) {
            throw new IllegalArgumentException("Week number must be between 1 and 13");
        }
        return weeklyAttendance[weekNumber - 1];
    }

    public String getWeekAttendance(int weekNumber) {
        if (weekNumber < 1 || weekNumber > 13) {
            throw new IllegalArgumentException("Week number must be between 1 and 13");
        }
        return weeklyAttendance[weekNumber - 1].get();
    }

    public void setWeekAttendance(int weekNumber, String status) {
        if (weekNumber < 1 || weekNumber > 13) {
            throw new IllegalArgumentException("Week number must be between 1 and 13");
        }
        weeklyAttendance[weekNumber - 1].set(status);
    }

    // Calculate totals
    public int getTotalPresent() {
        int count = 0;
        for (int i = 0; i < 13; i++) {
            String status = weeklyAttendance[i].get();
            if ("P".equals(status)) {
                count++;
            }
        }
        return count;
    }

    public int getTotalLate() {
        int count = 0;
        for (int i = 0; i < 13; i++) {
            String status = weeklyAttendance[i].get();
            if ("L".equals(status)) {
                count++;
            }
        }
        return count;
    }

    public int getTotalAbsent() {
        int count = 0;
        for (int i = 0; i < 13; i++) {
            String status = weeklyAttendance[i].get();
            if ("A".equals(status)) {
                count++;
            }
        }
        return count;
    }

    public StringProperty totalPresentProperty() {
        return new SimpleStringProperty(String.valueOf(getTotalPresent()));
    }

    public StringProperty totalLateProperty() {
        return new SimpleStringProperty(String.valueOf(getTotalLate()));
    }

    public StringProperty totalAbsentProperty() {
        return new SimpleStringProperty(String.valueOf(getTotalAbsent()));
    }

    // Check if this row has any attendance data (P, L, or A)
    public boolean hasAnyAttendance() {
        for (int i = 0; i < 13; i++) {
            String status = weeklyAttendance[i].get();
            if ("P".equals(status) || "L".equals(status) || "A".equals(status)) {
                return true;
            }
        }
        return false;
    }
}
