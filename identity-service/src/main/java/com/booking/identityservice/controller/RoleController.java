package com.booking.identityservice.controller;

import com.booking.identityservice.dto.ApiResponse;
import com.booking.identityservice.dto.request.RoleCreationRequest;
import com.booking.identityservice.dto.response.RoleResponse;
import com.booking.identityservice.entity.Role;
import com.booking.identityservice.mapper.RoleMapper;
import com.booking.identityservice.service.RoleService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    private ApiResponse<Void> delete(@PathVariable String id) {
        roleService.delete(id);
        return ApiResponse.<Void>builder()
                .success(true)
                .code(200)
                .data(null)
                .build();
    }

}
