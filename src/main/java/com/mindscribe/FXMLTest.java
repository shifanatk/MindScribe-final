package com.mindscribe;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import java.net.URL;

/**
 * Simple test to verify FXML loading works correctly
 */
public class FXMLTest {
    
    public static void main(String[] args) {
        try {
            System.out.println("Testing FXML loading...");
            
            // Test mood dashboard
            URL moodDashboardURL = FXMLTest.class.getResource("/fxml/mood-dashboard-view.fxml");
            if (moodDashboardURL != null) {
                System.out.println("✅ mood-dashboard-view.fxml found at: " + moodDashboardURL);
                
                FXMLLoader moodLoader = new FXMLLoader(moodDashboardURL);
                Parent moodRoot = moodLoader.load();
                System.out.println("✅ Mood Dashboard FXML loaded successfully");
                System.out.println("   Controller: " + moodLoader.getController());
            } else {
                System.out.println("❌ mood-dashboard-view.fxml NOT found");
            }
            
            // Test AI insights
            URL aiInsightsURL = FXMLTest.class.getResource("/fxml/ai-insights-view.fxml");
            if (aiInsightsURL != null) {
                System.out.println("✅ ai-insights-view.fxml found at: " + aiInsightsURL);
                
                FXMLLoader aiLoader = new FXMLLoader(aiInsightsURL);
                Parent aiRoot = aiLoader.load();
                System.out.println("✅ AI Insights FXML loaded successfully");
                System.out.println("   Controller: " + aiLoader.getController());
            } else {
                System.out.println("❌ ai-insights-view.fxml NOT found");
            }
            
        } catch (Exception e) {
            System.err.println("❌ FXML loading failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
