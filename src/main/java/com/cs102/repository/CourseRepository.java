package com.cs102.repository;

import com.cs102.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Course.CourseId> {

    // Find a specific course by course code and section
    Optional<Course> findByCourseAndSection(String course, String section);

    // Find all courses taught by a specific professor
    List<Course> findByProfessorId(String professorId);

    // Find all sections of a specific course
    List<Course> findByCourse(String course);

    // Find all courses in a specific semester
    List<Course> findBySemester(String semester);

    // Check if a course/section exists
    boolean existsByCourseAndSection(String course, String section);

    // Delete a specific course/section
    void deleteByCourseAndSection(String course, String section);
}
