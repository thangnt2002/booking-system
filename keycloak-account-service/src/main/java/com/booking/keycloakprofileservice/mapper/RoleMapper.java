package com.booking.keycloakprofileservice.mapper;

import com.booking.keycloakprofileservice.dto.requests.RoleCreationRequest;
import com.booking.keycloakprofileservice.dto.responses.RoleResponse;
import com.booking.keycloakprofileservice.entities.Role;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RoleMapper {
    Role toRole(RoleCreationRequest request);
    RoleResponse toRoleResponse(Role role);
}
