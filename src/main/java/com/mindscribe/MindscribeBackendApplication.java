package com.mindscribe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@EnableMongoRepositories(basePackages = "com.mindscribe.repository.mongodb")
public class MindscribeBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(MindscribeBackendApplication.class, args);
    }
}
