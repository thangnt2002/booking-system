package com.booking.keycloakprofileservice.services.impl;

import com.booking.common.exceptions.BusinessException;
import com.booking.common.exceptions.ErrorCode;
import com.booking.common.exceptions.NotFoundException;
import com.booking.common.utils.UserUtils;
import com.booking.keycloakprofileservice.dto.requests.UserRequest;
import com.booking.keycloakprofileservice.entities.Role;
import com.booking.keycloakprofileservice.entities.User;
import com.booking.keycloakprofileservice.enums.RoleName;
import com.booking.keycloakprofileservice.event.NotificationEvent;
import com.booking.keycloakprofileservice.kafka.producer.NotificationProducer;
import com.booking.keycloakprofileservice.mapper.UserMapper;
import com.booking.keycloakprofileservice.repositories.RoleRepository;
import com.booking.keycloakprofileservice.repositories.UserRepository;
import com.booking.keycloakprofileservice.services.UserService;
import jakarta.ws.rs.WebApplicationException;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.booking.keycloakprofileservice.common.Constant.TOPIC_USER_OB;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserServiceImpl implements UserService {

    UserRepository userRepository;
    UserMapper userMapper;
    NotificationProducer notificationProducer;
    Keycloak keycloak;
    RoleRepository roleRepository;

    @NonFinal
    @Value("${spring.app-name}")
    private String appName;

    // ------------------------------------------------------------------ create

    @Override
    public User create(UserRequest request) {
        return create(request, List.of(RoleName.USER));
    }

    /**
     * Keycloak first, then the local row. Keycloak is not transactional, so if anything after the
     * Keycloak user exists fails, the Keycloak user is deleted again (compensation).
     */
    @Override
    public User create(UserRequest request, Collection<RoleName> roleNames) {
        if (!StringUtils.hasText(request.getUsername()) || !StringUtils.hasText(request.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(ErrorCode.USER_EXISTED);
        }

        String keycloakId = createKeycloakUser(request);
        try {
            assignRealmRoles(keycloakId, resolveRoles(roleNames));
            assignAccountClientRoles(keycloakId, List.of("manage-account", "view-profile"));

            User user = userMapper.toUser(request);
            user.setId(null);
            user.setUserId(keycloakId);
            user = userRepository.save(user);

            sendWelcomeNotification(request);
            return user;
        } catch (RuntimeException e) {
            log.error("[Create profile] Failed after Keycloak user {} was created, rolling back", keycloakId, e);
            deleteKeycloakUserQuietly(keycloakId);
            if (e instanceof BusinessException) throw e;
            throw new BusinessException(ErrorCode.SERVER_ERROR);
        }
    }

    @Override
    @Transactional
    public User ensureUser(UserRequest request, Collection<RoleName> roleNames) {
        Optional<User> existing = userRepository.findByUsername(request.getUsername());
        if (existing.isEmpty()) {
            return create(request, roleNames);
        }
        User user = existing.get();
        if (!StringUtils.hasText(user.getUserId())) {
            String keycloakId = findKeycloakIdByUsername(request.getUsername())
                    .orElseGet(() -> createKeycloakUser(request));
            user.setUserId(keycloakId);
            user = userRepository.save(user);
            log.info("[Seed] Linked profile {} to Keycloak user {}", user.getUsername(), keycloakId);
        }
        assignRealmRoles(user.getUserId(), resolveRoles(roleNames));
        return user;
    }

    // ------------------------------------------------------------------ update / read

    /**
     * Updates the caller's own profile. An ADMIN may update someone else by passing that user's
     * Keycloak id in {@code userId}. Null/blank fields are left unchanged; username is immutable.
     * The DB change rolls back if the Keycloak update fails.
     */
    @Override
    @Transactional
    public User update(UserRequest request) {
        String currentKeycloakId = currentKeycloakId();
        String targetKeycloakId = currentKeycloakId;
        if (StringUtils.hasText(request.getUserId()) && !request.getUserId().equals(currentKeycloakId)) {
            if (!isAdmin()) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
            targetKeycloakId = request.getUserId();
        }

        User user = userRepository.findByUserId(targetKeycloakId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

        if (StringUtils.hasText(request.getAvatar())) user.setAvatar(request.getAvatar());
        if (StringUtils.hasText(request.getEmail())) user.setEmail(request.getEmail());
        if (StringUtils.hasText(request.getFirstName())) user.setFirstName(request.getFirstName());
        if (StringUtils.hasText(request.getLastName())) user.setLastName(request.getLastName());
        if (StringUtils.hasText(request.getCity())) user.setCity(request.getCity());
        if (request.getDob() != null) user.setDob(request.getDob());
        user = userRepository.save(user);

        updateKeycloakUser(targetKeycloakId, request);
        return user;
    }

    @Override
    public User getMyProfile() {
        return userRepository.findByUserId(currentKeycloakId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
    }

    @Override
    public User findById(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
    }

    @Override
    public List<User> getAll() {
        return userRepository.findAll();
    }

    // ------------------------------------------------------------------ Keycloak helpers

    private RealmResource realm() {
        return keycloak.realm(appName);
    }

    /** @return the new Keycloak user id */
    private String createKeycloakUser(UserRequest request) {
        UserRepresentation rep = new UserRepresentation();
        rep.setUsername(request.getUsername());
        rep.setFirstName(request.getFirstName());
        rep.setLastName(request.getLastName());
        rep.setEmail(request.getEmail());
        rep.setEmailVerified(StringUtils.hasText(request.getEmail()));
        rep.setEnabled(true);
        rep.setCredentials(List.of(password(request.getPassword())));

        try (Response response = realm().users().create(rep)) {
            int status = response.getStatus();
            if (status == Response.Status.CONFLICT.getStatusCode()) {
                throw new BusinessException(ErrorCode.USER_EXISTED);
            }
            if (status != Response.Status.CREATED.getStatusCode() || response.getLocation() == null) {
                log.error("[Keycloak] Create user {} failed: HTTP {} {}", request.getUsername(), status,
                        response.readEntity(String.class));
                throw new BusinessException(ErrorCode.SERVER_ERROR);
            }
            return response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");
        }
    }

    private void updateKeycloakUser(String keycloakId, UserRequest request) {
        try {
            UserResource resource = realm().users().get(keycloakId);
            UserRepresentation rep = resource.toRepresentation();
            boolean changed = false;
            if (StringUtils.hasText(request.getFirstName())) { rep.setFirstName(request.getFirstName()); changed = true; }
            if (StringUtils.hasText(request.getLastName())) { rep.setLastName(request.getLastName()); changed = true; }
            if (StringUtils.hasText(request.getEmail())) { rep.setEmail(request.getEmail()); changed = true; }
            if (changed) resource.update(rep);
            if (StringUtils.hasText(request.getPassword())) resource.resetPassword(password(request.getPassword()));
        } catch (WebApplicationException e) {
            int status = e.getResponse().getStatus();
            log.error("[Keycloak] Update user {} failed: HTTP {}", keycloakId, status, e);
            throw new BusinessException(status == 409 ? ErrorCode.USER_EXISTED : ErrorCode.INVALID_REQUEST);
        }
    }

    private Optional<String> findKeycloakIdByUsername(String username) {
        return realm().users().searchByUsername(username, true).stream().findFirst().map(UserRepresentation::getId);
    }

    private void deleteKeycloakUserQuietly(String keycloakId) {
        try (Response ignored = realm().users().delete(keycloakId)) {
            log.warn("[Keycloak] Compensated: deleted user {}", keycloakId);
        } catch (Exception ex) {
            log.error("[Keycloak] Could not delete orphan user {}", keycloakId, ex);
        }
    }

    private List<Role> resolveRoles(Collection<RoleName> names) {
        return names.stream().map(roleRepository::findByName).flatMap(Optional::stream).toList();
    }

    /** Adds the missing realm roles; never removes roles (so Keycloak-only roles such as SALE survive). */
    private void assignRealmRoles(String keycloakId, List<Role> roles) {
        RoleMappingResource mapping = realm().users().get(keycloakId).roles();
        List<String> current = mapping.realmLevel().listEffective().stream().map(RoleRepresentation::getName).toList();
        List<RoleRepresentation> toAdd = roles.stream()
                .map(r -> r.getName().name())
                .filter(name -> !current.contains(name))
                .map(name -> realm().roles().get(name).toRepresentation())
                .toList();
        if (!toAdd.isEmpty()) {
            mapping.realmLevel().add(toAdd);
        }
    }

    private void assignAccountClientRoles(String keycloakId, List<String> roleNames) {
        List<ClientRepresentation> clients = realm().clients().findByClientId("account");
        if (clients == null || clients.isEmpty()) {
            throw new IllegalStateException("Client 'account' not found in realm: " + appName);
        }
        String accountClientUuid = clients.getFirst().getId();
        RoleScopeResource scope = realm().users().get(keycloakId).roles().clientLevel(accountClientUuid);
        List<String> current = scope.listAll().stream().map(RoleRepresentation::getName).toList();
        List<RoleRepresentation> toAdd = roleNames.stream()
                .filter(name -> !current.contains(name))
                .map(name -> realm().clients().get(accountClientUuid).roles().get(name).toRepresentation())
                .toList();
        if (!toAdd.isEmpty()) {
            scope.add(toAdd);
        }
    }

    private static CredentialRepresentation password(String value) {
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(value);
        credential.setTemporary(false);
        return credential;
    }

    // ------------------------------------------------------------------ misc

    private void sendWelcomeNotification(UserRequest request) {
        if (!StringUtils.hasText(request.getEmail())) return;
        try {
            notificationProducer.sendNotificationEvent(TOPIC_USER_OB, NotificationEvent.builder()
                    .subject("Subject")
                    .channel("EMAIL")
                    .recipient(request.getEmail())
                    .body("Body for test")
                    .build());
        } catch (Exception e) {
            // the account exists already; a lost welcome email must not fail the registration
            log.error("[Create profile] Could not publish welcome notification for {}", request.getUsername(), e);
        }
    }

    private static String currentKeycloakId() {
        UUID id = UserUtils.getCurrentUserId();
        if (id == null) throw new BusinessException(ErrorCode.UN_AUTHORIZE);
        return id.toString();
    }

    private static boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}
