package com.orioljt.userservice.application.service;

import com.orioljt.userservice.application.dto.AuthResponse;
import com.orioljt.userservice.application.dto.LoginRequest;
import com.orioljt.userservice.application.dto.RegisterRequest;
import com.orioljt.userservice.domain.exception.DuplicateResourceException;
import com.orioljt.userservice.domain.model.Role;
import com.orioljt.userservice.domain.model.User;
import com.orioljt.userservice.domain.repository.RoleRepository;
import com.orioljt.userservice.domain.repository.UserRepository;
import com.orioljt.userservice.infrastructure.security.JwtProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_success() {
        RegisterRequest request = new RegisterRequest(
                "new@example.com", "newuser", "password123", "New", "User"
        );
        Role userRole = Role.builder().id(1L).name("ROLE_USER").build();
        User savedUser = User.builder()
                .id(UUID.randomUUID())
                .email("new@example.com")
                .username("newuser")
                .passwordHash("encoded-password")
                .firstName("New")
                .lastName("User")
                .enabled(true)
                .roles(Set.of(userRole))
                .build();

        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtProvider.generateAccessToken(savedUser)).thenReturn("access-token");
        when(jwtProvider.generateRefreshToken(savedUser)).thenReturn("refresh-token");
        when(jwtProvider.getAccessExpirationSeconds()).thenReturn(3600L);

        AuthResponse result = authService.register(request);

        assertNotNull(result);
        assertEquals("access-token", result.accessToken());
        assertEquals("refresh-token", result.refreshToken());
        assertEquals("Bearer", result.tokenType());
        assertEquals(3600L, result.expiresIn());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_duplicateEmail_throwsDuplicateResource() {
        RegisterRequest request = new RegisterRequest(
                "existing@example.com", "newuser", "password123", "New", "User"
        );

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_duplicateUsername_throwsDuplicateResource() {
        RegisterRequest request = new RegisterRequest(
                "new@example.com", "existinguser", "password123", "New", "User"
        );

        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_success() {
        LoginRequest request = new LoginRequest("test@example.com", "password123");
        Role userRole = Role.builder().id(1L).name("ROLE_USER").build();
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .username("testuser")
                .passwordHash("encoded-password")
                .enabled(true)
                .roles(Set.of(userRole))
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true);
        when(jwtProvider.generateAccessToken(user)).thenReturn("access-token");
        when(jwtProvider.generateRefreshToken(user)).thenReturn("refresh-token");
        when(jwtProvider.getAccessExpirationSeconds()).thenReturn(3600L);

        AuthResponse result = authService.login(request);

        assertNotNull(result);
        assertEquals("access-token", result.accessToken());
        assertEquals("refresh-token", result.refreshToken());
        verify(userRepository).findByEmail("test@example.com");
        verify(passwordEncoder).matches("password123", "encoded-password");
    }

    @Test
    void login_invalidPassword_throwsBadCredentials() {
        LoginRequest request = new LoginRequest("test@example.com", "wrong-password");
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .username("testuser")
                .passwordHash("encoded-password")
                .enabled(true)
                .roles(Set.of())
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
        verify(jwtProvider, never()).generateAccessToken(any());
    }

    @Test
    void login_userNotFound_throwsBadCredentials() {
        LoginRequest request = new LoginRequest("nonexistent@example.com", "password123");

        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtProvider, never()).generateAccessToken(any());
    }
}
