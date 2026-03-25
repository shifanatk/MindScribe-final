package com.mindscribe;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class DashboardTest extends Application {
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        try {
            System.out.println("Testing Mood Dashboard...");
            
            // Test Mood Dashboard
            FXMLLoader moodLoader = new FXMLLoader(getClass().getResource("/fxml/mood-dashboard-view.fxml"));
            Parent moodRoot = moodLoader.load();
            MoodDashboardController moodController = moodLoader.getController();
            System.out.println("✅ Mood Dashboard loaded successfully");
            System.out.println("   Controller: " + moodController);
            
            // Test AI Insights
            System.out.println("Testing AI Insights...");
            FXMLLoader aiLoader = new FXMLLoader(getClass().getResource("/fxml/ai-insights-view.fxml"));
            Parent aiRoot = aiLoader.load();
            AIInsightsController aiController = aiLoader.getController();
            System.out.println("✅ AI Insights loaded successfully");
            System.out.println("   Controller: " + aiController);
            
            // Show mood dashboard for testing
            Scene scene = new Scene(moodRoot, 1200, 800);
            scene.getStylesheets().add(getClass().getResource("/styles/style.css").toExternalForm());
            
            primaryStage.setTitle("Dashboard Test - Mood Dashboard");
            primaryStage.setScene(scene);
            primaryStage.show();
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
