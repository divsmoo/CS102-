package com.cs102.repository;

import com.cs102.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, String> {  // Changed to String for userId

    @Query(value = "SELECT * FROM public.profiles WHERE user_id = :userId LIMIT 1", nativeQuery = true)
    Optional<User> findByUserId(@Param("userId") String userId);

    @Query(value = "SELECT * FROM public.profiles WHERE database_id = :databaseId LIMIT 1", nativeQuery = true)
    Optional<User> findByDatabaseId(@Param("databaseId") UUID databaseId);

    @Query(value = "SELECT * FROM public.profiles WHERE email = :email LIMIT 1", nativeQuery = true)
    Optional<User> findByEmail(@Param("email") String email);

    @Query(value = "SELECT * FROM public.profiles WHERE role = :role", nativeQuery = true)
    List<User> findByRole(@Param("role") String role);

    @Query(value = "SELECT EXISTS(SELECT 1 FROM public.profiles WHERE email = :email)", nativeQuery = true)
    boolean existsByEmail(@Param("email") String email);

    @Query(value = "SELECT EXISTS(SELECT 1 FROM public.profiles WHERE user_id = :userId)", nativeQuery = true)
    boolean existsByUserId(@Param("userId") String userId);
}
