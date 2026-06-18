package com.booking.identityservice.service;

import com.booking.identityservice.dto.ApiResponse;
import com.booking.identityservice.dto.request.RoleCreationRequest;
import com.booking.identityservice.entity.Role;

import java.util.List;

public interface RoleService {

    Role create(RoleCreationRequest request);

    List<Role> getAll();

    void delete(String id);
}
