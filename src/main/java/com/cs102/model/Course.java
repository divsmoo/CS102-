package com.cs102.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "courses")
@IdClass(Course.CourseId.class)
public class Course {

    @Id
    @Column(name = "course", length = 50, nullable = false)
    private String course;

    @Id
    @Column(name = "section", length = 10, nullable = false)
    private String section;

    @Column(name = "professor_id", length = 20)
    private String professorId;

    @Column(name = "semester", length = 20)
    private String semester;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Course() {
        this.createdAt = LocalDateTime.now();
    }

    public Course(String course, String section, String professorId, String semester) {
        this.course = course;
        this.section = section;
        this.professorId = professorId;
        this.semester = semester;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getCourse() {
        return course;
    }

    public void setCourse(String course) {
        this.course = course;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getProfessorId() {
        return professorId;
    }

    public void setProfessorId(String professorId) {
        this.professorId = professorId;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // Composite Primary Key Class
    public static class CourseId implements Serializable {
        private String course;
        private String section;

        public CourseId() {
        }

        public CourseId(String course, String section) {
            this.course = course;
            this.section = section;
        }

        // Getters and Setters
        public String getCourse() {
            return course;
        }

        public void setCourse(String course) {
            this.course = course;
        }

        public String getSection() {
            return section;
        }

        public void setSection(String section) {
            this.section = section;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CourseId courseId = (CourseId) o;
            return Objects.equals(course, courseId.course) &&
                   Objects.equals(section, courseId.section);
        }

        @Override
        public int hashCode() {
            return Objects.hash(course, section);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Course course1 = (Course) o;
        return Objects.equals(course, course1.course) &&
               Objects.equals(section, course1.section);
    }

    @Override
    public int hashCode() {
        return Objects.hash(course, section);
    }
}
