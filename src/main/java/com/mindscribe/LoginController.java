package com.mindscribe;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Hyperlink signupLink;
    @FXML private Label errorLabel;
    
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @FXML
    public void initialize() {
        clearError();
        loginButton.setOnAction(e -> handleLogin());
        signupLink.setOnAction(e -> handleSignup());
        passwordField.setOnAction(e -> handleLogin());
        usernameField.setOnAction(e -> passwordField.requestFocus());
    }
    
    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        
        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter username and password");
            return;
        }
        
        try {
            loginButton.setDisable(true);
            loginButton.setText("Logging in...");
            
            String requestBody = String.format(
                "{\"username\":\"%s\",\"password\":\"%s\"}", 
                username, password
            );
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                LoginResponse loginResponse = objectMapper.readValue(response.body(), LoginResponse.class);
                if (loginResponse.isSuccess() || loginResponse.getUser() != null) {
                    // Store session with credentials for API authentication
                    SessionManager.login(username, password);
                    navigateToHome();
                } else {
                    showError("Invalid username or password");
                }
            } else {
                showError("Login failed: " + response.statusCode());
            }
        } catch (Exception e) {
            showError("Connection error: " + e.getMessage());
        } finally {
            loginButton.setDisable(false);
            loginButton.setText("Login");
        }
    }
    
    @FXML
    private void handleSignup() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/signup-view.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) usernameField.getScene().getWindow();
            Scene scene = new Scene(root, 400, 500);
            scene.getStylesheets().add(getClass().getResource("/styles/style.css").toExternalForm());
            
            stage.setTitle("MindScribe - Sign Up");
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            showError("Error loading signup page: " + e.getMessage());
        }
    }
    
    private void navigateToHome() {
        try {
            System.out.println("Attempting to load elegant-home-view.fxml...");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/elegant-home-view.fxml"));
            
            if (loader.getLocation() == null) {
                System.out.println("ERROR: Could not find elegant-home-view.fxml");
                showError("Could not find elegant-home-view.fxml file");
                return;
            }
            
            Parent root = loader.load();
            System.out.println("Successfully loaded elegant-home-view.fxml");
            
            Stage stage = (Stage) usernameField.getScene().getWindow();
            Scene scene = new Scene(root, 1200, 800);
            
            String cssPath = getClass().getResource("/styles/style.css").toExternalForm();
            System.out.println("Loading CSS from: " + cssPath);
            scene.getStylesheets().add(cssPath);
            
            stage.setTitle("MindScribe - Home");
            stage.setScene(scene);
            stage.show();
            
            System.out.println("Elegant home page loaded successfully");
        } catch (Exception e) {
            System.out.println("Error loading elegant home page: " + e.getMessage());
            e.printStackTrace();
            showError("Error loading home page: " + e.getMessage());
        }
    }
    
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
    
    private void clearError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
    }
    
    private static class LoginResponse {
        private String message;
        private Map<String, Object> user;
        private boolean success;
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public Map<String, Object> getUser() { return user; }
        public void setUser(Map<String, Object> user) { this.user = user; }
        public boolean isSuccess() { 
            return success || (user != null && !user.isEmpty()); 
        }
        public void setSuccess(boolean success) { this.success = success; }
    }
}
