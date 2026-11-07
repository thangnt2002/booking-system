package com.booking.keycloakprofileservice.services.impl;

import com.booking.common.exceptions.BusinessException;
import com.booking.common.exceptions.ErrorCode;
import com.booking.common.exceptions.NotFoundException;
import com.booking.keycloakprofileservice.dto.requests.RoleCreationRequest;
import com.booking.keycloakprofileservice.enums.RoleName;
import jakarta.ws.rs.ClientErrorException;
import com.booking.keycloakprofileservice.entities.Role;
import com.booking.keycloakprofileservice.mapper.RoleMapper;
import com.booking.keycloakprofileservice.repositories.RoleRepository;
import com.booking.keycloakprofileservice.services.RoleService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoleServiceImpl implements RoleService {

    RoleRepository roleRepository;
    RoleMapper roleMapper;
    Keycloak keycloak;

    @NonFinal
    @Value("${spring.app-name}")
    private String appName;

    /** Roles the application logic depends on (registration, seeding); deleting them would break it. */
    private static final Set<RoleName> BUILT_IN = EnumSet.of(RoleName.USER, RoleName.ADMIN);

    @Override
    @Transactional
    public Role create(RoleCreationRequest request) {
        RoleName name = parseName(request.getName());
        if (roleRepository.existsByName(name)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        Role role = roleMapper.toRole(request);
        role.setName(name);
        role = roleRepository.save(role);   // DB first: a DB failure leaves Keycloak untouched
        createKeyCloakRole(role);           // a Keycloak failure rolls the DB insert back
        return role;
    }

    private void createKeyCloakRole(Role role) {
        RoleRepresentation roleKeycloak = new RoleRepresentation();
        roleKeycloak.setName(role.getName().name());
        roleKeycloak.setDescription(role.getDescription());
        try {
            keycloak.realm(appName).roles().create(roleKeycloak);
        } catch (ClientErrorException e) {
            if (e.getResponse().getStatus() != 409) throw e;   // already in Keycloak: just link it
            log.info("[Role] {} already exists in Keycloak", role.getName());
        }
    }

    @Override
    public List<Role> getAll() {
        return roleRepository.findAll();
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorCode.DATA_NOT_FOUND));
        if (BUILT_IN.contains(role.getName())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        roleRepository.delete(role);
        try {
            keycloak.realm(appName).roles().deleteRole(role.getName().name());
        } catch (jakarta.ws.rs.NotFoundException e) {
            log.warn("[Role] {} was not in Keycloak", role.getName());
        }
    }

    private static RoleName parseName(String raw) {
        try {
            return RoleName.valueOf(raw == null ? "" : raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }
}
