package com.mindscribe;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.HashMap;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindscribe.config.AppConfig;

public class WriteJournalController {

    @FXML private TextArea journalEditor;
    @FXML private Label dateLabel;
    @FXML private Button saveButton;
    @FXML private Button analyzeButton;
    @FXML private Button backButton;
    @FXML private Label statusLabel;
    @FXML private VBox analysisResult;
    @FXML private Label sentimentLabel;
    @FXML private Label insightLabel;
    
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private boolean isAnalysisInProgress = false;

    @FXML
    public void initialize() throws com.fasterxml.jackson.core.JsonProcessingException {
        // Set current date
        LocalDate today = LocalDate.now();
        String formattedDate = today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"));
        dateLabel.setText(formattedDate);
        
        // Setup buttons
        setupButtons();
    }
    
    private void setupButtons() {
        saveButton.setOnAction(e -> saveJournalEntry());
        analyzeButton.setOnAction(e -> analyzeWithAI());
        backButton.setOnAction(e -> navigateToHome());
    }
    
    private void saveJournalEntry() {
        String content = journalEditor.getText().trim();
        if (content.isEmpty()) {
            showStatus("Please write something before saving.", true);
            return;
        }
        
        String currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) {
            showStatus("❌ Please login to save entries", true);
            return;
        }
        
        saveButton.setDisable(true);
        saveButton.setText("Saving...");
        
        new Thread(() -> {
            try {
                String title = "Journal Entry - " + LocalDate.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
                
                // Create proper JSON with escaped content
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                Map<String, String> requestBody = new HashMap<>();
                requestBody.put("title", title);
                requestBody.put("content", content);
                requestBody.put("mood", "neutral"); // Add default mood
                
                String jsonBody = mapper.writeValueAsString(requestBody);
                
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(AppConfig.DIARY_ENTRY + "?username=" + currentUser))
                    .header("Content-Type", "application/json")
                    .header("Authorization", SessionManager.getBasicAuthHeader())
                    .timeout(java.time.Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                javafx.application.Platform.runLater(() -> {
                    saveButton.setDisable(false);
                    saveButton.setText("💾 Save Entry");
                    
                    if (response.statusCode() == 200) {
                        showStatus("✅ Entry saved successfully!");
                        
                        // Store the content for analysis before clearing
                        String contentForAnalysis = journalEditor.getText().trim();
                        
                        // Clear the editor
                        journalEditor.clear();
                        
                        // Automatically trigger AI analysis with the saved content
                        analyzeContentAfterSave(contentForAnalysis);
                    } else {
                        showStatus("❌ Failed to save entry. Server response: " + response.statusCode(), true);
                        System.err.println("Save error response: " + response.body());
                    }
                });
                
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    saveButton.setDisable(false);
                    saveButton.setText("💾 Save Entry");
                    showStatus("❌ Error saving entry: " + e.getMessage(), true);
                    e.printStackTrace();
                });
            }
        }).start();
    }
    
    private void analyzeContentAfterSave(String content) {
        if (content.isEmpty()) {
            showStatus("No content to analyze.", true);
            return;
        }
        
        if (isAnalysisInProgress) {
            showStatus("Analysis already in progress...", true);
            return;
        }
        
        String currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) {
            showStatus("❌ Please login to use AI analysis", true);
            return;
        }
        
        isAnalysisInProgress = true;
        analyzeButton.setDisable(true);
        analyzeButton.setText("Analyzing...");
        
        new Thread(() -> {
            try {
                String requestBody = String.format(
                    "{\"content\":\"%s\"}", 
                    content.replace("\"", "\\\"")
                );
                
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(AppConfig.AI_ANALYZE))
                    .header("Content-Type", "application/json")
                    .header("Authorization", SessionManager.getBasicAuthHeader())
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                javafx.application.Platform.runLater(() -> {
                    isAnalysisInProgress = false;
                    analyzeButton.setDisable(false);
                    analyzeButton.setText("🤖 AI Analyze");
                    
                    if (response.statusCode() == 200) {
                        try {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> result = objectMapper.readValue(response.body(), Map.class);
                            String sentiment = (String) result.getOrDefault("sentiment", "neutral");
                            String insight = (String) result.getOrDefault("insight", "Analysis complete");
                            
                            sentimentLabel.setText("Sentiment: " + capitalize(sentiment));
                            insightLabel.setText(insight);
                            analysisResult.setVisible(true);
                            showStatus("🎉 AI Analysis completed for your saved entry!");
                        } catch (Exception e) {
                            showStatus("❌ Error parsing AI response: " + e.getMessage(), true);
                            generateLocalAnalysis(content);
                        }
                    } else {
                        showStatus("⚠️ AI service unavailable - Using local analysis", true);
                        generateLocalAnalysis(content);
                    }
                });
                
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    isAnalysisInProgress = false;
                    analyzeButton.setDisable(false);
                    analyzeButton.setText("🤖 AI Analyze");
                    showStatus("⚠️ Connection error - Using local analysis", true);
                    generateLocalAnalysis(content);
                });
            }
        }).start();
    }
    
    private void analyzeWithAI() {
        String content = journalEditor.getText().trim();
        if (content.isEmpty()) {
            showStatus("Please write something before analyzing.", true);
            return;
        }
        
        if (isAnalysisInProgress) {
            showStatus("Analysis already in progress...", true);
            return;
        }
        
        String currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) {
            showStatus("❌ Please login to use AI analysis", true);
            return;
        }
        
        isAnalysisInProgress = true;
        analyzeButton.setDisable(true);
        analyzeButton.setText("Analyzing...");
        
        new Thread(() -> {
            try {
                String requestBody = String.format(
                    "{\"content\":\"%s\"}", 
                    content.replace("\"", "\\\"")
                );
                
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(AppConfig.AI_ANALYZE))
                    .header("Content-Type", "application/json")
                    .header("Authorization", SessionManager.getBasicAuthHeader())
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                javafx.application.Platform.runLater(() -> {
                    isAnalysisInProgress = false;
                    analyzeButton.setDisable(false);
                    analyzeButton.setText("🤖 AI Analyze");
                    
                    if (response.statusCode() == 200) {
                        try {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> result = objectMapper.readValue(response.body(), Map.class);
                            String sentiment = (String) result.getOrDefault("sentiment", "neutral");
                            String insight = (String) result.getOrDefault("insight", "Analysis complete");
                            
                            sentimentLabel.setText("Sentiment: " + capitalize(sentiment));
                            insightLabel.setText(insight);
                            analysisResult.setVisible(true);
                            showStatus("🎉 AI Analysis completed!");
                        } catch (Exception e) {
                            showStatus("❌ Error parsing AI response: " + e.getMessage(), true);
                            generateLocalAnalysis(content);
                        }
                    } else {
                        showStatus("⚠️ AI service unavailable - Using local analysis", true);
                        generateLocalAnalysis(content);
                    }
                });
                
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    isAnalysisInProgress = false;
                    analyzeButton.setDisable(false);
                    analyzeButton.setText("🤖 AI Analyze");
                    showStatus("⚠️ Connection error - Using local analysis", true);
                    generateLocalAnalysis(content);
                });
            }
        }).start();
    }
    
    private void generateLocalAnalysis(String content) {
        // Simple sentiment analysis based on keywords
        String[] positiveWords = {"happy", "joy", "great", "good", "wonderful", "amazing", "love", "grateful", "excited"};
        String[] negativeWords = {"sad", "angry", "frustrated", "stressed", "anxious", "worried", "bad", "terrible", "hate"};
        
        String lowerContent = content.toLowerCase();
        int positiveCount = 0;
        int negativeCount = 0;
        
        for (String word : positiveWords) {
            if (lowerContent.contains(word)) positiveCount++;
        }
        for (String word : negativeWords) {
            if (lowerContent.contains(word)) negativeCount++;
        }
        
        String sentiment;
        String insight;
        
        if (positiveCount > negativeCount) {
            sentiment = "positive";
            insight = "This entry shows positive emotions. Keep up the great mood!";
        } else if (negativeCount > positiveCount) {
            sentiment = "negative";
            insight = "This entry shows some challenging emotions. It's okay to have difficult days.";
        } else {
            sentiment = "neutral";
            insight = "This entry appears balanced. A good reflection on your day.";
        }
        
        sentimentLabel.setText("Sentiment: " + capitalize(sentiment));
        insightLabel.setText(insight);
        analysisResult.setVisible(true);
        showStatus("✅ Local analysis completed!");
    }
    
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
    
    private void navigateToHome() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/elegant-home-view.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) saveButton.getScene().getWindow();
            Scene scene = new Scene(root, 1200, 800);
            scene.getStylesheets().add(getClass().getResource("/styles/style.css").toExternalForm());
            
            stage.setTitle("MindScribe - Home");
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            showStatus("❌ Error navigating to home: " + e.getMessage(), true);
        }
    }
    
    private void showStatus(String message) {
        showStatus(message, false);
    }
    
    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        if (isError) {
            statusLabel.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 14px;");
        } else {
            statusLabel.setStyle("-fx-text-fill: #10B981; -fx-font-size: 14px;");
        }
    }
}
