package com.mindscribe;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

public class MindScribeApplication extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        try {
            // Load Login View
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login-view-simple.fxml"));
            Parent root = loader.load();
            
            // Setup Scene
            Scene scene = new Scene(root, 400, 500);
            scene.getStylesheets().add(getClass().getResource("/styles/style.css").toExternalForm());
            
            // Setup Stage
            primaryStage.setTitle("MindScribe - Your Digital Journal");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(400);
            primaryStage.setMinHeight(500);
            primaryStage.setResizable(false);
            
            // Apply smooth entrance
            primaryStage.setOpacity(0);
            primaryStage.show();
            
            // Fade in effect
            javafx.animation.FadeTransition fade = new javafx.animation.FadeTransition(javafx.util.Duration.millis(1000), primaryStage.getScene().getRoot());
            fade.setFromValue(0);
            fade.setToValue(1);
            fade.play();
            
            primaryStage.setOpacity(1);
            
        } catch (Exception e) {
            showAlert("Error loading UI", "Failed to load login interface: " + e.getMessage());
            throw e;
        }
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
