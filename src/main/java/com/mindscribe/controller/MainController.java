package com.mindscribe.controller;

import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.util.Duration;
import org.springframework.stereotype.Controller;

import java.net.URL;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
public class MainController implements Initializable {

    @FXML private BorderPane mainPane;
    @FXML private Label dateLabel;
    @FXML private TextArea journalEditor;
    @FXML private Button saveButton;
    @FXML private Button analyzeButton;
    @FXML private HBox sentimentContainer;
    @FXML private Label sentimentLabel;
    @FXML private Label sentimentScore;
    @FXML private PieChart moodPieChart;
    @FXML private AreaChart<Number, Number> sentimentAreaChart;
    @FXML private VBox recentEntriesContainer;
    @FXML private Label insightLabel;
    @FXML private Button refreshInsightsButton;
    
    // Sidebar buttons
    @FXML private Button homeButton;
    @FXML private Button journalButton;
    @FXML private Button analyticsButton;
    @FXML private Button aiButton;
    @FXML private Button settingsButton;
    @FXML private Button profileButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Apply fade-in entrance animation
        applyEntranceAnimations();
        
        // Set current date
        setCurrentDate();
        
        // Initialize charts
        initializeCharts();
        
        // Setup event handlers
        setupEventHandlers();
        
        // Load initial data
        loadRecentEntries();
        loadAIInsights();
    }

    /**
     * Apply beautiful entrance animations to UI components
     */
    private void applyEntranceAnimations() {
        // Main content fade-in
        FadeTransition mainFade = new FadeTransition(Duration.millis(800), mainPane);
        mainFade.setFromValue(0.0);
        mainFade.setToValue(1.0);
        mainFade.play();
        
        // Sidebar slide-in from left
        TranslateTransition sidebarSlide = new TranslateTransition(Duration.millis(600), homeButton.getParent());
        sidebarSlide.setFromX(-100);
        sidebarSlide.setToX(0);
        sidebarSlide.setDelay(Duration.millis(200));
        sidebarSlide.play();
        
        // Editor slide-in from bottom
        TranslateTransition editorSlide = new TranslateTransition(Duration.millis(600), journalEditor);
        editorSlide.setFromY(50);
        editorSlide.setToY(0);
        editorSlide.setDelay(Duration.millis(400));
        editorSlide.play();
        
        // Analytics slide-in from right
        TranslateTransition analyticsSlide = new TranslateTransition(Duration.millis(600), moodPieChart.getParent());
        analyticsSlide.setFromX(100);
        analyticsSlide.setToX(0);
        analyticsSlide.setDelay(Duration.millis(600));
        analyticsSlide.play();
    }

    /**
     * Set current date in header
     */
    private void setCurrentDate() {
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");
        dateLabel.setText(today.format(formatter));
    }

    /**
     * Initialize charts with no data - will load from API
     */
    private void initializeCharts() {
        // Mood Pie Chart
        initializeMoodChart();
        
        // Sentiment Area Chart
        initializeSentimentChart();
    }

    private void initializeMoodChart() {
        // Show no data message
        PieChart.Data noData = new PieChart.Data("No Data", 1);
        moodPieChart.getData().add(noData);
        
        // Apply custom colors
        applyChartColors();
    }

    private void initializeSentimentChart() {
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("No Data Available");
        
        // Show no data instead of sample data
        for (int i = 1; i <= 7; i++) {
            series.getData().add(new XYChart.Data<>(i, 0));
        }
        
        sentimentAreaChart.getData().add(series);
    }

    private void applyChartColors() {
        // Custom colors for pie chart slices
        String[] colors = {"#86EFAC", "#FDE68A", "#FCA5A5"};
        
        for (int i = 0; i < moodPieChart.getData().size(); i++) {
            PieChart.Data data = moodPieChart.getData().get(i);
            Node node = data.getNode();
            if (node != null) {
                node.setStyle("-fx-pie-color: " + colors[i] + ";");
            }
        }
    }

    /**
     * Setup event handlers for buttons and interactions
     */
    private void setupEventHandlers() {
        // Save button
        saveButton.setOnAction(e -> saveJournalEntry());
        
        // Analyze button
        analyzeButton.setOnAction(e -> analyzeSentiment());
        
        // Sidebar navigation
        homeButton.setOnAction(e -> handleNavigation("home"));
        journalButton.setOnAction(e -> handleNavigation("journal"));
        analyticsButton.setOnAction(e -> handleNavigation("analytics"));
        aiButton.setOnAction(e -> handleNavigation("ai"));
        settingsButton.setOnAction(e -> handleNavigation("settings"));
        profileButton.setOnAction(e -> handleNavigation("profile"));
        
        // Refresh insights
        refreshInsightsButton.setOnAction(e -> loadAIInsights());
        
        // Auto-save on text change (with delay)
        journalEditor.textProperty().addListener((obs, oldText, newText) -> {
            // Debounce auto-save
            // Implementation would go here
        });
    }

    /**
     * Save journal entry
     */
    private void saveJournalEntry() {
        String content = journalEditor.getText();
        if (content.trim().isEmpty()) {
            showAlert("Please write something before saving.", Alert.AlertType.WARNING);
            return;
        }
        
        // Show saving animation
        saveButton.setText("Saving...");
        saveButton.setDisable(true);
        
        // Simulate API call
        new Thread(() -> {
            try {
                Thread.sleep(1000); // Simulate network delay
                
                Platform.runLater(() -> {
                    saveButton.setText("Save Entry");
                    saveButton.setDisable(false);
                    journalEditor.clear();
                    
                    // Show success animation
                    showSuccessAnimation();
                    
                    // Refresh recent entries
                    loadRecentEntries();
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    /**
     * Analyze sentiment of current journal entry
     */
    private void analyzeSentiment() {
        String content = journalEditor.getText();
        if (content.trim().isEmpty()) {
            showAlert("Please write something before analyzing.", Alert.AlertType.WARNING);
            return;
        }
        
        analyzeButton.setText("Analyzing...");
        analyzeButton.setDisable(true);
        
        // Simulate AI analysis
        new Thread(() -> {
            try {
                Thread.sleep(1500); // Simulate AI processing
                
                Platform.runLater(() -> {
                    analyzeButton.setText("AI Analyze");
                    analyzeButton.setDisable(false);
                    
                    // Show sentiment result with animation
                    showSentimentResult("positive", 0.87);
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    /**
     * Display sentiment analysis result with animation
     */
    private void showSentimentResult(String sentiment, double score) {
        sentimentLabel.setText(sentiment.toUpperCase());
        sentimentScore.setText(String.format("Confidence: %.1f%%", score * 100));
        
        // Apply sentiment-specific styling
        sentimentLabel.getStyleClass().removeAll("sentiment-positive", "sentiment-negative", "sentiment-neutral");
        sentimentLabel.getStyleClass().add("sentiment-" + sentiment);
        
        // Show container with slide-up animation
        sentimentContainer.setVisible(true);
        
        TranslateTransition slideUp = new TranslateTransition(Duration.millis(500), sentimentContainer);
        slideUp.setFromY(30);
        slideUp.setToY(0);
        slideUp.play();
    }

    /**
     * Load recent journal entries
     */
    private void loadRecentEntries() {
        // Clear existing entries
        recentEntriesContainer.getChildren().clear();
        
        // Show no data message instead of sample entries
        Label noDataLabel = new Label("No journal entries available. Start writing to see your entries here!");
        noDataLabel.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 14px; -fx-font-style: italic;");
        noDataLabel.setWrapText(true);
        
        VBox noDataContainer = new VBox(noDataLabel);
        noDataContainer.setStyle("-fx-background-color: #2D2A3E40; -fx-background-radius: 12px; -fx-padding: 20px;");
        noDataContainer.setAlignment(javafx.geometry.Pos.CENTER);
        
        recentEntriesContainer.getChildren().add(noDataContainer);
        
        // Load real entries from backend
        loadRealEntries();
    }

    /**
     * Create entry card UI component
     */
    private VBox createEntryCard(String preview, LocalDate date) {
        VBox card = new VBox(10);
        card.getStyleClass().add("entry-card");
        
        Label dateLabel = new Label(date.format(DateTimeFormatter.ofPattern("MMM d")));
        dateLabel.getStyleClass().add("entry-date");
        
        Label previewLabel = new Label(preview);
        previewLabel.getStyleClass().add("entry-preview");
        previewLabel.setWrapText(true);
        
        card.getChildren().addAll(dateLabel, previewLabel);
        return card;
    }

    /**
     * Load AI insights
     */
    private void loadAIInsights() {
        refreshInsightsButton.setText("Loading...");
        refreshInsightsButton.setDisable(true);
        
        new Thread(() -> {
            try {
                Thread.sleep(1000);
                
                Platform.runLater(() -> {
                    insightLabel.setText("Based on your recent entries, you seem to be in a positive emotional state. " +
                            "Your writing shows increased optimism and gratitude. Keep up the great work!");
                    
                    refreshInsightsButton.setText("Refresh Insights");
                    refreshInsightsButton.setDisable(false);
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    /**
     * Handle sidebar navigation
     */
    private void handleNavigation(String section) {
        // Reset all sidebar buttons
        homeButton.getStyleClass().remove("selected");
        journalButton.getStyleClass().remove("selected");
        analyticsButton.getStyleClass().remove("selected");
        aiButton.getStyleClass().remove("selected");
        settingsButton.getStyleClass().remove("selected");
        profileButton.getStyleClass().remove("selected");
        
        // Highlight selected button
        switch (section) {
            case "home" -> homeButton.getStyleClass().add("selected");
            case "journal" -> journalButton.getStyleClass().add("selected");
            case "analytics" -> analyticsButton.getStyleClass().add("selected");
            case "ai" -> aiButton.getStyleClass().add("selected");
            case "settings" -> settingsButton.getStyleClass().add("selected");
            case "profile" -> profileButton.getStyleClass().add("selected");
        }
        
        // Here you would switch views/sections
        System.out.println("Navigating to: " + section);
    }

    /**
     * Show success animation
     */
    private void showSuccessAnimation() {
        // Create a temporary success label
        Label successLabel = new Label("✓ Entry Saved");
        successLabel.getStyleClass().add("sentiment-positive");
        
        // Position it and animate
        // Implementation would go here
    }
    
    /**
     * Load real entries from backend API
     */
    private void loadRealEntries() {
        new Thread(() -> {
            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/diary/entries"))
                    .GET()
                    .build();
                
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                
                javafx.application.Platform.runLater(() -> {
                    if (response.statusCode() == 200) {
                        // Parse and display real entries
                        try {
                            ObjectMapper mapper = new ObjectMapper();
                            List<Map<String, Object>> entries = mapper.readValue(response.body(), 
                                mapper.getTypeFactory().constructCollectionType(List.class, Map.class));
                            
                            if (!entries.isEmpty()) {
                                recentEntriesContainer.getChildren().clear();
                                
                                // Display first 3 entries
                                int maxEntries = Math.min(3, entries.size());
                                for (int i = 0; i < maxEntries; i++) {
                                    Map<String, Object> entry = entries.get(i);
                                    String content = (String) entry.get("content");
                                    if (content != null && content.length() > 100) {
                                        content = content.substring(0, 100) + "...";
                                    }
                                    
                                    LocalDate date = LocalDate.now();
                                    if (entry.containsKey("createdAt")) {
                                        date = LocalDate.parse(entry.get("createdAt").toString().substring(0, 10));
                                    }
                                    
                                    VBox entryCard = createEntryCard(content, date);
                                    recentEntriesContainer.getChildren().add(entryCard);
                                    
                                    // Staggered animation
                                    FadeTransition fade = new FadeTransition(Duration.millis(500), entryCard);
                                    fade.setFromValue(0.0);
                                    fade.setToValue(1.0);
                                    fade.setDelay(Duration.millis(i * 100));
                                    fade.play();
                                }
                            }
                        } catch (Exception e) {
                            // Keep showing no data message
                        }
                    }
                });
            } catch (Exception e) {
                // Keep showing no data message
            }
        }).start();
    }

    /**
     * Show alert dialog
     */
    private void showAlert(String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle("MindScribe");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
