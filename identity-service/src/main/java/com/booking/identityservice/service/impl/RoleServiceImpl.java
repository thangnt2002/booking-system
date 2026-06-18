package com.booking.identityservice.service.impl;

import com.booking.identityservice.dto.ApiResponse;
import com.booking.identityservice.dto.request.RoleCreationRequest;
import com.booking.identityservice.dto.response.RoleResponse;
import com.booking.identityservice.entity.Role;
import com.booking.identityservice.mapper.RoleMapper;
import com.booking.identityservice.repository.PermissionRepository;
import com.booking.identityservice.repository.RoleRepository;
import com.booking.identityservice.service.RoleService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoleServiceImpl implements RoleService {

    RoleRepository roleRepository;
    PermissionRepository permissionRepository;

    RoleMapper roleMapper;

    @Override
    public Role create(RoleCreationRequest request) {
        Role role = roleMapper.toRole(request);
        var permission = permissionRepository.findAllById(request.getPermissions());
        role.setPermissions(new HashSet<>(permission));
        return roleRepository.save(role);
    }

    @Override
    public List<Role> getAll() {
        return roleRepository.findAll();
    }

    @Override
    public void delete(String id) {
        roleRepository.deleteById(id);
    }
}
