package com.mindscribe;

import java.util.Base64;

public class SessionManager {
    private static String currentUser = null;
    private static String authToken = null;
    private static String basicAuthHeader = null;
    
    public static void login(String username, String password) {
        currentUser = username;
        // Create Basic Auth header for API calls
        String credentials = username + ":" + password;
        basicAuthHeader = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());
        authToken = null; // We'll use Basic Auth instead
    }
    
    public static void logout() {
        currentUser = null;
        authToken = null;
        basicAuthHeader = null;
    }
    
    public static String getCurrentUser() {
        return currentUser;
    }
    
    public static String getAuthToken() {
        return authToken;
    }
    
    public static String getBasicAuthHeader() {
        return basicAuthHeader;
    }
    
    public static boolean isLoggedIn() {
        return currentUser != null && basicAuthHeader != null;
    }
}
