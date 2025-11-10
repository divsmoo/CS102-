package com.cs102.manager;

import com.cs102.model.AttendanceRecord;
import com.cs102.model.Class;
import com.cs102.model.Course;
import com.cs102.model.FaceImage;
import com.cs102.model.Session;
import com.cs102.model.User;
import com.cs102.model.UserRole;
import com.cs102.repository.AttendanceRecordRepository;
import com.cs102.repository.ClassRepository;
import com.cs102.repository.CourseRepository;
import com.cs102.repository.FaceImageRepository;
import com.cs102.repository.SessionRepository;
import com.cs102.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Autowired
    private FaceImageRepository faceImageRepository;

    @Autowired
    private ClassRepository classRepository;

    @Autowired
    private CourseRepository courseRepository;

    // ========== User Management ==========

    public User saveUser(User user) {
        return userRepository.save(user);
    }

    public Optional<User> findUserByUserId(String userId) {
        return userRepository.findByUserId(userId);
    }

    public Optional<User> findUserByDatabaseId(UUID databaseId) {
        return userRepository.findByDatabaseId(databaseId);
    }

    public Optional<User> findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Deprecated
    public Optional<User> findUserById(UUID id) {
        return userRepository.findByDatabaseId(id);
    }

    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    public List<User> findUsersByRole(UserRole role) {
        return userRepository.findByRole(role.name());
    }

    public boolean userExistsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public boolean userExistsByUserId(String userId) {
        return userRepository.existsByUserId(userId);
    }

    public void deleteUser(User user) {
        userRepository.delete(user);
    }

    // ========== Attendance Management ==========

    public AttendanceRecord saveAttendanceRecord(AttendanceRecord record) {
        return attendanceRecordRepository.save(record);
    }

    public List<AttendanceRecord> findAttendanceByUserId(String userId) {
        return attendanceRecordRepository.findByUserId(userId);
    }

    public List<AttendanceRecord> findAttendanceBySessionId(UUID sessionId) {
        return attendanceRecordRepository.findBySessionId(sessionId);
    }

    public Optional<AttendanceRecord> findAttendanceByUserIdAndSessionId(String userId, UUID sessionId) {
        return attendanceRecordRepository.findByUserIdAndSessionId(userId, sessionId);
    }

    public List<AttendanceRecord> findAttendanceByStatus(String attendance) {
        return attendanceRecordRepository.findByAttendance(attendance);
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

    public List<Session> findSessionsByCourseAndSection(String course, String section) {
        return sessionRepository.findByCourseAndSection(course, section);
    }

    public void deleteSession(Session session) {
        sessionRepository.delete(session);
    }

    public void deleteSessionBySessionId(String sessionId) {
        sessionRepository.deleteBySessionId(sessionId);
    }

    public boolean existsBySessionId(String sessionId) {
        return sessionRepository.existsBySessionId(sessionId);
    }

    public List<Session> findSessionsByCourseAndSectionAndTimeRange(String course, String section,
                                                                      java.time.LocalDate startDate,
                                                                      java.time.LocalDate endDate,
                                                                      java.time.LocalTime startTime,
                                                                      java.time.LocalTime endTime) {
        return sessionRepository.findByCourseAndSectionAndTimeRange(course, section, startDate, endDate, startTime, endTime);
    }

    // ========== Course Management ==========

    public Course saveCourse(Course course) {
        return courseRepository.save(course);
    }

    public Optional<Course> findCourseByCourseAndSection(String course, String section) {
        return courseRepository.findByCourseAndSection(course, section);
    }

    public List<Course> findCoursesByProfessorId(String professorId) {
        return courseRepository.findByProfessorId(professorId);
    }

    public List<Course> findAllCourses() {
        return courseRepository.findAll();
    }

    public boolean courseExists(String course, String section) {
        return courseRepository.existsByCourseAndSection(course, section);
    }

    @Transactional
    public void deleteCourse(String course, String section) {
        courseRepository.deleteByCourseAndSection(course, section);
    }

    // ========== Class Enrollment Management ==========

    public Class saveClassEnrollment(Class classEnrollment) {
        return classRepository.save(classEnrollment);
    }

    public List<Class> findEnrollmentsByUserId(String userId) {
        return classRepository.findByUserId(userId);
    }

    public List<Class> findEnrollmentsByCourseAndSection(String course, String section) {
        return classRepository.findByCourseAndSection(course, section);
    }

    public boolean isUserEnrolled(String course, String section, String userId) {
        return classRepository.existsByCourseAndSectionAndUserId(course, section, userId);
    }

    @Transactional
    public void deleteEnrollment(String course, String section, String userId) {
        classRepository.deleteByCourseAndSectionAndUserId(course, section, userId);
    }

    // ========== Face Image Management ==========

    @Transactional
    public void saveFaceImages(String userId, List<byte[]> faceImages) {
        // Delete existing face images for this student
        faceImageRepository.deleteByUserId(userId);

        // Save new face images
        for (int i = 0; i < faceImages.size(); i++) {
            FaceImage faceImage = new FaceImage(userId, faceImages.get(i), i + 1);
            faceImageRepository.save(faceImage);
        }

        System.out.println("Saved " + faceImages.size() + " face images for student: " + userId);
    }

    public List<FaceImage> findFaceImagesByUserId(String userId) {
        return faceImageRepository.findByUserId(userId);
    }

    @Transactional
    public void deleteFaceImagesByUserId(String userId) {
        faceImageRepository.deleteByUserId(userId);
    }

    // ========== Student Attendance View Helpers ==========

    /**
     * Get all courses that a student is enrolled in
     * @param userId The student's user ID
     * @return List of courses (only distinct course codes)
     */
    public List<String> getEnrolledCoursesForStudent(String userId) {
        List<Class> enrollments = classRepository.findByUserId(userId);
        return enrollments.stream()
                .map(Class::getCourse)
                .distinct()
                .sorted()
                .toList();
    }

    /**
     * Get attendance records for a student in a specific course
     * Returns a list of attendance records with their associated session information
     * @param userId The student's user ID
     * @param course The course code
     * @return List of attendance records
     */
    public List<AttendanceRecord> getAttendanceForStudentInCourse(String userId, String course) {
        // Get all sessions for this course (across all sections)
        List<Session> sessions = sessionRepository.findByCourse(course);

        // Get attendance records for this student for these sessions
        List<UUID> sessionIds = sessions.stream().map(Session::getId).toList();

        return attendanceRecordRepository.findByUserId(userId).stream()
                .filter(record -> sessionIds.contains(record.getSessionId()))
                .toList();
    }

    // ========== Analytics Dashboard Helpers ==========

    /**
     * Get attendance statistics for a specific course/section
     * Returns counts of Present, Late, and Absent
     * @param course The course code
     * @param section The section
     * @return Map with keys "Present", "Late", "Absent" and their counts
     */
    public java.util.Map<String, Integer> getAttendanceStatsByCourse(String course, String section) {
        List<Session> sessions = findSessionsByCourseAndSection(course, section);
        List<UUID> sessionIds = sessions.stream().map(Session::getId).toList();

        List<AttendanceRecord> records = attendanceRecordRepository.findAll().stream()
                .filter(record -> sessionIds.contains(record.getSessionId()))
                .toList();

        java.util.Map<String, Integer> stats = new java.util.HashMap<>();
        stats.put("Present", 0);
        stats.put("Late", 0);
        stats.put("Absent", 0);

        for (AttendanceRecord record : records) {
            String status = record.getAttendance();
            stats.put(status, stats.get(status) + 1);
        }

        return stats;
    }

    /**
     * Get attendance statistics for all courses taught by a professor
     * @param professorId The professor's user ID
     * @return Map with course/section as key and attendance stats as value
     */
    public java.util.Map<String, java.util.Map<String, Integer>> getAttendanceStatsByProfessor(String professorId) {
        List<Course> courses = findCoursesByProfessorId(professorId);
        java.util.Map<String, java.util.Map<String, Integer>> allStats = new java.util.HashMap<>();

        for (Course course : courses) {
            String key = course.getCourse() + " " + course.getSection();
            allStats.put(key, getAttendanceStatsByCourse(course.getCourse(), course.getSection()));
        }

        return allStats;
    }

    /**
     * Get attendance trends over time for a course/section
     * Returns weekly attendance counts
     * @param course The course code
     * @param section The section
     * @return List of maps containing week number and attendance counts
     */
    public List<java.util.Map<String, Object>> getAttendanceTrendsByCourse(String course, String section) {
        List<Session> sessions = findSessionsByCourseAndSection(course, section);
        sessions.sort((s1, s2) -> s1.getDate().compareTo(s2.getDate()));

        List<java.util.Map<String, Object>> trends = new java.util.ArrayList<>();

        for (int i = 0; i < sessions.size(); i++) {
            Session session = sessions.get(i);
            List<AttendanceRecord> records = findAttendanceBySessionId(session.getId());

            int presentCount = 0;
            int lateCount = 0;
            int absentCount = 0;

            for (AttendanceRecord record : records) {
                switch (record.getAttendance()) {
                    case "Present": presentCount++; break;
                    case "Late": lateCount++; break;
                    case "Absent": absentCount++; break;
                }
            }

            java.util.Map<String, Object> weekData = new java.util.HashMap<>();
            weekData.put("week", i + 1);
            weekData.put("date", session.getDate().toString());
            weekData.put("present", presentCount);
            weekData.put("late", lateCount);
            weekData.put("absent", absentCount);
            weekData.put("total", records.size());

            trends.add(weekData);
        }

        return trends;
    }

    /**
     * Get overall attendance rate for a course/section
     * @param course The course code
     * @param section The section
     * @return Attendance rate as a percentage (0-100)
     */
    public double getAttendanceRate(String course, String section) {
        java.util.Map<String, Integer> stats = getAttendanceStatsByCourse(course, section);
        int total = stats.values().stream().mapToInt(Integer::intValue).sum();

        if (total == 0) return 0.0;

        int present = stats.get("Present");
        return (present * 100.0) / total;
    }
}

