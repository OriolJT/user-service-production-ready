package com.orioljt.userservice.application.service;

import com.orioljt.userservice.application.dto.UpdateProfileRequest;
import com.orioljt.userservice.application.dto.UserResponse;
import com.orioljt.userservice.application.mapper.UserMapper;
import com.orioljt.userservice.domain.exception.ResourceNotFoundException;
import com.orioljt.userservice.domain.model.Role;
import com.orioljt.userservice.domain.model.User;
import com.orioljt.userservice.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    @Test
    void findById_existingUser_returnsResponse() {
        UUID userId = UUID.randomUUID();
        User user = buildTestUser(userId);
        UserResponse expectedResponse = buildTestUserResponse(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(expectedResponse);

        UserResponse result = userService.findById(userId);

        assertNotNull(result);
        assertEquals(expectedResponse.id(), result.id());
        assertEquals(expectedResponse.email(), result.email());
        assertEquals(expectedResponse.username(), result.username());
        verify(userRepository).findById(userId);
        verify(userMapper).toResponse(user);
    }

    @Test
    void findById_nonExistingUser_throwsResourceNotFound() {
        UUID userId = UUID.randomUUID();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.findById(userId));
        verify(userRepository).findById(userId);
        verify(userMapper, never()).toResponse(any());
    }

    @Test
    void updateProfile_updatesNonNullFields() {
        UUID userId = UUID.randomUUID();
        User user = buildTestUser(userId);
        UpdateProfileRequest request = new UpdateProfileRequest("NewFirst", null, "New bio", null);
        UserResponse expectedResponse = new UserResponse(
                userId, "test@example.com", "testuser",
                "NewFirst", "User", "New bio", null,
                Set.of("ROLE_USER"), Instant.now()
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toResponse(any(User.class))).thenReturn(expectedResponse);

        UserResponse result = userService.updateProfile(userId, request);

        assertNotNull(result);
        assertEquals("NewFirst", result.firstName());
        assertEquals("New bio", result.bio());
        verify(userRepository).findById(userId);
        verify(userRepository).save(user);
    }

    @Test
    void delete_existingUser_deletesSuccessfully() {
        UUID userId = UUID.randomUUID();

        when(userRepository.existsById(userId)).thenReturn(true);

        userService.delete(userId);

        verify(userRepository).existsById(userId);
        verify(userRepository).deleteById(userId);
    }

    @Test
    void delete_nonExistingUser_throwsResourceNotFound() {
        UUID userId = UUID.randomUUID();

        when(userRepository.existsById(userId)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> userService.delete(userId));
        verify(userRepository).existsById(userId);
        verify(userRepository, never()).deleteById(any());
    }

    private User buildTestUser(UUID id) {
        Role role = Role.builder()
                .id(1L)
                .name("ROLE_USER")
                .description("Default user role")
                .build();

        return User.builder()
                .id(id)
                .email("test@example.com")
                .username("testuser")
                .passwordHash("hashed-password")
                .firstName("Test")
                .lastName("User")
                .enabled(true)
                .roles(Set.of(role))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private UserResponse buildTestUserResponse(UUID id) {
        return new UserResponse(
                id, "test@example.com", "testuser",
                "Test", "User", null, null,
                Set.of("ROLE_USER"), Instant.now()
        );
    }
}
