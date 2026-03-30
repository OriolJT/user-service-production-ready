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
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user with email: {}", request.email());

        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email already in use: " + request.email());
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("Username already taken: " + request.username());
        }

        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new IllegalStateException("Default role ROLE_USER not found"));

        User user = User.builder()
                .email(request.email())
                .username(request.username())
                .passwordHash(passwordEncoder.encode(request.password()))
                .firstName(request.firstName())
                .lastName(request.lastName())
                .enabled(true)
                .roles(Set.of(userRole))
                .build();

        user = userRepository.save(user);
        log.info("User registered successfully with id: {}", user.getId());

        return generateAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.email());

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        if (!user.isEnabled()) {
            throw new DisabledException("User account is disabled");
        }

        log.info("User logged in successfully: {}", user.getId());
        return generateAuthResponse(user);
    }

    public AuthResponse refresh(String refreshToken) {
        log.debug("Refreshing token");

        if (!jwtProvider.validateToken(refreshToken)) {
            throw new BadCredentialsException("Invalid or expired refresh token");
        }

        String username = jwtProvider.extractUsername(refreshToken);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("User not found for token"));

        log.debug("Token refreshed for user: {}", user.getId());
        return generateAuthResponse(user);
    }

    private AuthResponse generateAuthResponse(User user) {
        String accessToken = jwtProvider.generateAccessToken(user);
        String newRefreshToken = jwtProvider.generateRefreshToken(user);
        long expiresIn = jwtProvider.getAccessExpirationSeconds();

        return new AuthResponse(accessToken, newRefreshToken, "Bearer", expiresIn);
    }
}
