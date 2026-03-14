package com.mindscribe;

public class SessionManager {
    private static String currentUser = null;
    private static String authToken = null;
    
    public static void login(String username, String token) {
        currentUser = username;
        authToken = token;
    }
    
    public static void logout() {
        currentUser = null;
        authToken = null;
    }
    
    public static String getCurrentUser() {
        return currentUser;
    }
    
    public static String getAuthToken() {
        return authToken;
    }
    
    public static boolean isLoggedIn() {
        return currentUser != null;
    }
}
