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
                // Parse UUID from response
                String responseBody = response.body();
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
}
