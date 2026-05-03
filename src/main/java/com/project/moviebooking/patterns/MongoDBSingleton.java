package com.project.moviebooking.patterns;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * ===================================================
 * DESIGN PATTERN 1: SINGLETON PATTERN
 * ===================================================
 * Purpose: Ensure only ONE instance of MongoDBConnection
 *          is created throughout the application lifecycle.
 *
 * Why Singleton here?
 * - Database connections are expensive to create
 * - We need a shared, globally accessible connection
 * - Prevents multiple connection pool creation
 *
 * How it works:
 * - Private static instance
 * - Private constructor (prevents external instantiation)
 * - Thread-safe getInstance() method using synchronized
 *
 * In Spring Boot: @Component makes the bean a Spring-managed
 * singleton automatically (Singleton via IoC container)
 * ===================================================
 */
@Component
public class MongoDBSingleton {

    // Step 1: Private static instance (the single instance)
    private static MongoDBSingleton instance;

    // MongoDB client connection
    private MongoClient mongoClient;

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Value("${spring.data.mongodb.database}")
    private String databaseName;

    // Step 2: Private constructor - prevents direct instantiation
    // Spring manages this as a bean, so constructor is called once
    private MongoDBSingleton() {
        // Spring will inject this via @Value after construction
    }

    /**
     * Step 3: Thread-safe getInstance() method
     * Uses double-checked locking for performance + thread safety
     *
     * @param uri MongoDB connection URI
     * @param dbName Database name
     * @return single instance of MongoDBSingleton
     */
    public static synchronized MongoDBSingleton getInstance(String uri, String dbName) {
        if (instance == null) {
            synchronized (MongoDBSingleton.class) {
                if (instance == null) {
                    instance = new MongoDBSingleton();
                    instance.mongoUri = uri;
                    instance.databaseName = dbName;
                    instance.mongoClient = MongoClients.create(uri);
                    System.out.println("✅ [SINGLETON] MongoDBSingleton instance created - connection established");
                }
            }
        }
        return instance;
    }

    /**
     * Get MongoDB database reference
     */
    public MongoDatabase getDatabase() {
        return mongoClient.getDatabase(databaseName);
    }

    /**
     * Get the underlying MongoClient
     */
    public MongoClient getMongoClient() {
        return mongoClient;
    }
}
