package com.booking.keycloakprofileservice.services;

import com.booking.keycloakprofileservice.dto.requests.UserRequest;
import com.booking.keycloakprofileservice.entities.User;
import com.booking.keycloakprofileservice.enums.RoleName;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface UserService {

    /** Creates the Keycloak account + local profile with role USER. */
    User create(UserRequest request);

    /** Creates the Keycloak account + local profile with the given realm roles. */
    User create(UserRequest request, Collection<RoleName> roles);

    /**
     * Idempotent seeding: creates the account if missing; otherwise links the local profile to its
     * Keycloak id (when userId is empty) and makes sure the given realm roles are assigned.
     */
    User ensureUser(UserRequest request, Collection<RoleName> roles);

    User update(UserRequest request);

    User getMyProfile();

    User findById(UUID id);

    List<User> getAll();
}
