package com.ontop.challenge.adapters.in;

import com.ontop.challenge.adapters.in.dto.auth.AuthenticationResponse;
import com.ontop.challenge.adapters.in.dto.auth.LoginRequest;
import com.ontop.challenge.adapters.in.dto.auth.RegisterRequest;
import com.ontop.challenge.application.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    /**
     * Register a new user
     * 
     * @param request registration request
     * @return authentication response with JWT tokens
     */
    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(@Valid @RequestBody RegisterRequest request) {
        request.setUsername(Encode.forHtml(request.getUsername())); //prevent XSS

        log.info("Registration request received for username: {}", request.getUsername());

        AuthenticationResponse response = authenticationService.register(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Login endpoint
     * 
     * @param request login request
     * @return authentication response with JWT tokens
     */
    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(@Valid @RequestBody LoginRequest request) {
        request.setUsername(Encode.forHtml(request.getUsername())); //  prevent XSS

        log.info("Login request received for username: {}", request.getUsername());
        
        AuthenticationResponse response = authenticationService.login(request);
        
        return ResponseEntity.ok(response);
    }

    // Refresh token, etc...
}

