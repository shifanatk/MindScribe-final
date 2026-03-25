package com.mindscribe.config;

public class AppConfig {
    public static final String BASE_URL = System.getProperty("mindscribe.api.url", "http://localhost:8080");
    public static final String API_BASE = BASE_URL + "/api";
    
    // API Endpoints
    public static final String AUTH_LOGIN = API_BASE + "/auth/login";
    public static final String AUTH_SIGNUP = API_BASE + "/auth/signup";
    public static final String DIARY_ENTRY = API_BASE + "/diary/entry";
    public static final String DIARY_ENTRIES = API_BASE + "/diary/entries";
    public static final String AI_ANALYZE = API_BASE + "/ai/analyze";
    public static final String AI_PATTERNS = API_BASE + "/ai/patterns";
    public static final String AI_RECOMMENDATIONS = API_BASE + "/ai/recommendations";
    public static final String ANALYTICS_STATISTICS = API_BASE + "/analytics/statistics";
    public static final String ANALYTICS_MOOD_DISTRIBUTION = API_BASE + "/analytics/mood-distribution";
    public static final String ANALYTICS_SENTIMENT_TRENDS = API_BASE + "/analytics/sentiment-trends";
    
    // UI Configuration
    public static final double WINDOW_WIDTH = 1200;
    public static final double WINDOW_HEIGHT = 800;
    public static final double LOGIN_WIDTH = 400;
    public static final double LOGIN_HEIGHT = 500;
    
    private AppConfig() {
        // Utility class - prevent instantiation
    }
}
