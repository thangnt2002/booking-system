package com.booking.identityservice.mapper;

import com.booking.identityservice.dto.request.RoleCreationRequest;
import com.booking.identityservice.dto.response.RoleResponse;
import com.booking.identityservice.entity.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RoleMapper {
    @Mapping(target = "permissions", ignore = true)
    Role toRole(RoleCreationRequest request);
    RoleResponse toRoleResponse(Role role);
}
