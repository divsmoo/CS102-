package com.cs102.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "classes")
@IdClass(Class.ClassId.class)
public class Class {

    @Id
    @Column(name = "course", length = 50, nullable = false)
    private String course;

    @Id
    @Column(name = "section", length = 10, nullable = false)
    private String section;

    @Id
    @Column(name = "user_id", length = 20, nullable = false)
    private String userId;

    @Column(name = "enrolled_at", nullable = false)
    private LocalDateTime enrolledAt;

    public Class() {
        this.enrolledAt = LocalDateTime.now();
    }

    public Class(String course, String section, String userId) {
        this.course = course;
        this.section = section;
        this.userId = userId;
        this.enrolledAt = LocalDateTime.now();
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

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public LocalDateTime getEnrolledAt() {
        return enrolledAt;
    }

    public void setEnrolledAt(LocalDateTime enrolledAt) {
        this.enrolledAt = enrolledAt;
    }

    // Composite Primary Key Class
    public static class ClassId implements Serializable {
        private String course;
        private String section;
        private String userId;

        public ClassId() {
        }

        public ClassId(String course, String section, String userId) {
            this.course = course;
            this.section = section;
            this.userId = userId;
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

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ClassId classId = (ClassId) o;
            return Objects.equals(course, classId.course) &&
                   Objects.equals(section, classId.section) &&
                   Objects.equals(userId, classId.userId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(course, section, userId);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Class aClass = (Class) o;
        return Objects.equals(course, aClass.course) &&
               Objects.equals(section, aClass.section) &&
               Objects.equals(userId, aClass.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(course, section, userId);
    }
}
