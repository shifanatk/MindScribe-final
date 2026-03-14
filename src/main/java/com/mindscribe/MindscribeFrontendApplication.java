package com.mindscribe;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import java.io.IOException;

@SpringBootApplication
@ComponentScan(basePackages = "com.mindscribe")
public class MindscribeFrontendApplication extends Application {

    private ConfigurableApplicationContext springContext;

    @Override
    public void init() {
        // Initialize Spring context
        String[] args = getParameters().getRaw().toArray(new String[0]);
        springContext = SpringApplication.run(MindscribeFrontendApplication.class, args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        // Load FXML
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main-view.fxml"));
        loader.setControllerFactory(springContext::getBean);
        
        Parent root = loader.load();
        
        // Setup scene
        Scene scene = new Scene(root, 1200, 800);
        scene.getStylesheets().add(getClass().getResource("/styles/style.css").toExternalForm());
        
        // Setup stage
        primaryStage.setTitle("MindScribe - Your Digital Journal");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        
        // Apply smooth entrance
        primaryStage.setOpacity(0);
        primaryStage.show();
        
        // Fade in the stage
        javafx.animation.FadeTransition fade = new javafx.animation.FadeTransition(javafx.util.Duration.millis(1000), primaryStage.getScene().getRoot());
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
        
        primaryStage.setOpacity(1);
    }

    @Override
    public void stop() {
        springContext.close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
