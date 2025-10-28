package com.cs102.repository;

import com.cs102.model.Class;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClassRepository extends JpaRepository<Class, Class.ClassId> {

    // Find all enrollments for a specific student
    List<Class> findByUserId(String userId);

    // Find all students in a specific course and section
    List<Class> findByCourseAndSection(String course, String section);

    // Find all courses for a specific student
    List<Class> findDistinctCourseByUserId(String userId);

    // Check if a student is enrolled in a specific course/section
    boolean existsByCourseAndSectionAndUserId(String course, String section, String userId);

    // Delete enrollment
    void deleteByCourseAndSectionAndUserId(String course, String section, String userId);
}
