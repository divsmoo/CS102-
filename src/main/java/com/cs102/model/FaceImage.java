package com.cs102.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "face_images")
public class FaceImage {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "user_id", length = 20, nullable = false)
    private String userId;  // Foreign key to profiles.user_id

    @Column(name = "image_data", columnDefinition = "bytea", nullable = false)
    private byte[] imageData;

    @Column(name = "image_number")
    private Integer imageNumber;

    public FaceImage() {
    }

    public FaceImage(String userId, byte[] imageData, Integer imageNumber) {
        this.userId = userId;
        this.imageData = imageData;
        this.imageNumber = imageNumber;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public byte[] getImageData() {
        return imageData;
    }

    public void setImageData(byte[] imageData) {
        this.imageData = imageData;
    }

    public Integer getImageNumber() {
        return imageNumber;
    }

    public void setImageNumber(Integer imageNumber) {
        this.imageNumber = imageNumber;
    }
}
