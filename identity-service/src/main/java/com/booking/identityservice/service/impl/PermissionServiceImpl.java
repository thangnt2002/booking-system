package com.booking.identityservice.service.impl;

import com.booking.identityservice.dto.ApiResponse;
import com.booking.identityservice.dto.request.PermissionCreationRequest;
import com.booking.identityservice.entity.Permission;
import com.booking.identityservice.mapper.PermissionMapper;
import com.booking.identityservice.repository.PermissionRepository;
import com.booking.identityservice.service.PermissionService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PermissionServiceImpl implements PermissionService {

    PermissionRepository permissionRepository;
    PermissionMapper permissionMapper;

    @Override
    public Permission create(PermissionCreationRequest request) {
        Permission permission = permissionMapper.toPermission(request);
        return permissionRepository.save(permission);
    }

    @Override
    public List<Permission> getAll() {
        return permissionRepository.findAll();
    }

    @Override
    public void delete(String id) {
        permissionRepository.deleteById(id);
    }
}
