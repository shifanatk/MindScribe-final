package com.mindscribe;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.animation.*;
import javafx.util.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ElegantHomeController {

    @FXML private Button writeJournalButton;
    @FXML private Button viewEntriesButton;
    @FXML private Button analyticsButton;
    @FXML private Button aiInsightsButton;
    @FXML private Button settingsButton;
    @FXML private Button logoutButton;
    @FXML private Label dateLabel;
    @FXML private Label welcomeLabel;
    @FXML private Label statusLabel;
    @FXML private VBox contentContainer;

    @FXML
    public void initialize() {
        setupEventHandlers();
        setupWelcomeMessage();
        setCurrentDate();
        loadDynamicContent();
    }
    
    private void setupEventHandlers() {
        writeJournalButton.setOnAction(e -> navigateToPage("write-journal"));
        viewEntriesButton.setOnAction(e -> navigateToPage("previous-entries"));
        analyticsButton.setOnAction(e -> navigateToPage("mood-dashboard"));
        aiInsightsButton.setOnAction(e -> navigateToPage("ai-insights"));
        settingsButton.setOnAction(e -> showComingSoon("Settings"));
        logoutButton.setOnAction(e -> handleLogout());
    }
    
    private void setupWelcomeMessage() {
        String currentUser = SessionManager.getCurrentUser();
        String username = currentUser != null ? currentUser.split("@")[0] : "User";
        
        String[] greetings = {
            "Welcome back, " + username + "! Ready to capture today's thoughts?",
            "Good to see you, " + username + "! How are you feeling today?",
            "Welcome back, " + username + "! Let's explore your emotional journey today.",
            "Great to have you here, " + username + "! What's on your mind today?",
            "Welcome back, " + username + "! Time to reflect and grow."
        };
        
        String greeting = greetings[(int)(Math.random() * greetings.length)];
        welcomeLabel.setText(greeting);
        welcomeLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 500; -fx-text-fill: #A5B4FC; -fx-font-style: italic;");
    }
    
    private void setCurrentDate() {
        LocalDate today = LocalDate.now();
        String formattedDate = today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"));
        dateLabel.setText(formattedDate);
    }
    
    private void loadDynamicContent() {
        // Clear existing content
        contentContainer.getChildren().clear();
        
        // Create welcome section
        VBox welcomeSection = createWelcomeSection();
        contentContainer.getChildren().add(welcomeSection);
        
        // Create dynamic quote
        VBox quoteSection = createQuoteSection();
        contentContainer.getChildren().add(quoteSection);
        
        // Create mood indicator
        HBox moodIndicator = createMoodIndicator();
        contentContainer.getChildren().add(moodIndicator);
    }
    
    private VBox createWelcomeSection() {
        VBox section = new VBox(15);
        section.setAlignment(javafx.geometry.Pos.CENTER);
        
        Label title = new Label("✨ MindScribe");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        title.setTextFill(Color.web("#E2E8F0"));
        
        Label subtitle = new Label("Your Personal Mental Wellness Companion");
        subtitle.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 14));
        subtitle.setTextFill(Color.web("#9CA3AF"));
        
        section.getChildren().addAll(title, subtitle);
        section.setStyle("-fx-background-color: #2D2A3E40; -fx-background-radius: 15px; -fx-padding: 20px;");
        
        return section;
    }
    
    private HBox createActionButtons() {
        HBox buttonBox = new HBox(25);
        buttonBox.setAlignment(javafx.geometry.Pos.CENTER);
        
        Button[] buttons = {
            createStyledButton("📝 Write Journal", "#10B981"),
            createStyledButton("📚 View Entries", "#3B82F6"),
            createStyledButton("📊 Analytics", "#8B5CF6"),
            createStyledButton("🤖 AI Insights", "#A855F7")
        };
        
        buttonBox.getChildren().addAll(buttons);
        return buttonBox;
    }
    
    private Button createStyledButton(String text, String color) {
        Button button = new Button(text);
        button.setFont(Font.font("Segoe UI", FontWeight.MEDIUM, 14));
        button.setTextFill(Color.WHITE);
        button.setStyle(String.format(
            "-fx-background-color: %s; " +
            "-fx-background-radius: 25px; " +
            "-fx-padding: 12px 24px; " +
            "-fx-cursor: hand; " +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 8, 0, 0, 4); " +
            "-fx-transition: all 0.3s ease;"
        , color));
        
        // Add hover effect
        button.setOnMouseEntered(e -> {
            button.setStyle(String.format(
                "-fx-background-color: %s; " +
                "-fx-background-radius: 25px; " +
                "-fx-padding: 12px 24px; " +
                "-fx-cursor: hand; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 12, 0, 0, 6); " +
                "-fx-scale-x: 1.05; -fx-scale-y: 1.05;"
            , color));
        });
        
        button.setOnMouseExited(e -> {
            button.setStyle(String.format(
                "-fx-background-color: %s; " +
                "-fx-background-radius: 25px; " +
                "-fx-padding: 12px 24px; " +
                "-fx-cursor: hand; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 8, 0, 0, 4); " +
                "-fx-transition: all 0.3s ease;"
            , color));
        });
        
        return button;
    }
    
    private VBox createQuoteSection() {
        VBox section = new VBox(15);
        section.setAlignment(javafx.geometry.Pos.CENTER);
        
        Label quoteTitle = new Label("💭 Daily Inspiration");
        quoteTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        quoteTitle.setTextFill(Color.web("#A5B4FC"));
        
        String[] quotes = {
            "\"The journey of a thousand miles begins with a single step.\" - Lao Tzu",
            "\"Your present circumstances don't determine where you can go; they merely determine where you start.\" - Nido Qubein",
            "\"The only way to do great work is to love what you do.\" - Steve Jobs",
            "\"Believe you can and you're halfway there.\" - Theodore Roosevelt"
        };
        
        String quote = quotes[(int)(Math.random() * quotes.length)];
        
        Label quoteLabel = new Label(quote);
        quoteLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 14));
        quoteLabel.setTextFill(Color.web("#E2E8F0"));
        quoteLabel.setWrapText(true);
        quoteLabel.setMaxWidth(400);
        quoteLabel.setAlignment(javafx.geometry.Pos.CENTER);
        
        section.getChildren().addAll(quoteTitle, quoteLabel);
        section.setStyle("-fx-background-color: #2D2A3E40; -fx-background-radius: 15px; -fx-padding: 20px;");
        
        return section;
    }
    
    private HBox createMoodIndicator() {
        HBox indicator = new HBox(20);
        indicator.setAlignment(javafx.geometry.Pos.CENTER);
        
        Circle moodCircle = new Circle(8);
        moodCircle.setFill(Color.web("#10B981"));
        moodCircle.setStroke(Color.web("#059669"));
        moodCircle.setStrokeWidth(2);
        
        Label moodLabel = new Label("Feeling Good");
        moodLabel.setFont(Font.font("Segoe UI", FontWeight.MEDIUM, 12));
        moodLabel.setTextFill(Color.web("#E2E8F0"));
        
        indicator.getChildren().addAll(moodCircle, moodLabel);
        indicator.setStyle("-fx-background-color: #2D2A3E40; -fx-background-radius: 20px; -fx-padding: 15px;");
        
        return indicator;
    }
    
    private void navigateToPage(String page) {
        try {
            String fxmlFile = "/fxml/" + page + "-view.fxml";
            System.out.println("🔍 Attempting to load: " + fxmlFile);
            
            // Check if resource exists
            if (getClass().getResource(fxmlFile) == null) {
                throw new RuntimeException("FXML file not found: " + fxmlFile);
            }
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            javafx.scene.Parent root = loader.load();
            
            System.out.println("✅ Successfully loaded: " + fxmlFile);
            
            Stage stage = (Stage) writeJournalButton.getScene().getWindow();
            Scene scene = new Scene(root, 1200, 800);
            scene.getStylesheets().add(getClass().getResource("/styles/style.css").toExternalForm());
            
            String title = "MindScribe - " + page.substring(0, 1).toUpperCase() + page.substring(1).replace("-", " ");
            stage.setTitle(title);
            stage.setScene(scene);
            stage.show();
            
            System.out.println("🎉 Successfully navigated to: " + title);
            
        } catch (Exception e) {
            System.err.println("❌ Error loading " + page + ": " + e.getMessage());
            e.printStackTrace();
            showError("Error loading " + page + ": " + e.getMessage());
        }
    }
    
    private void showComingSoon(String feature) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Coming Soon");
        alert.setHeaderText(null);
        alert.setContentText(feature + " is coming soon! We're working on making it amazing.");
        alert.showAndWait();
    }
    
    private void handleLogout() {
        SessionManager.logout();
        
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login-view.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) logoutButton.getScene().getWindow();
            Scene scene = new Scene(root, 400, 500);
            scene.getStylesheets().add(getClass().getResource("/styles/style.css").toExternalForm());
            
            stage.setTitle("MindScribe - Login");
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            showError("Error loading login: " + e.getMessage());
        }
    }
    
    private void showError(String message) {
        statusLabel.setText(message);
        statusLabel.setTextFill(Color.web("#EF4444"));
        statusLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
        
        // Auto-hide after 3 seconds
        PauseTransition pause = new PauseTransition(Duration.seconds(3));
        pause.setOnFinished(e -> statusLabel.setText(""));
        pause.play();
    }
}
