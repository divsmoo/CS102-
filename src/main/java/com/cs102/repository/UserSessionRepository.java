package com.cs102.repository;

import com.cs102.model.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {

    // Find all active sessions for a user
    @Query("SELECT us FROM UserSession us WHERE us.email = :email AND us.isActive = true")
    List<UserSession> findActiveSessionsByEmail(@Param("email") String email);

    // Count active sessions for a user
    @Query("SELECT COUNT(us) FROM UserSession us WHERE us.email = :email AND us.isActive = true")
    long countActiveSessionsByEmail(@Param("email") String email);

    // Find all sessions for a user in a time range (for pattern analysis)
    @Query("SELECT us FROM UserSession us WHERE us.email = :email AND us.loginTime >= :since ORDER BY us.loginTime DESC")
    List<UserSession> findSessionsByEmailSince(@Param("email") String email, @Param("since") LocalDateTime since);

    // Find all sessions for a user
    List<UserSession> findByEmailOrderByLoginTimeDesc(String email);

    // Find session by ID
    UserSession findBySessionId(UUID sessionId);

    // Deactivate old inactive sessions (cleanup)
    @Query("UPDATE UserSession us SET us.isActive = false WHERE us.isActive = true AND us.loginTime < :cutoffTime")
    void deactivateOldSessions(@Param("cutoffTime") LocalDateTime cutoffTime);
}
