package com.ontop.challenge.adapters.in;

import com.ontop.challenge.adapters.in.dto.auth.AuthenticationResponse;
import com.ontop.challenge.adapters.in.dto.auth.LoginRequest;
import com.ontop.challenge.adapters.in.dto.auth.RegisterRequest;
import com.ontop.challenge.application.service.AuthenticationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationControllerTest {

    @Mock
    private AuthenticationService authenticationService;

    @InjectMocks
    private AuthenticationController authenticationController;

    @Test
    void register_Success_ReturnsCreatedWithToken() {
        RegisterRequest request = new RegisterRequest("testuser", "Password123!");
        AuthenticationResponse expectedResponse = AuthenticationResponse.builder()
                .accessToken("jwt-token-123")
                .expiresIn(3600L)
                .username("testuser")
                .userId("user-123")
                .build();

        when(authenticationService.register(any(RegisterRequest.class))).thenReturn(expectedResponse);

        ResponseEntity<AuthenticationResponse> response = authenticationController.register(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getAccessToken()).isEqualTo("jwt-token-123");
        assertThat(response.getBody().getUsername()).isEqualTo("testuser");
        assertThat(response.getBody().getUserId()).isEqualTo("user-123");
        assertThat(response.getBody().getExpiresIn()).isEqualTo(3600L);
    }

    @Test
    void register_WithXSSAttempt_EncodesUsername() {
        RegisterRequest request = new RegisterRequest("<script>alert('xss')</script>", "Password123!");
        AuthenticationResponse expectedResponse = AuthenticationResponse.builder()
                .accessToken("jwt-token-123")
                .build();

        when(authenticationService.register(any(RegisterRequest.class))).thenReturn(expectedResponse);

        authenticationController.register(request);

        ArgumentCaptor<RegisterRequest> requestCaptor = ArgumentCaptor.forClass(RegisterRequest.class);
        verify(authenticationService).register(requestCaptor.capture());

        RegisterRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getUsername()).contains("&lt;script&gt;");
        assertThat(capturedRequest.getUsername()).doesNotContain("<script>");
    }

    @Test
    void login_Success_ReturnsOkWithToken() {
        LoginRequest request = new LoginRequest("testuser", "Password123!");
        AuthenticationResponse expectedResponse = AuthenticationResponse.builder()
                .accessToken("jwt-token-789")
                .expiresIn(3600L)
                .username("testuser")
                .userId("user-123")
                .build();

        when(authenticationService.login(any(LoginRequest.class))).thenReturn(expectedResponse);

        ResponseEntity<AuthenticationResponse> response = authenticationController.login(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getAccessToken()).isEqualTo("jwt-token-789");
        assertThat(response.getBody().getUsername()).isEqualTo("testuser");
        assertThat(response.getBody().getUserId()).isEqualTo("user-123");
        assertThat(response.getBody().getExpiresIn()).isEqualTo(3600L);

        verify(authenticationService).login(any(LoginRequest.class));
    }

    @Test
    void login_WithXSSAttempt_EncodesUsername() {
        LoginRequest request = new LoginRequest("<img src=x onerror=alert('xss')>", "Password123!");
        AuthenticationResponse expectedResponse = AuthenticationResponse.builder()
                .accessToken("jwt-token-789")
                .build();

        when(authenticationService.login(any(LoginRequest.class))).thenReturn(expectedResponse);

        authenticationController.login(request);

        ArgumentCaptor<LoginRequest> requestCaptor = ArgumentCaptor.forClass(LoginRequest.class);
        verify(authenticationService).login(requestCaptor.capture());
        

        LoginRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getUsername()).contains("&lt;img");
        assertThat(capturedRequest.getUsername()).doesNotContain("<img");
    }

    @Test
    void login_WithValidCredentials_CallsServiceOnce() {
        LoginRequest request = new LoginRequest("validuser", "ValidPass123!");
        AuthenticationResponse expectedResponse = AuthenticationResponse.builder()
                .accessToken("token")
                .build();

        when(authenticationService.login(any(LoginRequest.class))).thenReturn(expectedResponse);

        authenticationController.login(request);

        verify(authenticationService).login(any(LoginRequest.class));
    }

    @Test
    void register_WithValidData_CallsServiceOnce() {
        RegisterRequest request = new RegisterRequest("newuser", "NewPass123!");
        AuthenticationResponse expectedResponse = AuthenticationResponse.builder()
                .accessToken("token")
                .build();

        when(authenticationService.register(any(RegisterRequest.class))).thenReturn(expectedResponse);

        authenticationController.register(request);

        verify(authenticationService).register(any(RegisterRequest.class));
    }
}

