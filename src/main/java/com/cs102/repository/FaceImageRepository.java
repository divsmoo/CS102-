package com.cs102.repository;

import com.cs102.model.FaceImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FaceImageRepository extends JpaRepository<FaceImage, UUID> {
    List<FaceImage> findByUserId(String userId);
    void deleteByUserId(String userId);
}
