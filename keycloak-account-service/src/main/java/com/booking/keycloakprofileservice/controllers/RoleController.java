package com.booking.keycloakprofileservice.controllers;

import com.booking.keycloakprofileservice.dto.ApiResponse;
import com.booking.keycloakprofileservice.dto.requests.RoleCreationRequest;
import com.booking.keycloakprofileservice.dto.responses.RoleResponse;
import com.booking.keycloakprofileservice.entities.Role;
import com.booking.keycloakprofileservice.mapper.RoleMapper;
import com.booking.keycloakprofileservice.services.RoleService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoleController {

    RoleService roleService;
    RoleMapper roleMapper;

    @PostMapping
    private ResponseEntity<ApiResponse<RoleResponse>> create(@RequestBody RoleCreationRequest request) {
        Role role = roleService.create(request);

        ApiResponse<RoleResponse> body = ApiResponse.<RoleResponse>builder()
                .success(true)
                .code(201)
                .data(roleMapper.toRoleResponse(role))
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @GetMapping
    private ApiResponse<List<RoleResponse>> getAll() {
        List<RoleResponse> responses =
                roleService.getAll()
                        .stream()
                        .map(roleMapper::toRoleResponse).toList();

        return ApiResponse.<List<RoleResponse>>builder()
                .success(true)
                .code(200)
                .data(responses)
                .build();
    }

    @DeleteMapping("/{id}")
    private ApiResponse<Void> delete(@PathVariable UUID id) {
        roleService.delete(id);
        return ApiResponse.<Void>builder()
                .success(true)
                .code(200)
                .data(null)
                .build();
    }

}
