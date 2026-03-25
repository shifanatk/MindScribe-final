package com.mindscribe;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PureMainController {

    @FXML private BorderPane mainPane;
    @FXML private TextArea journalEditor;
    @FXML private Label dateLabel;
    @FXML private Label sentimentLabel;
    @FXML private Label sentimentScore;
    @FXML private Label summaryLabel;
    @FXML private HBox sentimentContainer;
    @FXML private VBox journalEditorContainer;
    
    @FXML private Button writeJournalButton;
    @FXML private Button quickWriteButton;
    @FXML private Button viewEntriesButton;
    @FXML private Button analyticsDashboardButton;
    @FXML private Button saveQuickButton;
    @FXML private Button cancelQuickButton;
    @FXML private Button analyzeButton;
    @FXML private Button saveButton;
    @FXML private VBox recentEntriesContainer;
    @FXML private Label insightLabel;
    @FXML private Button refreshInsightsButton;
    
    // Sidebar buttons
    @FXML private Button homeButton;
    @FXML private Button journalButton;
    @FXML private Button analyticsButton;
    @FXML private Button aiButton;
    @FXML private Button settingsButton;
    @FXML private Button logoutButton;

    public void initialize() {
        // Set current date
        setCurrentDate();
        
        // Setup event handlers
        setupEventHandlers();
        
        // Load initial data
        loadInitialData();
    }
    
    private void setCurrentDate() {
        LocalDate today = LocalDate.now();
        String formattedDate = today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"));
        dateLabel.setText(formattedDate);
    }
    
    private void setupEventHandlers() {
        // New home page buttons
        writeJournalButton.setOnAction(e -> handleNavigation("journal"));
        quickWriteButton.setOnAction(e -> showQuickEditor());
        viewEntriesButton.setOnAction(e -> handleNavigation("entries"));
        analyticsDashboardButton.setOnAction(e -> handleNavigation("analytics"));
        saveQuickButton.setOnAction(e -> saveQuickEntry());
        cancelQuickButton.setOnAction(e -> hideQuickEditor());
        
        // Legacy buttons (for compatibility)
        saveButton.setOnAction(e -> saveJournalEntry());
        analyzeButton.setOnAction(e -> analyzeSentiment());
        
        // Sidebar navigation
        homeButton.setOnAction(e -> handleNavigation("home"));
        journalButton.setOnAction(e -> handleNavigation("entries")); // Changed to entries
        analyticsButton.setOnAction(e -> handleNavigation("analytics"));
        aiButton.setOnAction(e -> handleNavigation("ai"));
        settingsButton.setOnAction(e -> handleNavigation("settings"));
        logoutButton.setOnAction(e -> handleLogout());
        
        // Refresh insights
        refreshInsightsButton.setOnAction(e -> loadAIInsights());
    }
    
    private void loadInitialData() {
        loadRecentEntries();
        loadAIInsights();
    }
    
    private void saveJournalEntry() {
        String content = journalEditor.getText().trim();
        if (content.isEmpty()) {
            showAlert("Please write something before saving.");
            return;
        }
        
        saveButton.setDisable(true);
        saveButton.setText("Saving...");
        
        new Thread(() -> {
            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/diary/entries"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"content\":\"" + content.replace("\"", "\\\"") + "\"}"
                    ))
                    .build();
                
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                
                javafx.application.Platform.runLater(() -> {
                    saveButton.setDisable(false);
                    saveButton.setText("Save Entry");
                    
                    if (response.statusCode() == 201) {
                        showAlert("Entry saved successfully!");
                        journalEditor.clear();
                        loadRecentEntries();
                    } else {
                        showAlert("Failed to save entry. Please try again.");
                    }
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    saveButton.setDisable(false);
                    saveButton.setText("Save Entry");
                    showAlert("Error saving entry: " + e.getMessage());
                });
            }
        }).start();
    }
    
    private void analyzeSentiment() {
        String content = journalEditor.getText().trim();
        if (content.isEmpty()) {
            showAlert("Please write something before analyzing.");
            return;
        }
        
        analyzeButton.setDisable(true);
        analyzeButton.setText("Analyzing...");
        
        new Thread(() -> {
            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/ai/analyze"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"text\":\"" + content.replace("\"", "\\\"") + "\"}"
                    ))
                    .build();
                
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                
                javafx.application.Platform.runLater(() -> {
                    analyzeButton.setDisable(false);
                    analyzeButton.setText("AI Analyze");
                    
                    if (response.statusCode() == 200) {
                        sentimentContainer.setVisible(true);
                        // Parse real sentiment from API response
                        try {
                            ObjectMapper mapper = new ObjectMapper();
                            @SuppressWarnings("unchecked")
                            Map<String, Object> result = mapper.readValue(response.body(), Map.class);
                            
                            String sentiment = (String) result.get("sentiment");
                            Double score = (Double) result.get("score");
                            
                            sentimentLabel.setText("Sentiment: " + (sentiment != null ? sentiment : "Unknown"));
                            sentimentScore.setText("Score: " + (score != null ? String.format("%.2f", score) : "N/A"));
                        } catch (Exception parseError) {
                            showAlert("Error parsing sentiment response: " + parseError.getMessage());
                        }
                    } else {
                        showAlert("Failed to analyze sentiment. Please try again.");
                    }
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    analyzeButton.setDisable(false);
                    analyzeButton.setText("AI Analyze");
                    showAlert("Error analyzing sentiment: " + e.getMessage());
                });
            }
        }).start();
    }
    
    private void loadRecentEntries() {
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
                        Label placeholder = new Label("Recent entries will be loaded here");
                        placeholder.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 14px;");
                        recentEntriesContainer.getChildren().setAll(placeholder);
                    } else {
                        Label error = new Label("Failed to load entries");
                        error.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 14px;");
                        recentEntriesContainer.getChildren().setAll(error);
                    }
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    Label error = new Label("Connection error");
                    error.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 14px;");
                    recentEntriesContainer.getChildren().setAll(error);
                });
            }
        }).start();
    }
    
    private void loadAIInsights() {
        insightLabel.setText("AI insights will be loaded here based on your journal patterns.");
    }
    
    private void showQuickEditor() {
        journalEditorContainer.setVisible(true);
        journalEditor.requestFocus();
    }
    
    private void hideQuickEditor() {
        journalEditorContainer.setVisible(false);
        journalEditor.clear();
    }
    
    private void saveQuickEntry() {
        String content = journalEditor.getText().trim();
        if (content.isEmpty()) {
            showAlert("Please write something before saving.");
            return;
        }
        
        saveQuickButton.setDisable(true);
        saveQuickButton.setText("Saving...");
        
        new Thread(() -> {
            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/diary/entries"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"content\":\"" + content.replace("\"", "\\\"") + "\"}"
                    ))
                    .build();
                
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                
                javafx.application.Platform.runLater(() -> {
                    saveQuickButton.setDisable(false);
                    saveQuickButton.setText("💾 Save");
                    
                    if (response.statusCode() == 201) {
                        showAlert("Quick entry saved successfully!");
                        hideQuickEditor();
                        loadRecentEntries();
                    } else {
                        showAlert("Failed to save entry. Please try again.");
                    }
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    saveQuickButton.setDisable(false);
                    saveQuickButton.setText("💾 Save");
                    showAlert("Error saving entry: " + e.getMessage());
                });
            }
        }).start();
    }
    
    private void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login-view.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) mainPane.getScene().getWindow();
            Scene scene = new Scene(root, 400, 500);
            scene.getStylesheets().add(getClass().getResource("/styles/style.css").toExternalForm());
            
            stage.setTitle("MindScribe - Login");
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            showAlert("Error loading login page: " + e.getMessage());
        }
    }
    
    private void navigateToSimpleHome() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/home-view.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) mainPane.getScene().getWindow();
            Scene scene = new Scene(root, 1200, 800);
            scene.getStylesheets().add(getClass().getResource("/styles/style.css").toExternalForm());
            
            stage.setTitle("MindScribe - Home");
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            showAlert("Error loading home page: " + e.getMessage());
        }
    }
    
    private void handleNavigation(String section) {
        try {
            FXMLLoader loader;
            String title;
            
            switch (section) {
                case "home":
                    // Just stay on home page, no navigation needed
                    return;
                case "journal":
                    loader = new FXMLLoader(getClass().getResource("/fxml/write-journal-view.fxml"));
                    title = "MindScribe - Write Journal";
                    break;
                case "analytics":
                    loader = new FXMLLoader(getClass().getResource("/fxml/mood-dashboard-view.fxml"));
                    title = "MindScribe - Mood Dashboard";
                    break;
                case "entries":
                    loader = new FXMLLoader(getClass().getResource("/fxml/previous-entries-view.fxml"));
                    title = "MindScribe - Previous Entries";
                    break;
                case "ai":
                    loader = new FXMLLoader(getClass().getResource("/fxml/ai-insights-view.fxml"));
                    title = "MindScribe - AI Insights";
                    break;
                case "settings":
                    loader = new FXMLLoader(getClass().getResource("/fxml/settings-view.fxml"));
                    title = "MindScribe - Settings";
                    break;
                default:
                    return;
            }
            
            // Load and navigate to the selected page
            Parent root = loader.load();
            Stage stage = (Stage) mainPane.getScene().getWindow();
            Scene scene = new Scene(root, 1200, 800);
            scene.getStylesheets().add(getClass().getResource("/styles/style.css").toExternalForm());
            
            stage.setTitle(title);
            stage.setScene(scene);
            stage.show();
            
        } catch (Exception e) {
            showAlert("Error loading page: " + e.getMessage());
        }
    }
    
    private void showHomeContent(VBox container) {
        Label title = new Label("📝 Write Your Journal");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: 700; -fx-text-fill: #E2E8F0;");
        
        Label subtitle = new Label("What's on your mind today?");
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #9CA3AF;");
        
        TextArea editor = new TextArea();
        editor.setPromptText("Share your thoughts, feelings, and experiences...");
        editor.setStyle("-fx-background-color: #2D2A3E60; -fx-background-radius: 12px; -fx-border-color: #7C3AED40; -fx-border-width: 2px; -fx-text-fill: #E2E8F0; -fx-prompt-text-fill: #9CA3AF; -fx-padding: 15px;");
        editor.setPrefHeight(200);
        
        HBox buttonBox = new HBox(10);
        Button saveBtn = new Button("Save Entry");
        saveBtn.setStyle("-fx-background-color: #7C3AED; -fx-text-fill: white; -fx-background-radius: 25px; -fx-padding: 12px 24px;");
        
        Button analyzeBtn = new Button("AI Analyze");
        analyzeBtn.setStyle("-fx-background-color: #FBCFE8; -fx-text-fill: #1F2937; -fx-background-radius: 25px; -fx-padding: 12px 24px;");
        
        buttonBox.getChildren().addAll(saveBtn, analyzeBtn);
        
        container.getChildren().addAll(title, subtitle, editor, buttonBox);
        
        // Wire up the buttons
        saveBtn.setOnAction(e -> {
            journalEditor = editor;
            saveJournalEntry();
        });
        
        analyzeBtn.setOnAction(e -> {
            journalEditor = editor;
            analyzeSentiment();
        });
    }
    
    private void showJournalContent(VBox container) {
        Label title = new Label("📚 Your Journal Entries");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: 700; -fx-text-fill: #E2E8F0;");
        
        Label subtitle = new Label("All your thoughts and experiences in one place");
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #9CA3AF;");
        
        VBox contentBox = new VBox(20, title, subtitle);
        contentBox.setStyle("-fx-background-color: #2D2A3E60; -fx-background-radius: 18px; -fx-padding: 30px;");
        
        Label entriesInfo = new Label("Your journal entries will be loaded here from the backend database.");
        entriesInfo.setStyle("-fx-text-fill: #E2E8F0; -fx-font-size: 14px;");
        
        contentBox.getChildren().add(entriesInfo);
        container.getChildren().add(contentBox);
        
        // Load real entries
        loadJournalEntries(contentBox);
    }
    
    private void showAnalyticsContent(VBox container) {
        Label title = new Label("📊 Mood Analytics");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: 700; -fx-text-fill: #E2E8F0;");
        
        Label subtitle = new Label("Track your emotional patterns and trends");
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #9CA3AF;");
        
        VBox contentBox = new VBox(20, title, subtitle);
        contentBox.setStyle("-fx-background-color: #2D2A3E60; -fx-background-radius: 18px; -fx-padding: 30px;");
        
        Label analyticsInfo = new Label("Your mood charts and sentiment trends will appear here.");
        analyticsInfo.setStyle("-fx-text-fill: #E2E8F0; -fx-font-size: 14px;");
        
        contentBox.getChildren().add(analyticsInfo);
        container.getChildren().add(contentBox);
    }
    
    private void showAIContent(VBox container) {
        Label title = new Label("🤖 AI Insights");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: 700; -fx-text-fill: #E2E8F0;");
        
        Label subtitle = new Label("Get intelligent analysis of your emotional patterns");
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #9CA3AF;");
        
        VBox contentBox = new VBox(20, title, subtitle);
        contentBox.setStyle("-fx-background-color: #2D2A3E60; -fx-background-radius: 18px; -fx-padding: 30px;");
        
        Label aiInfo = new Label("AI-powered insights about your journal patterns will appear here.");
        aiInfo.setStyle("-fx-text-fill: #E2E8F0; -fx-font-size: 14px;");
        
        contentBox.getChildren().add(aiInfo);
        container.getChildren().add(contentBox);
    }
    
    private void showSettingsContent(VBox container) {
        Label title = new Label("⚙️ Settings");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: 700; -fx-text-fill: #E2E8F0;");
        
        Label subtitle = new Label("Customize your MindScribe experience");
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #9CA3AF;");
        
        VBox contentBox = new VBox(20, title, subtitle);
        contentBox.setStyle("-fx-background-color: #2D2A3E60; -fx-background-radius: 18px; -fx-padding: 30px;");
        
        Label settingsInfo = new Label("Application settings and preferences will appear here.");
        settingsInfo.setStyle("-fx-text-fill: #E2E8F0; -fx-font-size: 14px;");
        
        contentBox.getChildren().add(settingsInfo);
        container.getChildren().add(contentBox);
    }
    
    private void loadJournalEntries(VBox container) {
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
                        Label entriesLabel = new Label("Journal entries loaded successfully from backend!");
                        entriesLabel.setStyle("-fx-text-fill: #10B981; -fx-font-size: 14px;");
                        container.getChildren().add(entriesLabel);
                    } else {
                        Label errorLabel = new Label("Backend API not available yet - entries will load when backend is ready");
                        errorLabel.setStyle("-fx-text-fill: #F59E0B; -fx-font-size: 14px;");
                        container.getChildren().add(errorLabel);
                    }
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    Label errorLabel = new Label("Connection error: " + e.getMessage());
                    errorLabel.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 14px;");
                    container.getChildren().add(errorLabel);
                });
            }
        }).start();
    }
    
    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("MindScribe");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
