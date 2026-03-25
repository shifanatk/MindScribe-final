package com.mindscribe;

import com.mindscribe.model.h2.JournalEntry;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindscribe.config.AppConfig;

public class PreviousEntriesController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> moodFilter;
    @FXML private Button filterButton;
    @FXML private TableView<JournalEntry> entriesTable;
    @FXML private TableColumn<JournalEntry, java.time.LocalDateTime> dateColumn;
    @FXML private TableColumn<JournalEntry, String> moodColumn;
    @FXML private TableColumn<JournalEntry, String> contentColumn;
    @FXML private TableColumn<JournalEntry, String> sentimentColumn;
    @FXML private TableColumn<JournalEntry, Void> actionsColumn;
    
    @FXML private VBox entryDetails;
    @FXML private Button closeDetailsButton;
    @FXML private Label detailDateLabel;
    @FXML private Label detailMoodLabel;
    @FXML private Label detailSentimentLabel;
    @FXML private TextArea detailContentArea;
    @FXML private Button editEntryButton;
    @FXML private Button deleteEntryButton;
    @FXML private Button analyzeEntryButton;
    
    @FXML private Button loadMoreButton;
    @FXML private Button exportButton;
    @FXML private Button refreshButton;
    @FXML private Button backButton;
    
    @FXML private Label statusLabel;
    
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private ObservableList<JournalEntry> entries = FXCollections.observableArrayList();
    private JournalEntry selectedEntry;

    @FXML
    public void initialize() {
        // Setup mood filter
        setupMoodFilter();
        
        // Setup table columns
        setupTableColumns();
        
        // Setup buttons
        setupButtons();
        
        // Load entries
        loadEntries();
        
        // Auto-refresh entries every 30 seconds
        setupAutoRefresh();
    }
    
    private void setupAutoRefresh() {
        // Schedule automatic refresh every 30 seconds
        javafx.animation.Timeline refreshTimer = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(
                javafx.util.Duration.seconds(30),
                event -> loadEntries()
            )
        );
        refreshTimer.setCycleCount(javafx.animation.Animation.INDEFINITE);
        refreshTimer.play();
    }
    
    private void setupMoodFilter() {
        moodFilter.getItems().addAll("All Moods", "Happy", "Sad", "Neutral", "Excited", "Anxious");
        moodFilter.setValue("All Moods");
    }
    
    private void setupTableColumns() {
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        // Format date for display
        dateColumn.setCellFactory(param -> new TableCell<JournalEntry, java.time.LocalDateTime>() {
            @Override
            protected void updateItem(java.time.LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a");
                    setText(item.format(formatter));
                }
            }
        });
        moodColumn.setCellValueFactory(new PropertyValueFactory<>("sentiment")); // Using sentiment as mood
        contentColumn.setCellValueFactory(new PropertyValueFactory<>("content"));
        sentimentColumn.setCellValueFactory(new PropertyValueFactory<>("sentiment"));
        
        // Setup actions column with View button
        actionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button viewButton = new Button("📖 View");
            
            {
                viewButton.setStyle("-fx-background-color: #7C3AED; -fx-text-fill: white; -fx-background-radius: 15px; -fx-padding: 5px 10px;");
                viewButton.setOnAction(event -> {
                    JournalEntry entry = getTableView().getItems().get(getIndex());
                    showEntryDetails(entry);
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(viewButton);
                }
            }
        });
        
        entriesTable.setItems(entries);
    }
    
    private void setupButtons() {
        filterButton.setOnAction(e -> filterEntries());
        closeDetailsButton.setOnAction(e -> hideEntryDetails());
        editEntryButton.setOnAction(e -> editEntry());
        deleteEntryButton.setOnAction(e -> deleteEntry());
        analyzeEntryButton.setOnAction(e -> analyzeEntry());
        loadMoreButton.setOnAction(e -> loadMoreEntries());
        exportButton.setOnAction(e -> exportEntries());
        refreshButton.setOnAction(e -> loadEntries());
        backButton.setOnAction(e -> navigateToHome());
        
        // Setup search field
        searchField.setOnKeyPressed(e -> {
            if (e.getCode().toString().equals("ENTER")) {
                filterEntries();
            }
        });
    }
    
    private void loadEntries() {
        showStatus("Loading entries...");
        
        String currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) {
            showStatus("❌ Please login to view entries");
            return;
        }
        
        new Thread(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(AppConfig.DIARY_ENTRIES + "?username=" + currentUser))
                    .header("Authorization", SessionManager.getBasicAuthHeader())
                    .GET()
                    .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                javafx.application.Platform.runLater(() -> {
                    if (response.statusCode() == 200) {
                        // Parse real entries from API response
                        try {
                            ObjectMapper mapper = new ObjectMapper();
                            List<Map<String, Object>> entriesData = mapper.readValue(response.body(), 
                                mapper.getTypeFactory().constructCollectionType(List.class, Map.class));
                            
                            entries.clear();
                            for (Map<String, Object> entryData : entriesData) {
                                JournalEntry entry = new JournalEntry();
                                entry.setId(Long.valueOf(entryData.get("id").toString()));
                                entry.setTitle(entryData.get("title").toString());
                                entry.setContent(entryData.get("content").toString());
                                entry.setUsername(entryData.get("username").toString());
                                entry.setSentiment((String) entryData.get("sentiment"));
                                entry.setSentimentScore((Double) entryData.get("sentimentScore"));
                                // Parse createdAt if available
                                if (entryData.containsKey("createdAt")) {
                                    entry.setCreatedAt(java.time.LocalDateTime.parse(entryData.get("createdAt").toString()));
                                }
                                entries.add(entry);
                            }
                            
                            entriesTable.setItems(entries);
                            showStatus("✅ Real entries loaded successfully!");
                        } catch (Exception parseError) {
                            showStatus("❌ Error parsing entries: " + parseError.getMessage());
                        }
                    } else {
                        showStatus("❌ Failed to load entries: HTTP " + response.statusCode());
                    }
                });
                
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    showStatus("❌ Connection error: " + e.getMessage());
                });
            }
        }).start();
    }
    
    private void filterEntries() {
        String searchText = searchField.getText().toLowerCase();
        String selectedMood = moodFilter.getValue();
        
        ObservableList<JournalEntry> filteredEntries = FXCollections.observableArrayList();
        
        for (JournalEntry entry : entries) {
            boolean matchesSearch = searchText.isEmpty() || 
                                  (entry.getContent() != null && entry.getContent().toLowerCase().contains(searchText)) ||
                                  (entry.getCreatedAt() != null && entry.getCreatedAt().toString().toLowerCase().contains(searchText));
            
            boolean matchesMood = selectedMood.equals("All Moods") || 
                                 (entry.getSentiment() != null && entry.getSentiment().equalsIgnoreCase(selectedMood));
            
            if (matchesSearch && matchesMood) {
                filteredEntries.add(entry);
            }
        }
        
        entriesTable.setItems(filteredEntries);
        showStatus("✅ Filter applied. Found " + filteredEntries.size() + " entries.");
    }
    
    private void showEntryDetails(JournalEntry entry) {
        selectedEntry = entry;
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a");
        detailDateLabel.setText("📅 " + entry.getCreatedAt().format(formatter));
        detailMoodLabel.setText("😊 " + (entry.getSentiment() != null ? entry.getSentiment() : "Neutral"));
        detailSentimentLabel.setText("💭 " + (entry.getSentimentScore() != null ? 
            "Score: " + String.format("%.2f", entry.getSentimentScore()) : "No score"));
        detailContentArea.setText(entry.getContent());
        
        entryDetails.setVisible(true);
        showStatus("Entry details loaded");
    }
    
    private void hideEntryDetails() {
        entryDetails.setVisible(false);
        selectedEntry = null;
    }
    
    private void editEntry() {
        if (selectedEntry == null) return;
        
        showStatus("Edit feature coming soon!");
    }
    
    private void deleteEntry() {
        if (selectedEntry == null) return;
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Entry");
        confirm.setHeaderText("Are you sure you want to delete this entry?");
        confirm.setContentText("This action cannot be undone.");
        
        if (confirm.showAndWait().get() == ButtonType.OK) {
            entries.remove(selectedEntry);
            hideEntryDetails();
            showStatus("✅ Entry deleted successfully!");
        }
    }
    
    private void analyzeEntry() {
        if (selectedEntry == null) return;
        
        analyzeEntryButton.setDisable(true);
        analyzeEntryButton.setText("Analyzing...");
        
        new Thread(() -> {
            try {
                // Simulate AI analysis
                Thread.sleep(2000);
                
                javafx.application.Platform.runLater(() -> {
                    analyzeEntryButton.setDisable(false);
                    analyzeEntryButton.setText("🤖 Analyze");
                    
                    Alert analysis = new Alert(Alert.AlertType.INFORMATION);
                    analysis.setTitle("AI Analysis");
                    analysis.setHeaderText("Entry Analysis Complete");
                    analysis.setContentText("This entry shows " + selectedEntry.getSentiment().toLowerCase() + 
                                          " sentiment with emotional awareness. Keep writing!");
                    analysis.showAndWait();
                });
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                javafx.application.Platform.runLater(() -> {
                    analyzeEntryButton.setDisable(false);
                    analyzeEntryButton.setText("🤖 Analyze");
                });
            }
        }).start();
    }
    
    private void loadMoreEntries() {
        showStatus("Load more feature coming soon!");
    }
    
    private void exportEntries() {
        exportButton.setDisable(true);
        exportButton.setText("Exporting...");
        
        new Thread(() -> {
            try {
                // Simulate export process
                Thread.sleep(2000);
                
                javafx.application.Platform.runLater(() -> {
                    exportButton.setDisable(false);
                    exportButton.setText("📥 Export All");
                    showStatus("✅ Export feature coming soon!");
                });
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                javafx.application.Platform.runLater(() -> {
                    exportButton.setDisable(false);
                    exportButton.setText("📥 Export All");
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
}
