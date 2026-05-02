package com.ipl.config;

import com.mongodb.client.model.IndexOptions;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration
@RequiredArgsConstructor
public class MongoConfig {

    private final MongoTemplate mongoTemplate;

    @jakarta.annotation.PostConstruct
    public void initIndexes() {
        try {
            // Create unique compound index on userId and matchId for user_predictions collection
            Document indexKeys = new Document("userId", 1).append("matchId", 1);
            IndexOptions indexOptions = new IndexOptions().unique(true);

            mongoTemplate.getCollection("user_predictions").createIndex(indexKeys, indexOptions);
            System.out.println("✅ Created unique compound index on (userId, matchId) for user_predictions collection");
        } catch (Exception e) {
            System.err.println("❌ Failed to create MongoDB index: " + e.getMessage());
        }
    }
}