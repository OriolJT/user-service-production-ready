package com.orioljt.userservice.api.controller;

import com.orioljt.userservice.IntegrationTestBase;
import com.orioljt.userservice.application.dto.AuthResponse;
import com.orioljt.userservice.application.dto.LoginRequest;
import com.orioljt.userservice.application.dto.RefreshTokenRequest;
import com.orioljt.userservice.application.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AuthControllerIT extends IntegrationTestBase {

    @Test
    void register_validRequest_returns201() {
        RegisterRequest request = new RegisterRequest(
                "register-test@example.com", "registeruser", "password123", "Test", "User"
        );

        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/api/auth/register", request, AuthResponse.class
        );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().accessToken());
        assertNotNull(response.getBody().refreshToken());
        assertEquals("Bearer", response.getBody().tokenType());
    }

    @Test
    void register_duplicateEmail_returns409() {
        RegisterRequest firstRequest = new RegisterRequest(
                "duplicate@example.com", "uniqueuser1", "password123", "Test", "User"
        );
        restTemplate.postForEntity("/api/auth/register", firstRequest, AuthResponse.class);

        RegisterRequest duplicateRequest = new RegisterRequest(
                "duplicate@example.com", "uniqueuser2", "password123", "Test", "User"
        );

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/register", duplicateRequest, String.class
        );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    void register_invalidEmail_returns400() {
        RegisterRequest request = new RegisterRequest(
                "not-an-email", "validuser", "password123", "Test", "User"
        );

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/register", request, String.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void login_validCredentials_returns200() {
        RegisterRequest registerRequest = new RegisterRequest(
                "login-test@example.com", "loginuser", "password123", "Test", "User"
        );
        restTemplate.postForEntity("/api/auth/register", registerRequest, AuthResponse.class);

        LoginRequest loginRequest = new LoginRequest("login-test@example.com", "password123");

        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/api/auth/login", loginRequest, AuthResponse.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().accessToken());
        assertNotNull(response.getBody().refreshToken());
    }

    @Test
    void login_invalidCredentials_returns401() {
        LoginRequest loginRequest = new LoginRequest("nonexistent@example.com", "wrongpassword");

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/login", loginRequest, String.class
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void refresh_validToken_returns200() {
        RegisterRequest registerRequest = new RegisterRequest(
                "refresh-test@example.com", "refreshuser", "password123", "Test", "User"
        );
        ResponseEntity<AuthResponse> registerResponse = restTemplate.postForEntity(
                "/api/auth/register", registerRequest, AuthResponse.class
        );
        String refreshToken = registerResponse.getBody().refreshToken();

        RefreshTokenRequest refreshRequest = new RefreshTokenRequest(refreshToken);

        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/api/auth/refresh", refreshRequest, AuthResponse.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().accessToken());
        assertNotNull(response.getBody().refreshToken());
    }
}
