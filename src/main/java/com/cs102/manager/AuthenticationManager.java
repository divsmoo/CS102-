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
     */
    public boolean register(String name, String email, String password, UserRole role) {
        try {
            // Check if email already exists in profiles
            if (databaseManager.userExistsByEmail(email)) {
                System.err.println("Email already exists in profiles: " + email);
                return false;
            }

            // Call Supabase Auth API to create user in auth.users
            UUID userId = supabaseAuthService.signUp(email, password, name);

            if (userId == null) {
                System.err.println("Failed to create user in Supabase Auth");
                return false;
            }

            // Create profile entry in profiles table
            System.out.println("Creating user with: userId=" + userId + ", email=" + email + ", name=" + name + ", role=" + role);
            User newUser = new User(userId, email, name, role);
            System.out.println("User object created: id=" + newUser.getId() + ", email=" + newUser.getEmail() + ", name=" + newUser.getName() + ", role=" + newUser.getRole());
            databaseManager.saveUser(newUser);

            return true;
        } catch (Exception e) {
            System.err.println("Error during registration: " + e.getMessage());
            e.printStackTrace();
            return false;
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

