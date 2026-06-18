package com.booking.identityservice.controller;

import com.booking.identityservice.dto.ApiResponse;
import com.booking.identityservice.dto.request.PermissionCreationRequest;
import com.booking.identityservice.dto.response.PermissionResponse;
import com.booking.identityservice.dto.response.RoleResponse;
import com.booking.identityservice.entity.Permission;
import com.booking.identityservice.mapper.PermissionMapper;
import com.booking.identityservice.service.PermissionService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/permissions")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PermissionController {

    PermissionService permissionService;
    PermissionMapper permissionMapper;

    @PostMapping
    private ResponseEntity<ApiResponse<PermissionResponse>> create(@RequestBody PermissionCreationRequest request){
        Permission permission = permissionService.create(request);

        ApiResponse<PermissionResponse> body = ApiResponse.<PermissionResponse>builder()
                .success(true)
                .code(201)
                .data(permissionMapper.toPermissionResponse(permission))
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @GetMapping
    private ApiResponse<List<PermissionResponse>> getAll(){
        List<PermissionResponse> responses =
                permissionService.getAll()
                        .stream()
                        .map(permissionMapper::toPermissionResponse).toList();
        return ApiResponse.<List<PermissionResponse>>builder()
                .success(true)
                .code(200)
                .data(responses)
                .build();
    }

    @DeleteMapping("/{id}")
    private ApiResponse<Void> delete(@PathVariable String id){
        permissionService.delete(id);
        return ApiResponse.<Void>builder()
                .success(true)
                .code(200)
                .data(null)
                .build();
    }

}
