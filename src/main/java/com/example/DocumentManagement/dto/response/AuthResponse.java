package com.example.DocumentManagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class AuthResponse {

    private String token;
    private String type = "Bearer";
    private UUID id;
    private String email;
    private String fullName;
    private String role;

    public AuthResponse(String token, UUID id, String email, String fullName, String role) {
        this.token = token;
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
    }
}
