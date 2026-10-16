package com.booking.keycloakprofileservice.services;



import com.booking.keycloakprofileservice.dto.requests.RoleCreationRequest;
import com.booking.keycloakprofileservice.entities.Role;

import java.util.List;
import java.util.UUID;

public interface RoleService {

    Role create(RoleCreationRequest request);

    List<Role> getAll();

    void delete(UUID id);
}
