package com.mindscribe.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Enables JPA repositories for H2 (JournalEntry, DiaryEntry).
 */
@Configuration
@EnableJpaRepositories(
        basePackages = "com.mindscribe.repository.h2",
        entityManagerFactoryRef = "h2EntityManagerFactory",
        transactionManagerRef = "h2TransactionManager"
)
public class H2JpaConfig {
}

