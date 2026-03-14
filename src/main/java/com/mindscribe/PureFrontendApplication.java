package com.mindscribe;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.io.IOException;

public class PureFrontendApplication extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException {
        try {
            // Load FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login-view.fxml"));
            Parent root = loader.load();
            
            // Setup scene
            Scene scene = new Scene(root, 400, 500);
            scene.getStylesheets().add(getClass().getResource("/styles/style.css").toExternalForm());
            
            // Setup stage
            primaryStage.setTitle("MindScribe - Your Digital Journal");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(400);
            primaryStage.setMinHeight(500);
            
            // Apply smooth entrance
            primaryStage.setOpacity(0);
            primaryStage.show();
            
            // Fade in the stage
            javafx.animation.FadeTransition fade = new javafx.animation.FadeTransition(javafx.util.Duration.millis(1000), primaryStage.getScene().getRoot());
            fade.setFromValue(0);
            fade.setToValue(1);
            fade.play();
            
            primaryStage.setOpacity(1);
            
        } catch (IOException e) {
            showAlert("Error loading UI", "Failed to load the main interface: " + e.getMessage());
            throw e;
        }
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
