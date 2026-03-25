package com.mindscribe;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@EnableMongoRepositories(basePackages = "com.mindscribe.repository.mongodb")
public class MindscribeBackendApplication {

    public static void main(String[] args) {
        // Load .env file
        Dotenv dotenv = Dotenv.configure().load();
        System.setProperty("MONGODB_URI", dotenv.get("MONGODB_URI"));
        
        SpringApplication.run(MindscribeBackendApplication.class, args);
    }
}
