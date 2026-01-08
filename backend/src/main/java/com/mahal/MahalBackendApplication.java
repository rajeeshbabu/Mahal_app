package com.mahal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan({ "com.mahal.subscription.model", "com.mahal.util" })
@EnableJpaRepositories("com.mahal.subscription.repository")
@org.springframework.scheduling.annotation.EnableScheduling
public class MahalBackendApplication {

    public static void main(String[] args) {
        // Ensure database directory exists before Spring Boot initializes
        // Hibernate/SQLite
        ensureDatabaseDirectoryExists();
        SpringApplication.run(MahalBackendApplication.class, args);
    }

    private static void ensureDatabaseDirectoryExists() {
        try {
            String userHome = System.getProperty("user.home");
            java.nio.file.Path dbPath = java.nio.file.Paths.get(userHome, "AppData", "Roaming", "MahalApp", "data");
            if (java.nio.file.Files.notExists(dbPath)) {
                java.nio.file.Files.createDirectories(dbPath);
                System.out.println("Created database directory: " + dbPath.toString());
            }
        } catch (Exception e) {
            System.err.println("Could not create database directory: " + e.getMessage());
        }
    }
}
