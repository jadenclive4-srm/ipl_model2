package com.ipl.model.mongo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "user_points")
public class UserPoints {
    @Id
    private String id;
    private Long userId;
    private String username;
    private String fullName;
    private Long totalPoints;
    private Integer totalPredictions;
    private Integer correctPredictions;
    private Long lastUpdated;
}