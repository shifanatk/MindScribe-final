package com.mindscribe;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class HomeController {

    @FXML private Button writeJournalButton;
    @FXML private Button viewEntriesButton;
    @FXML private Button analyticsButton;
    @FXML private Button aiButton;
    @FXML private Button aiInsightsButton;
    @FXML private Button settingsButton;
    @FXML private Button logoutButton;
    @FXML private Label dateLabel;

    @FXML
    public void initialize() {
        setupEventHandlers();
        setCurrentDate();
    }
    
    private void setupEventHandlers() {
        writeJournalButton.setOnAction(e -> navigateToPage("write-journal"));
        viewEntriesButton.setOnAction(e -> navigateToPage("previous-entries"));
        analyticsButton.setOnAction(e -> navigateToPage("mood-dashboard"));
        aiInsightsButton.setOnAction(e -> navigateToPage("ai-insights"));
        aiButton.setOnAction(e -> showAlert("AI Insights coming soon!"));
        settingsButton.setOnAction(e -> showAlert("Settings coming soon!"));
        logoutButton.setOnAction(e -> handleLogout());
    }
    
    private void setCurrentDate() {
        LocalDate today = LocalDate.now();
        String formattedDate = today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"));
        dateLabel.setText(formattedDate);
    }
    
    
    private void navigateToPage(String page) {
        try {
            if(page.equals("home")) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/elegant-home-view.fxml"));
                Parent root = loader.load();
                
                Stage stage = (Stage) writeJournalButton.getScene().getWindow();
                Scene scene = new Scene(root, 1200, 800);
                scene.getStylesheets().add(getClass().getResource("/styles/style.css").toExternalForm());
                
                stage.setTitle("MindScribe - Home");
                stage.setScene(scene);
                stage.show();
            } else {
                String fxmlFile = "/fxml/" + page + "-view.fxml";
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
                Parent root = loader.load();
                
                Stage stage = (Stage) writeJournalButton.getScene().getWindow();
                Scene scene = new Scene(root, 1200, 800);
                scene.getStylesheets().add(getClass().getResource("/styles/style.css").toExternalForm());
                
                String title = "MindScribe - " + page.substring(0, 1).toUpperCase() + page.substring(1);
                stage.setTitle(title);
                stage.setScene(scene);
                stage.show();
            }
        } catch (Exception e) {
            if(page.equals("home")) {
                showError("Error loading home page: " + e.getMessage());
            } else {
                showAlert("Error loading " + page + " page: " + e.getMessage());
            }
        }
    }
    
    private void handleLogout() {
        // Clear session
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
            showError("Error loading login page: " + e.getMessage());
        }
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
