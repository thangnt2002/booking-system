package com.booking.keycloakprofileservice.services.impl;

import com.booking.keycloakprofileservice.dto.requests.UserRequest;
import com.booking.keycloakprofileservice.entities.Role;
import com.booking.keycloakprofileservice.entities.User;
import com.booking.keycloakprofileservice.enums.RoleName;
import com.booking.keycloakprofileservice.event.NotificationEvent;
import com.booking.common.exceptions.BusinessException;
import com.booking.common.exceptions.ErrorCode;
import com.booking.common.exceptions.NotFoundException;
import com.booking.keycloakprofileservice.kafka.producer.NotificationProducer;
import com.booking.keycloakprofileservice.mapper.UserMapper;
import com.booking.keycloakprofileservice.repositories.RoleRepository;
import com.booking.keycloakprofileservice.repositories.UserRepository;
import com.booking.keycloakprofileservice.services.UserService;
import jakarta.ws.rs.core.Response;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.booking.keycloakprofileservice.common.Constant.TOPIC_USER_OB;

@Slf4j
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    UserRepository userRepository;
    UserMapper userMapper;
    NotificationProducer notificationProducer;
    Keycloak keycloak;
    RoleRepository roleRepository;

    @NonFinal
    @Value("${spring.app-name}")
    private String appName;

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public User create(UserRequest request) {
        try {
            User user = userMapper.toUser(request);
            user = userRepository.save(user);
            List<Role> roles = new ArrayList<>();

            roleRepository.findByName(RoleName.USER).ifPresent(roles::add);
            createKeyCloakUser(request, roles);

            NotificationEvent notificationEvent = NotificationEvent
                    .builder()
                    .subject("Subject")
                    .channel("EMAIL")
                    .recipient(request.getEmail())
                    .body("Body for test")
                    .build();

            notificationProducer.sendNotificationEvent(TOPIC_USER_OB, notificationEvent);

            return user;
        } catch (Exception e){
            log.error("[Create profile] Failed to create profile", e);
            throw new BusinessException(ErrorCode.SERVER_ERROR.getCode());
        }
    }

    private void createKeyCloakUser(UserRequest user, List<Role> roles) {
        UserRepresentation keycloakUser = new UserRepresentation();
        keycloakUser.setUsername(user.getUsername());
        keycloakUser.setFirstName(user.getFirstName());
        keycloakUser.setLastName(user.getLastName());
        keycloakUser.setEmail(user.getEmail());
        keycloakUser.setEnabled(true);
        String createdUserId = null;

        try (Response response = keycloak.realm(appName).users().create(keycloakUser)) {

            // setPass
            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(user.getPassword());
            credential.setTemporary(false);

            createdUserId = response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");

            keycloak.realm(appName).users().get(createdUserId).resetPassword(credential);

            // setRole
            UserResource userResource = keycloak.realm(appName).users().get(createdUserId);
            RoleMappingResource roleMapping = userResource.roles();
            List<RoleRepresentation> currentRoles = roleMapping.realmLevel().listEffective();
            List<RoleRepresentation> newRoles = this.buildKeycloakRoleByRoleName(appName, roles);

            List<RoleRepresentation> rolesToRemove = currentRoles.stream()
                    .filter(existing -> newRoles.stream()
                            .noneMatch(newR -> newR.getName().equals(existing.getName())))
                    .toList();

            List<RoleRepresentation> rolesToAdd = newRoles.stream()
                    .filter(newR -> currentRoles.stream()
                            .noneMatch(existing -> existing.getName().equals(newR.getName())))
                    .toList();

            if (!rolesToRemove.isEmpty()) {
                roleMapping.realmLevel().remove(rolesToRemove);
            }

            if (!rolesToAdd.isEmpty()) {
                roleMapping.realmLevel().add(rolesToAdd);
            }

            assignAccountClientRoles(appName, createdUserId, List.of("manage-account", "view-profile"));
        }
    }

    private List<RoleRepresentation> buildKeycloakRoleByRoleName(String realm, List<Role> roles) {
        List<RoleRepresentation> newRoles = new ArrayList<>();
        if (!CollectionUtils.isEmpty(roles)) {
            for (Role role : roles) {
                RoleRepresentation keycloakRole = keycloak.realm(realm).roles().get(String.valueOf(role.getName())).toRepresentation();
                newRoles.add(keycloakRole);
            }
        }
        return newRoles;
    }

    private void assignAccountClientRoles(String realm, String userId, List<String> roleNames) {
        RealmResource realmResource = keycloak.realm(realm);
        UserResource userResource = realmResource.users().get(userId);

        // find the built-in "account" client in this realm
        List<ClientRepresentation> clients = realmResource.clients().findByClientId("account");
        if (clients == null || clients.isEmpty()) {
            throw new IllegalStateException("Client 'account' not found in realm: " + realm);
        }

        ClientRepresentation accountClient = clients.get(0);
        String accountClientUuid = accountClient.getId();

        RoleScopeResource clientRoleScope =
                userResource.roles().clientLevel(accountClientUuid);

        // current direct client roles
        List<RoleRepresentation> currentClientRoles = clientRoleScope.listAll();

        // desired roles
        List<RoleRepresentation> targetRoles = roleNames.stream()
                .map(roleName -> realmResource.clients()
                        .get(accountClientUuid)
                        .roles()
                        .get(roleName)
                        .toRepresentation())
                .toList();

        List<RoleRepresentation> rolesToAdd = targetRoles.stream()
                .filter(target -> currentClientRoles.stream()
                        .noneMatch(existing -> existing.getName().equals(target.getName())))
                .toList();

        if (!rolesToAdd.isEmpty()) {
            clientRoleScope.add(rolesToAdd);
        }
    }


    @Override
    public User update(UserRequest request) {
        return null;
    }

    @Override
    public User findById(UUID id) {
        return userRepository.findById(id).orElseThrow(()-> new NotFoundException(ErrorCode.USER_NOT_FOUND.getCode()));
    }

    @Override
    public List<User> getAll() {
        return userRepository.findAll();
    }
}
