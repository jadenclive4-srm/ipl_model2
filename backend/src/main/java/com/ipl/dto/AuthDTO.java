package com.ipl.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthDTO {

    private String username;
    private String uniqueUserId;
    private String email;
    private String identifier;
    private String password;
    private String fullName;
    private String token;
    private Long userId;
    private String role;
}