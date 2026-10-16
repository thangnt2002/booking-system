package com.booking.keycloakprofileservice.services.impl;

import com.booking.keycloakprofileservice.dto.requests.RoleCreationRequest;
import com.booking.keycloakprofileservice.entities.Role;
import com.booking.keycloakprofileservice.mapper.RoleMapper;
import com.booking.keycloakprofileservice.repositories.RoleRepository;
import com.booking.keycloakprofileservice.services.RoleService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

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

    @Override
    @Transactional
    public Role create(RoleCreationRequest request) {
        Role role = roleMapper.toRole(request);
        createKeyCloakRole(role);
        return roleRepository.save(role);
    }

    private void createKeyCloakRole(Role role) {
        RoleRepresentation roleKeycloak = new RoleRepresentation();
        roleKeycloak.setName(String.valueOf(role.getName()));
        roleKeycloak.setDescription(role.getDescription());
        keycloak.realm(appName).roles().create(roleKeycloak);
    }


    @Override
    public List<Role> getAll() {
        return roleRepository.findAll();
    }

    @Override
    public void delete(UUID id) {
        roleRepository.deleteById(id);
    }
}
