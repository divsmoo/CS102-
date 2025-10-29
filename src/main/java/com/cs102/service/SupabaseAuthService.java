package com.cs102.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

@Service
public class SupabaseAuthService {

    @Value("${supabase.url:}")
    private String supabaseUrl;

    @Value("${supabase.api.key:}")
    private String supabaseApiKey;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    
    // Store access token after login
    private String currentAccessToken;

    /**
     * Register a new user with Supabase Auth
     * @return The user's UUID from Supabase Auth, or null if failed
     */
    public UUID signUp(String email, String password, String name) {
        try {
            String authUrl = supabaseUrl + "/auth/v1/signup";
            System.out.println(authUrl);
            System.out.println(supabaseApiKey);

            String jsonBody = String.format(
                "{\"email\":\"%s\",\"password\":\"%s\",\"data\":{\"name\":\"%s\"}}",
                email, password, name
            );

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(authUrl))
                .header("Content-Type", "application/json")
                .header("apikey", supabaseApiKey)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 || response.statusCode() == 201) {
                // Parse UUID and token from response
                String responseBody = response.body();
                storeAccessToken(responseBody);
                return parseUserIdFromResponse(responseBody);
            } else if (response.statusCode() == 400 && response.body().contains("User already registered")) {
                // User already exists in Supabase Auth, try to sign in to get their UUID
                System.out.println("User already exists in Supabase Auth, attempting to retrieve UUID via sign-in");
                return signIn(email, password);
            } else {
                System.err.println("Supabase signup failed: " + response.statusCode());
                System.err.println("Response: " + response.body());
                return null;
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Error calling Supabase Auth: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Sign in with email and password
     * @return The user's UUID from Supabase Auth, or null if failed
     */
    public UUID signIn(String email, String password) {
        try {
            String authUrl = supabaseUrl + "/auth/v1/token?grant_type=password";

            String jsonBody = String.format(
                "{\"email\":\"%s\",\"password\":\"%s\"}",
                email, password
            );

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(authUrl))
                .header("Content-Type", "application/json")
                .header("apikey", supabaseApiKey)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String responseBody = response.body();
                storeAccessToken(responseBody);
                return parseUserIdFromResponse(responseBody);
            } else {
                System.err.println("Supabase login failed: " + response.statusCode());
                return null;
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Error calling Supabase Auth: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Update user password in Supabase Auth
     * Requires user to be signed in (have access token)
     * @param email User's email (for re-authentication)
     * @param newPassword New password to set
     * @return true if successful, false otherwise
     */
    public boolean updatePassword(String email, String newPassword) {
        try {
            // If no access token, we can't update password
            if (currentAccessToken == null || currentAccessToken.isEmpty()) {
                System.err.println("No access token available. User must be logged in to update password.");
                return false;
            }

            String url = supabaseUrl + "/auth/v1/user";
            
            // Create JSON body with new password
            String jsonBody = String.format("{\"password\":\"%s\"}", newPassword);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("apikey", supabaseApiKey)
                    .header("Authorization", "Bearer " + currentAccessToken)
                    .method("PUT", HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                System.out.println("Password updated successfully");
                return true;
            } else {
                System.err.println("Failed to update password. Status: " + response.statusCode());
                System.err.println("Response: " + response.body());
                return false;
            }
        } catch (Exception e) {
            System.err.println("Error updating password: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private UUID parseUserIdFromResponse(String jsonResponse) {
        try {
            // Simple JSON parsing to extract user id
            // Format: {"user":{"id":"uuid-here",...},...}
            int idIndex = jsonResponse.indexOf("\"id\":\"");
            if (idIndex != -1) {
                int startIndex = idIndex + 6;
                int endIndex = jsonResponse.indexOf("\"", startIndex);
                String uuidStr = jsonResponse.substring(startIndex, endIndex);
                return UUID.fromString(uuidStr);
            }
        } catch (Exception e) {
            System.err.println("Error parsing user ID from response: " + e.getMessage());
        }
        return null;
    }

    private void storeAccessToken(String jsonResponse) {
        try {
            // Parse access_token from response
            // Format: {"access_token":"token-here",...}
            int tokenIndex = jsonResponse.indexOf("\"access_token\":\"");
            if (tokenIndex != -1) {
                int startIndex = tokenIndex + 16;
                int endIndex = jsonResponse.indexOf("\"", startIndex);
                currentAccessToken = jsonResponse.substring(startIndex, endIndex);
                System.out.println("Access token stored successfully");
            }
        } catch (Exception e) {
            System.err.println("Error parsing access token from response: " + e.getMessage());
        }
    }
}