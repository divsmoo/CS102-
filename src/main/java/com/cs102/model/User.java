package com.cs102.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "profiles", schema = "public")
public class User {

    @Id
    @Column(name = "user_id", length = 20, nullable = false)
    private String userId;  // Primary key: S12345

    @Column(name = "database_id", unique = true, nullable = false, columnDefinition = "UUID")
    private UUID databaseId;  // Links to auth.users.id

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserRole role;

    @Column(name = "face_image", columnDefinition = "bytea")
    private byte[] faceImage;

    @Column(name = "late_threshold")
    private Integer lateThreshold = 15; // Default late threshold in minutes for professors

    public User() {
    }

    public User(String userId, UUID databaseId, String email, String name, UserRole role) {
        this.userId = userId;
        this.databaseId = databaseId;
        this.email = email;
        this.name = name;
        this.role = role;
    }

    // Getters and Setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public UUID getDatabaseId() {
        return databaseId;
    }

    public void setDatabaseId(UUID databaseId) {
        this.databaseId = databaseId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public byte[] getFaceImage() {
        return faceImage;
    }

    public void setFaceImage(byte[] faceImage) {
        this.faceImage = faceImage;
    }

    public Integer getLateThreshold() {
        return lateThreshold != null ? lateThreshold : 15;
    }

    public void setLateThreshold(Integer lateThreshold) {
        this.lateThreshold = lateThreshold;
    }
}
