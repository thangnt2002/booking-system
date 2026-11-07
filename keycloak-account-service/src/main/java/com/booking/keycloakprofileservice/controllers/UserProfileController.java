package com.booking.keycloakprofileservice.controllers;

import com.booking.keycloakprofileservice.dto.ApiResponse;
import com.booking.keycloakprofileservice.dto.requests.UserRequest;
import com.booking.keycloakprofileservice.dto.responses.UserResponse;
import com.booking.keycloakprofileservice.mapper.UserMapper;
import com.booking.keycloakprofileservice.services.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/*
 * Handler methods must be public: @PreAuthorize makes this bean a CGLIB proxy, and a private
 * method invoked on the proxy runs without the injected fields (NPE) and without the security check.
 */
@RestController
@RequestMapping("/profiles")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class UserProfileController {

    UserService userService;
    UserMapper userMapper;

    /** Public self sign-up (whitelisted in common SecurityConfiguration). Always gets role USER. */
    @PostMapping("/registrations")
    public ResponseEntity<ApiResponse<UserResponse>> register(@RequestBody UserRequest request) {
        return created(userMapper.toUserResponse(userService.create(request)));
    }

    /** Admin creates an account on behalf of someone. */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> create(@RequestBody UserRequest request) {
        return created(userMapper.toUserResponse(userService.create(request)));
    }

    /** Updates the caller's profile; an ADMIN may target another user via {@code userId} (Keycloak id). */
    @PutMapping
    public ApiResponse<UserResponse> update(@RequestBody UserRequest request) {
        return ok(userMapper.toUserResponse(userService.update(request)));
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> me() {
        return ok(userMapper.toUserResponse(userService.getMyProfile()));
    }

    @GetMapping("/{id}")
    public ApiResponse<UserResponse> findById(@PathVariable UUID id) {
        return ok(userMapper.toUserResponse(userService.findById(id)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ApiResponse<List<UserResponse>> getAll() {
        return ok(userService.getAll().stream().map(userMapper::toUserResponse).toList());
    }

    private static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder().success(true).code(200).data(data).build();
    }

    private static <T> ResponseEntity<ApiResponse<T>> created(T data) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<T>builder().success(true).code(201).data(data).build());
    }
}
