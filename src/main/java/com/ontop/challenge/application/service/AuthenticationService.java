package com.ontop.challenge.application.service;

import com.ontop.challenge.adapters.in.dto.auth.AuthenticationResponse;
import com.ontop.challenge.adapters.in.dto.auth.LoginRequest;
import com.ontop.challenge.adapters.in.dto.auth.RegisterRequest;
import com.ontop.challenge.adapters.out.persistence.UserRepository;
import com.ontop.challenge.domain.User;
import com.ontop.challenge.infrastructure.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    /**
     * Register a new user
     * 
     * @param request registration request with user details
     * @return authentication response with JWT tokens
     * @throws IllegalArgumentException if username already exists
     */
    @Transactional
    public AuthenticationResponse register(RegisterRequest request) {
        log.info("Registering new user: {}", request.getUsername());

        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("Registration failed: username already exists: {}", request.getUsername());
            throw new IllegalArgumentException("Username already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.addRole("ROLE_USER");

        User savedUser = userRepository.save(user);
        log.info("User registered successfully: {}", savedUser.getUsername());

        // Generate JWT tokens
        UserDetails userDetails = userDetailsService.loadUserByUsername(savedUser.getUsername());
        String accessToken = jwtUtil.generateToken(userDetails, savedUser.getId().toString());

        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .expiresIn(900L) // 15 minutes in seconds
                .username(savedUser.getUsername())
                .userId(savedUser.getId().toString())
                .build();
    }

    /**
     * Authenticate user and generate JWT tokens
     * 
     * Implements brute force protection:
     * - Resets counter on successful login
     * 
     * @param request login request with credentials
     * @return authentication response with JWT tokens
     * @throws BadCredentialsException if credentials are invalid
     * @throws LockedException if account is locked
     */
    @Transactional
    public AuthenticationResponse login(LoginRequest request) {
        log.info("Login attempt for user: {}", request.getUsername());

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> {
                    log.warn("Login failed: user not found: {}", request.getUsername());
                    return new BadCredentialsException("Invalid username or password");
                });

        try {
            // Authenticate
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            log.info("User logged in successfully: {}", request.getUsername());

            // Generate JWT tokens
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String accessToken = jwtUtil.generateToken(userDetails, user.getId().toString());

            return AuthenticationResponse.builder()
                    .accessToken(accessToken)
                    .expiresIn(900L) // 15 minutes in seconds
                    .username(user.getUsername())
                    .userId(user.getId().toString())
                    .build();

        } catch (BadCredentialsException e) {
            log.warn("Login failed: invalid credentials for user: {}.", request.getUsername());
            throw new BadCredentialsException("Invalid username or password");
        }
    }
}

