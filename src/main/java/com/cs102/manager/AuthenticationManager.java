package com.cs102.manager;

import com.cs102.model.User;
import com.cs102.model.UserRole;
import com.cs102.service.IntrusionDetectionService;
import com.cs102.service.SessionAnomalyDetector;
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

    @Autowired
    private IntrusionDetectionService idsService;
    
    @Autowired
    private SessionAnomalyDetector sessionAnomalyDetector;

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
            // Validate input for SQL injection
            if (idsService.detectSQLInjection(userId) || 
                idsService.detectSQLInjection(name) || 
                idsService.detectSQLInjection(email)) {
                throw new RuntimeException("Invalid input detected. Please use valid characters.");
            }

            // Check for suspicious registration patterns
            if (idsService.detectSuspiciousRegistration(email)) {
                throw new RuntimeException("Too many registration attempts. Please try again later.");
            }

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
        // Check if account is locked
        if (idsService.isAccountLocked(email)) {
            long remainingMinutes = idsService.getRemainingLockoutTime(email);
            throw new RuntimeException(
                String.format("Account temporarily locked due to too many failed login attempts. " +
                    "Please try again in %d minutes.", remainingMinutes)
            );
        }

        // Validate input for SQL injection
        if (idsService.detectSQLInjection(email) || idsService.detectSQLInjection(password)) {
            // Don't record as failed login - SQL injection already logged as CRITICAL
            throw new RuntimeException("Invalid input detected. Please use valid credentials.");
        }

        // Call Supabase Auth API for authentication
        UUID databaseId = supabaseAuthService.signIn(email, password);

        if (databaseId == null) {
            System.err.println("Authentication failed with Supabase");
            // Record failed login attempt
            idsService.recordFailedLogin(email);
            return Optional.empty();
        }

        // Retrieve user profile from database using database_id
        Optional<User> userOpt = databaseManager.findUserByDatabaseId(databaseId);

        if (userOpt.isEmpty()) {
            System.err.println("User profile not found for database ID: " + databaseId);
            idsService.recordFailedLogin(email);
            return Optional.empty();
        }

        // Record successful login
        idsService.recordSuccessfulLogin(email);
        
        // Check for session anomalies
        String sessionId = UUID.randomUUID().toString();
        sessionAnomalyDetector.checkLoginAnomaly(email, sessionId);

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
     * Delete all face images for a user
     */
    public void deleteFaceImages(User user) {
        try {
            databaseManager.deleteFaceImagesByUserId(user.getUserId());
            System.out.println("Deleted all face images for student: " + user.getUserId());
        } catch (Exception e) {
            System.err.println("Error deleting face images: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to delete face images: " + e.getMessage(), e);
        }
    }

    /**
     * Update user's email in Supabase Auth
     * @param newEmail The new email address
     * @return true if successful, false otherwise
     */
    public boolean updateUserEmail(String newEmail) {
        String accessToken = supabaseAuthService.getCurrentAccessToken();
        if (accessToken == null) {
            System.err.println("No access token available. User must be logged in to update email.");
            return false;
        }
        return supabaseAuthService.updateEmail(accessToken, newEmail);
    }

    /**
     * Update user's password in Supabase Auth
     * @param newPassword The new password
     * @return true if successful, false otherwise
     */
    public boolean updateUserPassword(String newPassword) {
        String accessToken = supabaseAuthService.getCurrentAccessToken();
        System.out.println("Attempting to update password...");
        System.out.println("Access token available: " + (accessToken != null && !accessToken.isEmpty()));
        if (accessToken == null || accessToken.isEmpty()) {
            System.err.println("✗ No access token available. User must be logged in to update password.");
            return false;
        }
        System.out.println("Calling Supabase Auth service to update password...");
        return supabaseAuthService.updatePassword(accessToken, newPassword);
    }

    /**
     * Update user's name in Supabase Auth metadata
     * @param newName The new name
     * @return true if successful, false otherwise
     */
    public boolean updateUserName(String newName) {
        String accessToken = supabaseAuthService.getCurrentAccessToken();
        if (accessToken == null) {
            System.err.println("No access token available. User must be logged in to update name.");
            return false;
        }
        return supabaseAuthService.updateUserMetadata(accessToken, newName);
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

    /**
     * Get the IntrusionDetectionService instance
     * @return IntrusionDetectionService instance
     */
    public IntrusionDetectionService getIntrusionDetectionService() {
        return idsService;
    }
    
    /**
     * Get the SessionAnomalyDetector instance
     * @return SessionAnomalyDetector instance
     */
    public SessionAnomalyDetector getSessionAnomalyDetector() {
        return sessionAnomalyDetector;
    }
}

