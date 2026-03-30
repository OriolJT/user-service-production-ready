package com.orioljt.userservice.api.controller;

import com.orioljt.userservice.IntegrationTestBase;
import com.orioljt.userservice.application.dto.UpdateProfileRequest;
import com.orioljt.userservice.application.dto.UserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class UserControllerIT extends IntegrationTestBase {

    @Test
    void getProfile_authenticated_returns200() {
        String token = registerAndGetToken("profile@example.com", "profileuser", "password123");

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/users/me",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode(),
                "Body: " + response.getBody());
    }

    @Test
    void getProfile_unauthenticated_returns401() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/users/me",
                HttpMethod.GET,
                new HttpEntity<>(null, null),
                String.class
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode(),
                "Body: " + response.getBody());
    }

    @Test
    void updateProfile_validRequest_returns200() {
        String token = registerAndGetToken("update@example.com", "updateuser", "password123");

        UpdateProfileRequest updateRequest = new UpdateProfileRequest(
                "UpdatedFirst", "UpdatedLast", "My new bio", null
        );

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/users/me",
                HttpMethod.PUT,
                new HttpEntity<>(updateRequest, authHeaders(token)),
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode(),
                "Body: " + response.getBody());
    }

    @Test
    void listUsers_asRegularUser_returns403() {
        String token = registerAndGetToken("regular@example.com", "regularuser", "password123");

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/users",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                String.class
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode(),
                "Body: " + response.getBody());
    }
}
