package com.orioljt.userservice.application.service;

import com.orioljt.userservice.application.dto.UpdateProfileRequest;
import com.orioljt.userservice.application.dto.UserResponse;
import com.orioljt.userservice.application.mapper.UserMapper;
import com.orioljt.userservice.domain.exception.ResourceNotFoundException;
import com.orioljt.userservice.domain.model.User;
import com.orioljt.userservice.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Cacheable(value = "users", key = "#id")
    @Transactional(readOnly = true)
    public UserResponse findById(UUID id) {
        log.debug("Finding user by id: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        return userMapper.toResponse(user);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> findAll(Pageable pageable) {
        log.debug("Finding all users with pageable: {}", pageable);

        return userRepository.findAll(pageable)
                .map(userMapper::toResponse);
    }

    @CacheEvict(value = "users", key = "#id")
    @Transactional
    public UserResponse updateProfile(UUID id, UpdateProfileRequest request) {
        log.info("Updating profile for user: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        if (request.firstName() != null) {
            user.setFirstName(request.firstName());
        }
        if (request.lastName() != null) {
            user.setLastName(request.lastName());
        }
        if (request.bio() != null) {
            user.setBio(request.bio());
        }
        if (request.avatarUrl() != null) {
            user.setAvatarUrl(request.avatarUrl());
        }

        user = userRepository.save(user);
        log.info("Profile updated for user: {}", id);

        return userMapper.toResponse(user);
    }

    @CacheEvict(value = "users", key = "#id")
    @Transactional
    public void delete(UUID id) {
        log.info("Deleting user: {}", id);

        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User", "id", id);
        }

        userRepository.deleteById(id);
        log.info("User deleted: {}", id);
    }
}
