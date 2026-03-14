package com.mindscribe;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.time.Duration;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SignupController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button signupButton;
    @FXML private Hyperlink loginLink;
    @FXML private Label errorLabel;
    
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @FXML
    public void initialize() {
        // Setup event handlers
        signupButton.setOnAction(e -> handleSignup());
        loginLink.setOnAction(e -> handleBackToLogin());
        
        // Tab order
        usernameField.setOnAction(e -> passwordField.requestFocus());
        passwordField.setOnAction(e -> confirmPasswordField.requestFocus());
        confirmPasswordField.setOnAction(e -> handleSignup());
    }
    
    private void handleSignup() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        String confirmPassword = confirmPasswordField.getText().trim();
        
        // Validation
        if (username.isEmpty() || password.isEmpty()) {
            showError("Please fill all fields");
            return;
        }
        
        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match");
            return;
        }
        
        if (password.length() < 6) {
            showError("Password must be at least 6 characters");
            return;
        }
        
        // Disable signup button during request
        signupButton.setDisable(true);
        signupButton.setText("Creating account...");
        
        // Register with backend
        registerWithBackend(username, password);
    }
    
    private void registerWithBackend(String username, String password) {
        new Thread(() -> {
            try {
                // Create signup request body
                String requestBody = String.format(
                    "{\"username\":\"%s\",\"password\":\"%s\"}", 
                    username, password
                );
                
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/auth/register"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
                
                HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());
                
                javafx.application.Platform.runLater(() -> {
                    signupButton.setDisable(false);
                    signupButton.setText("Sign Up");
                    
                    if (response.statusCode() == 201) {
                        showSuccess("Account created successfully! Please login.");
                        // Navigate back to login after 2 seconds
                        new Thread(() -> {
                            try {
                                Thread.sleep(2000);
                                javafx.application.Platform.runLater(this::handleBackToLogin);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }).start();
                    } else {
                        showError("Username already exists. Please try a different one.");
                    }
                });
                
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    signupButton.setDisable(false);
                    signupButton.setText("Sign Up");
                    showError("Connection error. Make sure backend is running on port 8080");
                });
            }
        }).start();
    }
    
    private void handleBackToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login-view.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) usernameField.getScene().getWindow();
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
        errorLabel.setText(message);
        errorLabel.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 12px;");
    }
    
    private void showSuccess(String message) {
        errorLabel.setText(message);
        errorLabel.setStyle("-fx-text-fill: #10B981; -fx-font-size: 12px;");
    }
}
