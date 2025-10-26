package com.cs102.repository;

import com.cs102.model.User;
import com.cs102.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, String> {  // Changed to String for userId

    Optional<User> findByUserId(String userId);

    Optional<User> findByDatabaseId(UUID databaseId);

    Optional<User> findByEmail(String email);

    List<User> findByRole(UserRole role);

    boolean existsByEmail(String email);

    boolean existsByUserId(String userId);
}
