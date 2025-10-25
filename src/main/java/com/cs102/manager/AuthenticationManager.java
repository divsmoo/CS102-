package com.cs102.manager;

import com.cs102.model.User;
import com.cs102.model.UserRole;
import com.cs102.service.SupabaseAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class AuthenticationManager {

    @Autowired
    private DatabaseManager databaseManager;

    @Autowired
    private SupabaseAuthService supabaseAuthService;

    /**
     * Register a new user with Supabase Auth and create profile
     * @return Optional<User> containing the registered user if successful, empty otherwise
     */
    public Optional<User> register(String name, String email, String password, UserRole role) {
        try {
            // First, try to sign up with Supabase Auth
            UUID userId = supabaseAuthService.signUp(email, password, name);

            if (userId == null) {
                System.err.println("Failed to create user in Supabase Auth");
                return Optional.empty();
            }

            // Check if profile already exists for this UUID
            // This handles cases where auth user was created but profile creation failed
            Optional<User> existingUser = databaseManager.findUserById(userId);
            if (existingUser.isPresent()) {
                System.out.println("Profile already exists for user: " + userId);
                return existingUser; // Return existing profile for auto-login
            }

            // Check if email already exists in profiles (safety check)
            if (databaseManager.userExistsByEmail(email)) {
                System.err.println("Email already exists in profiles: " + email);
                // Try to find and return the user by email
                return databaseManager.findUserByEmail(email);
            }

            // Create profile entry in profiles table
            System.out.println("Creating user with: userId=" + userId + ", email=" + email + ", name=" + name + ", role=" + role);
            User newUser = new User(userId, email, name, role);
            System.out.println("User object created: id=" + newUser.getId() + ", email=" + newUser.getEmail() + ", name=" + newUser.getName() + ", role=" + newUser.getRole());
            databaseManager.saveUser(newUser);

            // Return the user object for auto-login
            return Optional.of(newUser);
        } catch (Exception e) {
            System.err.println("Error during registration: " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }

    /**
     * Login user with email and password using Supabase Auth
     */
    public Optional<User> login(String email, String password) {
        // Call Supabase Auth API for authentication
        UUID userId = supabaseAuthService.signIn(email, password);

        if (userId == null) {
            System.err.println("Authentication failed with Supabase");
            return Optional.empty();
        }

        // Retrieve user profile from database
        Optional<User> userOpt = databaseManager.findUserById(userId);

        if (userOpt.isEmpty()) {
            System.err.println("User profile not found for UUID: " + userId);
        }

        return userOpt;
    }

    /**
     * Logout user (invalidate session)
     */
    public void logout(User user) {
        // TODO: Invalidate Supabase Auth session
        // Clear any local session data
    }
}

