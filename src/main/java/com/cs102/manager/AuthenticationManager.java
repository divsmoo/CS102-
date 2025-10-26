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
     * @param userId Student ID (e.g., S12345) - primary key for profiles
     * @param name Student name
     * @param email Student email
     * @param password Password for authentication
     * @param role User role (STUDENT/PROFESSOR)
     * @return Optional<User> containing the registered user if successful, empty otherwise
     * @throws RuntimeException with detailed error message if registration fails
     */
    public Optional<User> register(String userId, String name, String email, String password, UserRole role) {
        try {
            // Validate student ID format
            if (userId == null || userId.trim().isEmpty()) {
                throw new RuntimeException("Student ID is required");
            }

            // Check if student ID already exists
            if (databaseManager.userExistsByUserId(userId)) {
                throw new RuntimeException("Student ID already exists: " + userId);
            }

            // First, try to sign up with Supabase Auth
            UUID databaseId = supabaseAuthService.signUp(email, password, name);

            if (databaseId == null) {
                String errorMsg = "Failed to create user in Supabase Auth. The email may already be registered or the credentials are invalid.";
                System.err.println(errorMsg);
                throw new RuntimeException(errorMsg);
            }

            // Check if profile already exists for this database ID
            // This handles cases where auth user was created but profile creation failed
            Optional<User> existingUser = databaseManager.findUserByDatabaseId(databaseId);
            if (existingUser.isPresent()) {
                System.out.println("Profile already exists for database ID: " + databaseId);
                return existingUser; // Return existing profile for auto-login
            }

            // Check if email already exists in profiles (safety check)
            if (databaseManager.userExistsByEmail(email)) {
                System.err.println("Email already exists in profiles: " + email);
                // Try to find and return the user by email
                Optional<User> userByEmail = databaseManager.findUserByEmail(email);
                if (userByEmail.isPresent()) {
                    return userByEmail;
                } else {
                    throw new RuntimeException("Email already exists but profile could not be retrieved from database.");
                }
            }

            // Create profile entry in profiles table
            System.out.println("Creating user with: userId=" + userId + ", databaseId=" + databaseId + ", email=" + email + ", name=" + name + ", role=" + role);
            User newUser = new User(userId, databaseId, email, name, role);
            System.out.println("User object created: userId=" + newUser.getUserId() + ", email=" + newUser.getEmail() + ", name=" + newUser.getName() + ", role=" + newUser.getRole());
            databaseManager.saveUser(newUser);

            // Return the user object for auto-login
            return Optional.of(newUser);
        } catch (RuntimeException e) {
            // Re-throw runtime exceptions with original message
            throw e;
        } catch (Exception e) {
            System.err.println("Error during registration: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Database error: " + e.getMessage(), e);
        }
    }

    /**
     * Login user with email and password using Supabase Auth
     */
    public Optional<User> login(String email, String password) {
        // Call Supabase Auth API for authentication
        UUID databaseId = supabaseAuthService.signIn(email, password);

        if (databaseId == null) {
            System.err.println("Authentication failed with Supabase");
            return Optional.empty();
        }

        // Retrieve user profile from database using database_id
        Optional<User> userOpt = databaseManager.findUserByDatabaseId(databaseId);

        if (userOpt.isEmpty()) {
            System.err.println("User profile not found for database ID: " + databaseId);
        }

        return userOpt;
    }

    /**
     * Update user's face image (legacy - stores only one image)
     */
    public void updateUserFaceImage(User user, byte[] faceImage) {
        try {
            user.setFaceImage(faceImage);
            databaseManager.saveUser(user);
            System.out.println("Updated face image for student: " + user.getUserId());
        } catch (Exception e) {
            System.err.println("Error updating face image: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Save multiple face images for user
     */
    public void saveFaceImages(User user, java.util.List<byte[]> faceImages) {
        try {
            databaseManager.saveFaceImages(user.getUserId(), faceImages);
            System.out.println("Saved " + faceImages.size() + " face images for student: " + user.getUserId());
        } catch (Exception e) {
            System.err.println("Error saving face images: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to save face images: " + e.getMessage(), e);
        }
    }

    /**
     * Logout user (invalidate session)
     */
    public void logout(User user) {
        // TODO: Invalidate Supabase Auth session
        // Clear any local session data
    }

    /**
     * Get the DatabaseManager instance
     * @return DatabaseManager instance
     */
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
}

