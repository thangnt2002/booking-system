package com.booking.identityservice.mapper;

import com.booking.identityservice.dto.request.PermissionCreationRequest;
import com.booking.identityservice.dto.response.PermissionResponse;
import com.booking.identityservice.entity.Permission;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PermissionMapper {
    Permission toPermission(PermissionCreationRequest request);
    PermissionResponse toPermissionResponse(Permission permission);
}
