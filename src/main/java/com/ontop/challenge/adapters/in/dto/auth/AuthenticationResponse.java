package com.ontop.challenge.adapters.in.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Authentication response DTO
 * 
 * Contains JWT tokens for authentication
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticationResponse {

    private String accessToken;
    
    private Long expiresIn;
    private String username;
    private String userId;
}

