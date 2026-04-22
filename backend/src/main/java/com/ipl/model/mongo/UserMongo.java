package com.ipl.model.mongo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserMongo {

    @Id
    private Long id;

    @Indexed(unique = true)
    private String username;

    @Indexed(unique = true)
    private String uniqueUserId;

    @Indexed(unique = true)
    private String email;

    private String password;

    private String fullName;

    private Integer points = 0;

    private Integer rank = 0;

    private Boolean isActive = false;

    private Boolean emailVerified = false;

    private String role = "USER";

    private Long createdAt;

    private Long updatedAt;
}
