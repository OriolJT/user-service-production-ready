package com.orioljt.userservice.application.service;

import com.orioljt.userservice.application.dto.AssignPermissionsRequest;
import com.orioljt.userservice.application.dto.CreateRoleRequest;
import com.orioljt.userservice.application.dto.RoleResponse;
import com.orioljt.userservice.domain.exception.DuplicateResourceException;
import com.orioljt.userservice.domain.exception.ResourceNotFoundException;
import com.orioljt.userservice.domain.model.Permission;
import com.orioljt.userservice.domain.model.Role;
import com.orioljt.userservice.domain.repository.PermissionRepository;
import com.orioljt.userservice.domain.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleService {

    private static final Logger log = LoggerFactory.getLogger(RoleService.class);

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Transactional(readOnly = true)
    public List<RoleResponse> findAll() {
        log.debug("Finding all roles");

        return roleRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public RoleResponse create(CreateRoleRequest request) {
        log.info("Creating role: {}", request.name());

        if (roleRepository.findByName(request.name()).isPresent()) {
            throw new DuplicateResourceException("Role already exists: " + request.name());
        }

        Role role = Role.builder()
                .name(request.name())
                .description(request.description())
                .build();

        role = roleRepository.save(role);
        log.info("Role created with id: {}", role.getId());

        return toResponse(role);
    }

    @Transactional
    public RoleResponse assignPermissions(Long roleId, AssignPermissionsRequest request) {
        log.info("Assigning {} permissions to role: {}", request.permissionIds().size(), roleId);

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", roleId));

        Set<Permission> permissions = new HashSet<>(permissionRepository.findAllById(request.permissionIds()));

        if (permissions.size() != request.permissionIds().size()) {
            throw new ResourceNotFoundException("Permission", "ids", request.permissionIds());
        }

        role.setPermissions(permissions);
        role = roleRepository.save(role);
        log.info("Permissions assigned to role: {}", roleId);

        return toResponse(role);
    }

    private RoleResponse toResponse(Role role) {
        Set<String> permissionNames = role.getPermissions().stream()
                .map(Permission::getName)
                .collect(Collectors.toSet());

        return new RoleResponse(
                role.getId(),
                role.getName(),
                role.getDescription(),
                permissionNames
        );
    }
}
