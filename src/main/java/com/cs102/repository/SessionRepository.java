package com.cs102.repository;

import com.cs102.model.Session;
import com.cs102.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {

    Optional<Session> findBySessionId(String sessionId);

    Optional<Session> findByUserAndActiveTrue(User user);

    void deleteBySessionId(String sessionId);
}
