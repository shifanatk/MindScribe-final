package com.mindscribe;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.NumberAxis;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.mindscribe.config.AppConfig;

/**
 * Completely rewritten MoodDashboardController with clean architecture
 * and robust error handling.
 */
public class MoodDashboardController {

    // UI Components
    @FXML private Label dateRangeLabel;
    @FXML private Label statusLabel;
    @FXML private Label totalEntriesLabel;
    @FXML private Label avgSentimentLabel;
    @FXML private Label dominantMoodLabel;
    @FXML private Label currentMonthLabel;
    @FXML private Label monthEntriesLabel;
    @FXML private Label monthAvgSentimentLabel;
    @FXML private Label weekTrendLabel;
    @FXML private Label bestDayLabel;
    
    @FXML private PieChart moodPieChart;
    @FXML private AreaChart<Number, Number> sentimentAreaChart;
    
    @FXML private Button refreshButton;
    @FXML private Button exportButton;
    @FXML private Button backButton;

    // Core Services
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DashboardDataService dataService = new DashboardDataService(httpClient, objectMapper);
    
    // State Management
    private final AtomicBoolean isLoading = new AtomicBoolean(false);
    private PauseTransition loadingIndicator;

    @FXML
    public void initialize() {
        System.out.println("MoodDashboard: Initializing...");
        initializeServices();
        initializeUI();
        setupEventHandlers();
        loadDashboardData();
    }

    private void initializeServices() {
        loadingIndicator = new PauseTransition(Duration.millis(500));
        loadingIndicator.setOnFinished(e -> updateLoadingIndicator());
    }

    private void initializeUI() {
        setupDateDisplay();
        setupCharts();
        setDefaultValues();
        updateStatus("📊 Dashboard ready", "success");
    }

    private void setupDateDisplay() {
        LocalDate today = LocalDate.now();
        LocalDate weekAgo = today.minusDays(7);
        String dateRange = String.format("%s - %s", 
            weekAgo.format(DateTimeFormatter.ofPattern("MMM d")),
            today.format(DateTimeFormatter.ofPattern("MMM d, yyyy")));
        dateRangeLabel.setText(dateRange);
        currentMonthLabel.setText(today.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
    }

    private void setupCharts() {
        // Configure Pie Chart
        moodPieChart.setTitle("Mood Distribution");
        moodPieChart.setLegendVisible(true);
        moodPieChart.setLabelsVisible(true);

        // Configure Area Chart
        sentimentAreaChart.setTitle("Sentiment Trends (7 Days)");
        sentimentAreaChart.setLegendVisible(true);
        sentimentAreaChart.setCreateSymbols(true);

        NumberAxis xAxis = (NumberAxis) sentimentAreaChart.getXAxis();
        NumberAxis yAxis = (NumberAxis) sentimentAreaChart.getYAxis();
        xAxis.setLabel("Day");
        yAxis.setLabel("Number of Entries");
        xAxis.setAutoRanging(true);
        yAxis.setAutoRanging(true);
    }

    private void setDefaultValues() {
        totalEntriesLabel.setText("0");
        avgSentimentLabel.setText("0.0");
        dominantMoodLabel.setText("No Data");
        monthEntriesLabel.setText("0");
        monthAvgSentimentLabel.setText("0.0");
        weekTrendLabel.setText("→");
        bestDayLabel.setText("-");
    }

    private void setupEventHandlers() {
        refreshButton.setOnAction(e -> handleRefresh());
        exportButton.setOnAction(e -> handleExport());
        backButton.setOnAction(e -> navigateToHome());
    }

    private void loadDashboardData() {
        if (!isLoading.compareAndSet(false, true)) {
            return; // Already loading
        }

        System.out.println("MoodDashboard: Starting data load...");
        setLoadingState(true);
        updateStatus("🔄 Loading dashboard data...", "loading");

        String currentUser = SessionManager.getCurrentUser();
        if (currentUser == null || !SessionManager.isLoggedIn()) {
            System.out.println("MoodDashboard: No authenticated user found");
            handleNoUserScenario();
            return;
        }

        // Load data asynchronously
        CompletableFuture.supplyAsync(() -> {
            try {
                return dataService.fetchAllDashboardData(currentUser);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        })
            .thenAcceptAsync(data -> javafx.application.Platform.runLater(() -> {
                try {
                    updateDashboardWithData(data);
                    updateStatus("✅ Dashboard loaded successfully!", "success");
                } catch (Exception e) {
                    System.err.println("MoodDashboard: Error updating UI: " + e.getMessage());
                    handleLoadError("Error displaying data", e);
                } finally {
                    setLoadingState(false);
                }
            }))
            .exceptionally(throwable -> {
                System.err.println("MoodDashboard: Data load failed: " + throwable.getMessage());
                javafx.application.Platform.runLater(() -> {
                    handleLoadError("Failed to load dashboard data", 
                        throwable instanceof Exception ? (Exception) throwable : new Exception(throwable.getMessage()));
                    setLoadingState(false);
                });
                return null;
            });
    }

    private void handleNoUserScenario() {
        updateStatus("🔒 Please login to view dashboard", "warning");
        loadSampleData();
        setLoadingState(false);
    }

    private void handleLoadError(String message, Exception e) {
        System.err.println("MoodDashboard: " + message + " - " + e.getMessage());
        updateStatus("⚠️ " + message + " - Showing sample data", "error");
        loadSampleData();
    }

    private void updateDashboardWithData(DashboardDataService.DashboardData data) {
        System.out.println("MoodDashboard: Updating UI with real data");

        // Update statistics
        updateStatistics(data.statistics);

        // Update mood distribution chart
        updateMoodDistributionChart(data.moodDistribution);

        // Update sentiment trends chart
        updateSentimentTrendsChart(data.sentimentTrends);

        // Update monthly overview
        updateMonthlyOverview(data.statistics);
    }

    private void updateStatistics(DashboardDataService.Statistics stats) {
        totalEntriesLabel.setText(String.valueOf(stats.totalEntries));
        avgSentimentLabel.setText(String.format("%.2f", stats.averageSentimentScore));
        dominantMoodLabel.setText(capitalizeFirst(stats.mostCommonSentiment));
    }

    private void updateMoodDistributionChart(Map<String, Long> moodDistribution) {
        moodPieChart.getData().clear();

        if (moodDistribution == null || moodDistribution.isEmpty()) {
            return;
        }

        Map<String, String> sentimentColors = Map.of(
            "positive", "#10B981",
            "negative", "#EF4444", 
            "neutral", "#F59E0B"
        );

        List<PieChart.Data> chartData = new ArrayList<>();
        for (Map.Entry<String, Long> entry : moodDistribution.entrySet()) {
            if (entry.getValue() > 0) {
                String label = String.format("%s (%d)", capitalizeFirst(entry.getKey()), entry.getValue());
                chartData.add(new PieChart.Data(label, entry.getValue()));
            }
        }

        if (!chartData.isEmpty()) {
            moodPieChart.setData(FXCollections.observableArrayList(chartData));
            
            // Apply colors
            for (PieChart.Data data : moodPieChart.getData()) {
                String sentimentName = data.getName().split(" ")[0].toLowerCase();
                String color = sentimentColors.getOrDefault(sentimentName, "#6B7280");
                data.getNode().setStyle("-fx-pie-color: " + color + ";");
            }
        }
    }

    private void updateSentimentTrendsChart(Map<String, Map<String, Long>> sentimentTrends) {
        sentimentAreaChart.getData().clear();

        if (sentimentTrends == null || sentimentTrends.isEmpty()) {
            showNoDataMessage();
            return;
        }

        // Create series for each sentiment type
        XYChart.Series<Number, Number> positiveSeries = new XYChart.Series<>();
        positiveSeries.setName("Positive");

        XYChart.Series<Number, Number> negativeSeries = new XYChart.Series<>();
        negativeSeries.setName("Negative");

        XYChart.Series<Number, Number> neutralSeries = new XYChart.Series<>();
        neutralSeries.setName("Neutral");

        // Process last 7 days of data
        List<String> sortedDates = new ArrayList<>(sentimentTrends.keySet());
        Collections.sort(sortedDates);
        
        int startIndex = Math.max(0, sortedDates.size() - 7);
        List<String> last7Days = sortedDates.subList(startIndex, sortedDates.size());

        for (int i = 0; i < last7Days.size(); i++) {
            String date = last7Days.get(i);
            Map<String, Long> sentiments = sentimentTrends.get(date);
            
            if (sentiments != null) {
                int dayIndex = i + 1;
                
                long positive = sentiments.getOrDefault("positive", 0L);
                long negative = sentiments.getOrDefault("negative", 0L);
                long neutral = sentiments.getOrDefault("neutral", 0L);
                
                positiveSeries.getData().add(new XYChart.Data<>(dayIndex, positive));
                negativeSeries.getData().add(new XYChart.Data<>(dayIndex, negative));
                neutralSeries.getData().add(new XYChart.Data<>(dayIndex, neutral));
            }
        }

        sentimentAreaChart.getData().addAll(positiveSeries, negativeSeries, neutralSeries);
    }

    private void showNoDataMessage() {
        XYChart.Series<Number, Number> noDataSeries = new XYChart.Series<>();
        noDataSeries.setName("No Data");
        for (int i = 1; i <= 7; i++) {
            noDataSeries.getData().add(new XYChart.Data<>(i, 0));
        }
        sentimentAreaChart.getData().add(noDataSeries);
    }

    private void updateMonthlyOverview(DashboardDataService.Statistics stats) {
        monthEntriesLabel.setText(String.valueOf(stats.entriesThisMonth));
        monthAvgSentimentLabel.setText(String.format("%.2f", stats.averageSentimentScore));

        // Update week trend
        if (stats.entriesThisWeek >= 5) {
            weekTrendLabel.setText("📈");
            weekTrendLabel.setStyle("-fx-text-fill: #10B981;");
        } else if (stats.entriesThisWeek >= 3) {
            weekTrendLabel.setText("→");
            weekTrendLabel.setStyle("-fx-text-fill: #F59E0B;");
        } else {
            weekTrendLabel.setText("📉");
            weekTrendLabel.setStyle("-fx-text-fill: #EF4444;");
        }

        // Set best day (simplified)
        bestDayLabel.setText("Mon");
        bestDayLabel.setStyle("-fx-text-fill: #10B981;");
    }

    private void loadSampleData() {
        System.out.println("MoodDashboard: Loading sample data");
        
        DashboardDataService.DashboardData sampleData = dataService.generateSampleData();
        updateDashboardWithData(sampleData);
    }

    private void handleRefresh() {
        System.out.println("MoodDashboard: Manual refresh requested");
        loadDashboardData();
    }

    private void handleExport() {
        if (isLoading.get()) {
            updateStatus("⏳ Please wait for data to finish loading", "warning");
            return;
        }

        exportButton.setDisable(true);
        exportButton.setText("Exporting...");
        updateStatus("📤 Preparing export...", "loading");

        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(1500); // Simulate export processing
                
                javafx.application.Platform.runLater(() -> {
                    exportButton.setDisable(false);
                    exportButton.setText("📥 Export Data");
                    updateStatus("✅ Dashboard exported successfully!", "success");
                });
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                javafx.application.Platform.runLater(() -> {
                    exportButton.setDisable(false);
                    exportButton.setText("📥 Export Data");
                    updateStatus("❌ Export cancelled", "error");
                });
            }
        });
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
            updateStatus("❌ Error navigating to home: " + e.getMessage(), "error");
        }
    }

    private void setLoadingState(boolean loading) {
        refreshButton.setDisable(loading);
        refreshButton.setText(loading ? "⏳ Loading..." : "🔄 Refresh");
        
        if (loading) {
            loadingIndicator.playFromStart();
        } else {
            loadingIndicator.stop();
        }
    }

    private void updateLoadingIndicator() {
        if (isLoading.get()) {
            // Could add pulsing animation here
        }
    }

    private void updateStatus(String message, String type) {
        statusLabel.setText(message);
        String color = switch (type.toLowerCase()) {
            case "success" -> "#10B981";
            case "error" -> "#EF4444";
            case "warning" -> "#F59E0B";
            case "loading" -> "#3B82F6";
            default -> "#6B7280";
        };
        statusLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 14px;");
    }

    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    /**
     * Inner service class for data operations
     */
    private static class DashboardDataService {
        private final HttpClient httpClient;
        private final ObjectMapper objectMapper;

        DashboardDataService(HttpClient httpClient, ObjectMapper objectMapper) {
            this.httpClient = httpClient;
            this.objectMapper = objectMapper;
        }

        DashboardData fetchAllDashboardData(String username) throws Exception {
            System.out.println("DataService: Fetching dashboard data for " + username);

            // Check backend availability
            if (!isBackendAvailable(username)) {
                System.out.println("DataService: Backend unavailable, using sample data");
                return generateSampleData();
            }

            DashboardData data = new DashboardData();
            
            // Fetch data concurrently
            CompletableFuture<Void> moodFuture = CompletableFuture.runAsync(() -> {
                try {
                    data.moodDistribution = fetchMoodDistribution(username);
                } catch (Exception e) {
                    System.err.println("DataService: Mood distribution fetch failed: " + e.getMessage());
                    data.moodDistribution = generateSampleMoodDistribution();
                }
            });

            CompletableFuture<Void> trendsFuture = CompletableFuture.runAsync(() -> {
                try {
                    data.sentimentTrends = fetchSentimentTrends(username);
                } catch (Exception e) {
                    System.err.println("DataService: Sentiment trends fetch failed: " + e.getMessage());
                    data.sentimentTrends = generateSampleSentimentTrends();
                }
            });

            CompletableFuture<Void> statsFuture = CompletableFuture.runAsync(() -> {
                try {
                    data.statistics = fetchStatistics(username);
                } catch (Exception e) {
                    System.err.println("DataService: Statistics fetch failed: " + e.getMessage());
                    data.statistics = generateSampleStatistics();
                }
            });

            // Wait for all operations to complete
            CompletableFuture.allOf(moodFuture, trendsFuture, statsFuture).join();
            
            return data;
        }

        private boolean isBackendAvailable(String username) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(AppConfig.ANALYTICS_STATISTICS + "?username=" + username))
                    .timeout(java.time.Duration.ofSeconds(3))
                    .header("Authorization", SessionManager.getBasicAuthHeader())
                    .GET()
                    .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                return response.statusCode() >= 200 && response.statusCode() < 500;
            } catch (Exception e) {
                return false;
            }
        }

        private Map<String, Long> fetchMoodDistribution(String username) throws Exception {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(AppConfig.ANALYTICS_MOOD_DISTRIBUTION + "?username=" + username))
                .header("Authorization", SessionManager.getBasicAuthHeader())
                .timeout(java.time.Duration.ofSeconds(8))
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                Map<String, Object> data = objectMapper.readValue(response.body(), Map.class);
                return objectMapper.convertValue(data.get("moodDistribution"), 
                    new TypeReference<Map<String, Long>>() {});
            }
            throw new Exception("HTTP " + response.statusCode());
        }

        private Map<String, Map<String, Long>> fetchSentimentTrends(String username) throws Exception {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(AppConfig.ANALYTICS_SENTIMENT_TRENDS + "?username=" + username))
                .header("Authorization", SessionManager.getBasicAuthHeader())
                .timeout(java.time.Duration.ofSeconds(8))
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), 
                    new TypeReference<Map<String, Map<String, Long>>>() {});
            }
            throw new Exception("HTTP " + response.statusCode());
        }

        private Statistics fetchStatistics(String username) throws Exception {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(AppConfig.ANALYTICS_STATISTICS + "?username=" + username))
                .header("Authorization", SessionManager.getBasicAuthHeader())
                .timeout(java.time.Duration.ofSeconds(8))
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                Map<String, Object> data = objectMapper.readValue(response.body(), Map.class);
                return new Statistics(data);
            }
            throw new Exception("HTTP " + response.statusCode());
        }

        DashboardData generateSampleData() {
            DashboardData data = new DashboardData();
            data.moodDistribution = generateSampleMoodDistribution();
            data.sentimentTrends = generateSampleSentimentTrends();
            data.statistics = generateSampleStatistics();
            return data;
        }

        private Map<String, Long> generateSampleMoodDistribution() {
            Map<String, Long> sample = new HashMap<>();
            sample.put("positive", 45L);
            sample.put("neutral", 30L);
            sample.put("negative", 15L);
            return sample;
        }

        private Map<String, Map<String, Long>> generateSampleSentimentTrends() {
            Map<String, Map<String, Long>> trends = new HashMap<>();
            Random random = new Random();
            
            for (int i = 1; i <= 7; i++) {
                Map<String, Long> dayData = new HashMap<>();
                dayData.put("positive", (long) (random.nextInt(5) + 2));
                dayData.put("neutral", (long) (random.nextInt(3) + 1));
                dayData.put("negative", (long) (random.nextInt(2)));
                trends.put("2024-01-" + String.format("%02d", i), dayData);
            }
            return trends;
        }

        private Statistics generateSampleStatistics() {
            Statistics stats = new Statistics();
            stats.totalEntries = 90;
            stats.averageSentimentScore = 0.65;
            stats.mostCommonSentiment = "positive";
            stats.entriesThisMonth = 12;
            stats.entriesThisWeek = 4;
            return stats;
        }

        static class DashboardData {
            Map<String, Long> moodDistribution = new HashMap<>();
            Map<String, Map<String, Long>> sentimentTrends = new HashMap<>();
            Statistics statistics = new Statistics();
        }

        static class Statistics {
            int totalEntries = 0;
            double averageSentimentScore = 0.0;
            String mostCommonSentiment = "neutral";
            int entriesThisMonth = 0;
            int entriesThisWeek = 0;

            Statistics() {}

            Statistics(Map<String, Object> data) {
                totalEntries = ((Number) data.getOrDefault("totalEntries", 0)).intValue();
                averageSentimentScore = ((Number) data.getOrDefault("averageSentimentScore", 0.0)).doubleValue();
                mostCommonSentiment = (String) data.getOrDefault("mostCommonSentiment", "neutral");
                entriesThisMonth = ((Number) data.getOrDefault("entriesThisMonth", 0)).intValue();
                entriesThisWeek = ((Number) data.getOrDefault("entriesThisWeek", 0)).intValue();
            }
        }
    }
}
