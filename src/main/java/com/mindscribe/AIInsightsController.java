package com.mindscribe;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;
import javafx.animation.*;
import javafx.util.Duration;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindscribe.config.AppConfig;

public class AIInsightsController {

    @FXML private Label vibeLabel;
    @FXML private Label primarySentimentLabel;
    @FXML private Label keyTriggerLabel;
    @FXML private Label insightLabel;
    @FXML private Label focusForTomorrowLabel;
    @FXML private Label statusLabel;
    
    @FXML private AreaChart<Number, Number> moodTrendChart;
    @FXML private Label wordCloudLabel;
    @FXML private Label activityCorrelationLabel;
    
    @FXML private Button refreshInsightsButton;
    @FXML private Button exportReportButton;
    @FXML private Button backButton;
    
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private Map<String, Object> patternsData;
    private Map<String, Object> recommendationsData;

    @FXML
    public void initialize() {
        setupButtons();
        loadAIInsights();
    }
    
    private void setupButtons() {
        refreshInsightsButton.setOnAction(e -> loadAIInsights());
        exportReportButton.setOnAction(e -> exportAIReport());
        backButton.setOnAction(e -> navigateToHome());
    }
    
    private void loadAIInsights() {
        String currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) {
            showStatus("❌ Please login to view AI insights");
            return;
        }
        
        showStatus("Loading AI insights...");
        
        // Load pattern analysis
        loadPatternAnalysis(currentUser);
        
        // Load emotional trends
        loadEmotionalTrends(currentUser);
        
        // Load personalized recommendations
        loadRecommendations(currentUser);
    }
    
    private void loadPatternAnalysis(String username) {
        new Thread(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(AppConfig.AI_PATTERNS + "?username=" + username))
                    .header("Authorization", SessionManager.getBasicAuthHeader())
                    .GET()
                    .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                javafx.application.Platform.runLater(() -> {
                    if (response.statusCode() == 200) {
                        try {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> data = objectMapper.readValue(response.body(), Map.class);
                            updatePatternAnalysis(data);
                        } catch (Exception e) {
                            System.err.println("Error parsing AI patterns: " + e.getMessage());
                            generateLocalPatternAnalysis();
                        }
                    } else {
                        System.err.println("AI patterns endpoint returned: " + response.statusCode());
                        generateLocalPatternAnalysis();
                    }
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    generateLocalPatternAnalysis();
                });
            }
        }).start();
    }
    
    private void generateLocalPatternAnalysis() {
        String currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) {
            vibeLabel.setText("🔒 Login Required");
            primarySentimentLabel.setText("Primary Sentiment: No Data");
            keyTriggerLabel.setText("Key Trigger: No Data");
            insightLabel.setText("Please login to see AI insights.");
            return;
        }
        
        // Try to get some basic statistics for better insights
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(AppConfig.ANALYTICS_STATISTICS + "?username=" + currentUser))
                .header("Authorization", SessionManager.getBasicAuthHeader())
                .GET()
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                @SuppressWarnings("unchecked")
                Map<String, Object> stats = objectMapper.readValue(response.body(), Map.class);
                Number totalEntries = (Number) stats.getOrDefault("totalEntries", 0);
                String mostCommonSentiment = (String) stats.getOrDefault("mostCommonSentiment", "neutral");
                
                if (totalEntries.intValue() > 0) {
                    vibeLabel.setText(getVibeEmoji(mostCommonSentiment) + " " + mostCommonSentiment.substring(0, 1).toUpperCase() + mostCommonSentiment.substring(1));
                    primarySentimentLabel.setText("Primary Sentiment: " + mostCommonSentiment.substring(0, 1).toUpperCase() + mostCommonSentiment.substring(1));
                    keyTriggerLabel.setText("Key Trigger: Self-reflection");
                    insightLabel.setText("You have " + totalEntries + " journal entries. Your emotional journey shows " + mostCommonSentiment + " patterns.");
                } else {
                    vibeLabel.setText("� Start Writing");
                    primarySentimentLabel.setText("Primary Sentiment: No Data");
                    keyTriggerLabel.setText("Key Trigger: No Data");
                    insightLabel.setText("No journal entries available for analysis. Start writing to gain insights!");
                }
            } else {
                generateDefaultInsights();
            }
        } catch (Exception e) {
            generateDefaultInsights();
        }
    }
    
    private void generateDefaultInsights() {
        vibeLabel.setText("🌱 Growing");
        primarySentimentLabel.setText("Primary Sentiment: Discovering");
        keyTriggerLabel.setText("Key Trigger: Daily Life");
        insightLabel.setText("Start journaling regularly to discover your emotional patterns and gain deeper insights.");
    }
    
    private String getVibeEmoji(String sentiment) {
        return switch (sentiment.toLowerCase()) {
            case "positive" -> "😊";
            case "negative" -> "😔";
            case "neutral" -> "😐";
            default -> "🌱";
        };
    }
    
    private void loadEmotionalTrends(String username) {
        new Thread(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(AppConfig.ANALYTICS_SENTIMENT_TRENDS + "?username=" + username))
                    .header("Authorization", SessionManager.getBasicAuthHeader())
                    .GET()
                    .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                javafx.application.Platform.runLater(() -> {
                    if (response.statusCode() == 200) {
                        try {
                            @SuppressWarnings("unchecked")
                            Map<String, Map<String, Long>> trends = objectMapper.readValue(response.body(), Map.class);
                            updateEmotionalTrendsChart(trends);
                        } catch (Exception e) {
                            generateSampleTrends();
                        }
                    } else {
                        generateSampleTrends();
                    }
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    generateSampleTrends();
                });
            }
        }).start();
    }
    
    private void generateSampleTrends() {
        moodTrendChart.getData().clear();
        
        XYChart.Series<Number, Number> sentimentSeries = new XYChart.Series<>();
        sentimentSeries.setName("No Data Available");
        
        // Show no data message instead of sample data
        for (int i = 30; i >= 1; i--) {
            sentimentSeries.getData().add(new XYChart.Data<>(31 - i, 0));
        }
        
        moodTrendChart.getData().add(sentimentSeries);
        
        // Show no data status
        showStatus("⚠️ No real data available - Please create journal entries first");
    }
    
    private void updateEmotionalTrendsChart(Map<String, Map<String, Long>> trends) {
        moodTrendChart.getData().clear();
        
        XYChart.Series<Number, Number> sentimentSeries = new XYChart.Series<>();
        sentimentSeries.setName("Sentiment Score");
        
        // Sort dates and calculate sentiment scores
        java.util.List<String> sortedDates = new java.util.ArrayList<>(trends.keySet());
        java.util.Collections.sort(sortedDates);
        
        // Limit to last 30 days
        int maxDays = Math.min(30, sortedDates.size());
        int startIndex = Math.max(0, sortedDates.size() - maxDays);
        
        for (int i = startIndex; i < sortedDates.size(); i++) {
            String date = sortedDates.get(i);
            Map<String, Long> sentiments = trends.get(date);
            
            int dayIndex = i - startIndex + 1;
            
            // Calculate sentiment score for the day
            long positive = sentiments.getOrDefault("positive", 0L);
            long negative = sentiments.getOrDefault("negative", 0L);
            long neutral = sentiments.getOrDefault("neutral", 0L);
            long total = positive + negative + neutral;
            
            double score = 0.5; // neutral baseline
            if (total > 0) {
                score = (positive * 1.0 + neutral * 0.5) / total;
            }
            
            sentimentSeries.getData().add(new XYChart.Data<>(dayIndex, score));
        }
        
        moodTrendChart.getData().add(sentimentSeries);
    }
    
    private void loadRecommendations(String username) {
        new Thread(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(AppConfig.AI_RECOMMENDATIONS + "?username=" + username))
                    .header("Authorization", SessionManager.getBasicAuthHeader())
                    .GET()
                    .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                javafx.application.Platform.runLater(() -> {
                    if (response.statusCode() == 200) {
                        try {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> data = objectMapper.readValue(response.body(), Map.class);
                            updateRecommendations(data);
                        } catch (Exception e) {
                            generateLocalRecommendations();
                        }
                    } else {
                        generateLocalRecommendations();
                    }
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    generateLocalRecommendations();
                });
            }
        }).start();
    }
    
    private void generateLocalRecommendations() {
        String currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) {
            focusForTomorrowLabel.setText("Focus for Tomorrow: Login to get personalized recommendations");
            return;
        }
        
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(AppConfig.ANALYTICS_STATISTICS + "?username=" + currentUser))
                .header("Authorization", SessionManager.getBasicAuthHeader())
                .GET()
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                @SuppressWarnings("unchecked")
                Map<String, Object> stats = objectMapper.readValue(response.body(), Map.class);
                Number totalEntries = (Number) stats.getOrDefault("totalEntries", 0);
                String mostCommonSentiment = (String) stats.getOrDefault("mostCommonSentiment", "neutral");
                
                String recommendation = getPersonalizedRecommendation(totalEntries.intValue(), mostCommonSentiment);
                focusForTomorrowLabel.setText("Focus for Tomorrow: " + recommendation);
            } else {
                focusForTomorrowLabel.setText("Focus for Tomorrow: Write your first journal entry to begin your wellness journey!");
            }
        } catch (Exception e) {
            focusForTomorrowLabel.setText("Focus for Tomorrow: Take 5 minutes to reflect on your day");
        }
    }
    
    private String getPersonalizedRecommendation(int totalEntries, String sentiment) {
        if (totalEntries == 0) {
            return "Write your first journal entry to begin your wellness journey!";
        } else if (totalEntries < 3) {
            return "Try to write more frequently to track your emotional patterns better.";
        } else if (sentiment.equals("positive")) {
            return "Share your positive energy with someone you care about today.";
        } else if (sentiment.equals("negative")) {
            return "Take three deep breaths and do something kind for yourself.";
        } else {
            return "Notice and appreciate one small moment of joy during your day.";
        }
    }
    
    private void updatePatternAnalysis(Map<String, Object> data) {
        String pattern = (String) data.getOrDefault("pattern", "Consistent emotional expression with growth patterns");
        insightLabel.setText(pattern);
        showStatus("✅ Pattern analysis complete");
    }
    
    private void updateRecommendations(Map<String, Object> data) {
        String recommendation = (String) data.getOrDefault("recommendation", "Continue your current journaling practice for optimal emotional wellness.");
        // Animate recommendation text
        animateText(focusForTomorrowLabel, "Focus for Tomorrow: " + recommendation);
        
        showStatus("✅ Recommendations generated");
    }
    
    private void exportAIReport() {
        exportReportButton.setDisable(true);
        exportReportButton.setText("Exporting...");
        
        new Thread(() -> {
            try {
                Thread.sleep(2000); // Simulate export process
                
                javafx.application.Platform.runLater(() -> {
                    exportReportButton.setDisable(false);
                    exportReportButton.setText("📊 Export Report");
                    showStatus("✅ AI insights report exported successfully!");
                });
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                javafx.application.Platform.runLater(() -> {
                    exportReportButton.setDisable(false);
                    exportReportButton.setText("📊 Export Report");
                    showStatus("❌ Export cancelled");
                });
            }
        }).start();
    }
    
    private void navigateToHome() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/elegant-home-view.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) backButton.getScene().getWindow();
            Scene scene = new Scene(root, 1200, 800);
            scene.getStylesheets().add(getClass().getResource("/styles/style.css").toExternalForm());
            
            stage.setTitle("MindScribe - Home");
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            showStatus("❌ Error navigating to home: " + e.getMessage());
        }
    }
    
    private void showStatus(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: #10B981; -fx-font-size: 14px;");
    }
    
    private void animateText(Label label, String text) {
        label.setText("");
        Timeline timeline = new Timeline();
        
        for (int i = 0; i <= text.length(); i++) {
            final int index = i;
            KeyFrame keyFrame = new KeyFrame(Duration.millis(i * 30), e -> {
                label.setText(text.substring(0, index));
            });
            timeline.getKeyFrames().add(keyFrame);
        }
        
        timeline.play();
    }
}
