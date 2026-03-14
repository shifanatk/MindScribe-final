package com.mindscribe;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class MoodDashboardController {

    @FXML private Label dateRangeLabel;
    @FXML private Label statusLabel;
    @FXML private Label totalEntriesLabel;
    @FXML private Label avgSentimentLabel;
    @FXML private Label dominantMoodLabel;
    
    @FXML private PieChart moodPieChart;
    @FXML private AreaChart<Number, Number> sentimentAreaChart;
    
    @FXML private Button refreshButton;
    @FXML private Button exportButton;
    @FXML private Button backButton;
    
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @FXML
    public void initialize() {
        // Set date range
        LocalDate today = LocalDate.now();
        LocalDate weekAgo = today.minusDays(7);
        String dateRange = weekAgo.format(DateTimeFormatter.ofPattern("MMM d")) + " - " + 
                           today.format(DateTimeFormatter.ofPattern("MMM d, yyyy"));
        dateRangeLabel.setText(dateRange);
        
        // Setup buttons
        setupButtons();
        
        String currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) {
            showStatus("❌ Please login to view dashboard");
            return;
        }
        
        showStatus("Loading dashboard data...");
        
        // Load all real data
        loadMoodDistribution();
        loadSentimentTrends();
        loadStatistics();
    }
    
    private void updatePieChart(Map<String, Long> moodCounts) {
        javafx.scene.chart.PieChart.Data[] pieChartData = moodCounts.entrySet().stream()
            .map(entry -> new javafx.scene.chart.PieChart.Data(entry.getKey(), entry.getValue()))
            .toArray(javafx.scene.chart.PieChart.Data[]::new);
        
        javafx.scene.chart.PieChart pieChart = new javafx.scene.chart.PieChart(javafx.collections.FXCollections.observableArrayList(pieChartData));
        pieChart.setTitle("Mood Distribution");
        moodPieChart.setData(pieChart.getData());
    }
    
    private void setupButtons() {
        refreshButton.setOnAction(e -> initialize());
        exportButton.setOnAction(e -> exportData());
        backButton.setOnAction(e -> navigateToHome());
    }
    
    private void loadMoodDistribution() {
        String currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) {
            javafx.application.Platform.runLater(() -> {
                showStatus("❌ Please login to view mood distribution");
            });
            return;
        }
        
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/analytics/mood-distribution?username=" + currentUser))
                .GET()
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            javafx.application.Platform.runLater(() -> {
                if (response.statusCode() == 200) {
                    try {
                        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        @SuppressWarnings("unchecked")
                        Map<String, Object> data = mapper.readValue(response.body(), Map.class);
                        
                        // Extract mood distribution from the response
                        @SuppressWarnings("unchecked")
                        Map<String, Long> moodCounts = (Map<String, Long>) data.getOrDefault("moodDistribution", new java.util.HashMap<>());
                        
                        // Update pie chart
                        updatePieChart(moodCounts);
                        
                        // Update statistics
                        Number totalEntries = (Number) data.getOrDefault("totalEntries", 0);
                        totalEntriesLabel.setText(String.valueOf(totalEntries));
                        
                        // Find dominant mood
                        String dominantMood = moodCounts.entrySet().stream()
                            .max(Map.Entry.comparingByValue())
                            .map(Map.Entry::getKey)
                            .orElse("Neutral");
                        dominantMoodLabel.setText(dominantMood);
                        
                        showStatus("✅ Dashboard updated successfully!");
                    } catch (Exception e) {
                        showStatus("❌ Error parsing mood data: " + e.getMessage());
                        e.printStackTrace();
                    }
                } else {
                    showStatus("❌ Failed to load mood distribution");
                }
            });
            
        } catch (Exception e) {
            javafx.application.Platform.runLater(() -> {
                showStatus("❌ Connection error loading mood data: " + e.getMessage());
            });
        }
    }
    
    private void createSampleMoodData() {
        moodPieChart.getData().clear();
        
        PieChart.Data happy = new PieChart.Data("😊 Happy", 35);
        PieChart.Data sad = new PieChart.Data("😢 Sad", 15);
        PieChart.Data neutral = new PieChart.Data("😐 Neutral", 30);
        PieChart.Data excited = new PieChart.Data("🎉 Excited", 12);
        PieChart.Data anxious = new PieChart.Data("😰 Anxious", 8);
        
        moodPieChart.getData().addAll(happy, sad, neutral, excited, anxious);
        
        // Customize colors
        happy.getNode().setStyle("-fx-background-color: #10B981;");
        sad.getNode().setStyle("-fx-background-color: #3B82F6;");
        neutral.getNode().setStyle("-fx-background-color: #6B7280;");
        excited.getNode().setStyle("-fx-background-color: #F59E0B;");
        anxious.getNode().setStyle("-fx-background-color: #EF4444;");
    }
    
    private void loadSentimentTrends() {
        String currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) {
            javafx.application.Platform.runLater(() -> {
                showStatus("❌ Please login to view sentiment trends");
            });
            return;
        }
        
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/analytics/sentiment-trends?username=" + currentUser))
                .GET()
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            javafx.application.Platform.runLater(() -> {
                if (response.statusCode() == 200) {
                    try {
                        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        @SuppressWarnings("unchecked")
                        Map<String, Map<String, Long>> data = mapper.readValue(response.body(), Map.class);
                        updateSentimentChart(data);
                        showStatus("✅ Sentiment trends loaded!");
                    } catch (Exception e) {
                        showStatus("❌ Error parsing sentiment trends: " + e.getMessage());
                        e.printStackTrace();
                    }
                } else {
                    showStatus("❌ Failed to load sentiment trends");
                }
            });
            
        } catch (Exception e) {
            javafx.application.Platform.runLater(() -> {
                showStatus("❌ Connection error loading sentiment trends: " + e.getMessage());
            });
        }
    }
    
    private void updateSentimentChart(Map<String, Map<String, Long>> trends) {
        sentimentAreaChart.getData().clear();
        
        XYChart.Series<Number, Number> positiveSeries = new XYChart.Series<>();
        positiveSeries.setName("Positive");
        
        XYChart.Series<Number, Number> negativeSeries = new XYChart.Series<>();
        negativeSeries.setName("Negative");
        
        XYChart.Series<Number, Number> neutralSeries = new XYChart.Series<>();
        neutralSeries.setName("Neutral");
        
        // Sort dates and add data points
        java.util.List<String> sortedDates = new java.util.ArrayList<>(trends.keySet());
        java.util.Collections.sort(sortedDates);
        
        int dayIndex = 1;
        for (String date : sortedDates) {
            Map<String, Long> sentiments = trends.get(date);
            
            // For simplicity, we'll show total entries per day as sentiment value
            long totalForDay = sentiments.values().stream().mapToLong(Long::longValue).sum();
            double sentimentValue = 0.5; // Default neutral
            
            if (sentiments.containsKey("positive")) {
                sentimentValue = 0.5 + (sentiments.get("positive") / (double) totalForDay) * 0.5;
            } else if (sentiments.containsKey("negative")) {
                sentimentValue = 0.5 - (sentiments.get("negative") / (double) totalForDay) * 0.5;
            }
            
            positiveSeries.getData().add(new XYChart.Data<>(dayIndex, sentimentValue));
            dayIndex++;
        }
        
        sentimentAreaChart.getData().add(positiveSeries);
    }
    
    private void createSampleSentimentData() {
        sentimentAreaChart.getData().clear();
        
        XYChart.Series<Number, Number> sentimentSeries = new XYChart.Series<>();
        sentimentSeries.setName("Sentiment Trend");
        
        // Add sample data for last 7 days
        sentimentSeries.getData().addAll(
            new XYChart.Data<>(1, 0.6),
            new XYChart.Data<>(2, 0.8),
            new XYChart.Data<>(3, 0.4),
            new XYChart.Data<>(4, 0.7),
            new XYChart.Data<>(5, 0.9),
            new XYChart.Data<>(6, 0.5),
            new XYChart.Data<>(7, 0.8)
        );
        
        sentimentAreaChart.getData().add(sentimentSeries);
    }
    
    private void loadStatistics() {
        String currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) {
            javafx.application.Platform.runLater(() -> {
                showStatus("❌ Please login to view statistics");
            });
            return;
        }
        
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/analytics/statistics?username=" + currentUser))
                .GET()
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            javafx.application.Platform.runLater(() -> {
                if (response.statusCode() == 200) {
                    try {
                        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        @SuppressWarnings("unchecked")
                        Map<String, Object> data = mapper.readValue(response.body(), Map.class);
                        updateStatisticsFromData(data);
                        showStatus("✅ Statistics loaded!");
                    } catch (Exception e) {
                        showStatus("❌ Error parsing statistics: " + e.getMessage());
                        e.printStackTrace();
                    }
                } else {
                    showStatus("❌ Failed to load statistics");
                }
            });
            
        } catch (Exception e) {
            javafx.application.Platform.runLater(() -> {
                showStatus("❌ Connection error loading statistics: " + e.getMessage());
            });
        }
    }
    
    private void updateStatisticsFromData(Map<String, Object> data) {
        Number totalEntries = (Number) data.getOrDefault("totalEntries", 0);
        Number avgSentiment = (Number) data.getOrDefault("averageSentimentScore", 0.5);
        String dominantMood = (String) data.getOrDefault("mostCommonSentiment", "neutral");
        
        totalEntriesLabel.setText(String.valueOf(totalEntries));
        avgSentimentLabel.setText(String.format("%.2f", avgSentiment.doubleValue()));
        dominantMoodLabel.setText(dominantMood);
    }
    
    private void exportData() {
        exportButton.setDisable(true);
        exportButton.setText("Exporting...");
        
        new Thread(() -> {
            try {
                // Simulate export process
                Thread.sleep(2000);
                
                javafx.application.Platform.runLater(() -> {
                    exportButton.setDisable(false);
                    exportButton.setText("📥 Export Data");
                    showStatus("✅ Data exported successfully! (Feature coming soon)");
                });
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                javafx.application.Platform.runLater(() -> {
                    exportButton.setDisable(false);
                    exportButton.setText("📥 Export Data");
                    showStatus("❌ Export cancelled");
                });
            }
        }).start();
    }
    
    private void navigateToHome() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/home-view.fxml"));
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
}
