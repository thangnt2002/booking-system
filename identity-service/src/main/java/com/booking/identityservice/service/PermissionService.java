package com.booking.identityservice.service;

import com.booking.identityservice.dto.request.PermissionCreationRequest;
import com.booking.identityservice.entity.Permission;

import java.util.List;

public interface PermissionService {

    Permission create(PermissionCreationRequest request);

    List<Permission> getAll();

    void delete(String id);
}
