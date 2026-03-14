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
import com.fasterxml.jackson.databind.ObjectMapper;

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
                
                String requestBody = String.format(
                    "{\"title\":\"%s\",\"content\":\"%s\"}", 
                    title.replace("\"", "\\\""),
                    content.replace("\"", "\\\"")
                );
                
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/diary/entry?username=" + currentUser))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                javafx.application.Platform.runLater(() -> {
                    saveButton.setDisable(false);
                    saveButton.setText("💾 Save Entry");
                    
                    if (response.statusCode() == 200) {
                        showStatus("✅ Entry saved successfully!");
                        journalEditor.clear();
                    } else {
                        showStatus("❌ Failed to save entry. Please try again.", true);
                    }
                });
                
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    saveButton.setDisable(false);
                    saveButton.setText("💾 Save Entry");
                    showStatus("❌ Error saving entry: " + e.getMessage(), true);
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
        
        analyzeButton.setDisable(true);
        analyzeButton.setText("Analyzing...");
        
        new Thread(() -> {
            try {
                String requestBody = String.format(
                    "{\"content\":\"%s\"}", 
                    content.replace("\"", "\\\"")
                );
                
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/ai/analyze"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                javafx.application.Platform.runLater(() -> {
                    analyzeButton.setDisable(false);
                    analyzeButton.setText("🤖 AI Analyze");
                    
                    if (response.statusCode() == 200) {
                        try {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> result = objectMapper.readValue(response.body(), Map.class);
                            String sentiment = (String) result.getOrDefault("sentiment", "neutral");
                            String insight = (String) result.getOrDefault("insight", "Analysis complete");
                            
                            sentimentLabel.setText("Sentiment: " + sentiment);
                            insightLabel.setText(insight);
                            analysisResult.setVisible(true);
                            showStatus("🎉 AI Analysis completed!");
                        } catch (Exception e) {
                            showStatus("❌ Error parsing AI response: " + e.getMessage(), true);
                        }
                    } else {
                        showStatus("❌ AI analysis failed. Please try again.", true);
                    }
                });
                
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    analyzeButton.setDisable(false);
                    analyzeButton.setText("🤖 AI Analyze");
                    showStatus("❌ Error analyzing: " + e.getMessage(), true);
                });
            }
        }).start();
    }
    
    private void navigateToHome() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/home-view.fxml"));
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
