package com.booking.keycloakprofileservice.controllers;

import com.booking.keycloakprofileservice.dto.ApiResponse;
import com.booking.keycloakprofileservice.dto.requests.UserRequest;
import com.booking.keycloakprofileservice.dto.responses.UserResponse;
import com.booking.keycloakprofileservice.entities.User;
import com.booking.keycloakprofileservice.mapper.UserMapper;
import com.booking.keycloakprofileservice.services.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/profiles")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class UserProfileController
{
    UserService userService;

    UserMapper userMapper;


    @PostMapping()
    private ApiResponse<UserResponse> create(@RequestBody UserRequest request){
        User profile = userService.create(request);
        return ApiResponse.<UserResponse>builder()
                .success(true)
                .code(201)
                .data(userMapper.toUserResponse(profile))
                .build();
    }

    @PutMapping
    private ApiResponse<UserResponse> update(@RequestBody UserRequest request){
        User profile = userService.update(request);
        return ApiResponse.<UserResponse>builder()
                .success(true)
                .code(200)
                .data(userMapper.toUserResponse(profile))
                .build();
    }

    @GetMapping("/{id}")
    private ApiResponse<UserResponse> findById(@PathVariable UUID id){
        User user = userService.findById(id);
        return ApiResponse.<UserResponse>builder()
                .success(true)
                .code(200)
                .data(userMapper.toUserResponse(user))
                .build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
     ApiResponse<List<UserResponse>> getAll(){

        List<UserResponse> response =
                userService.getAll()
                .stream()
                .map(userMapper::toUserResponse)
                .toList();

        return ApiResponse.<List<UserResponse>>builder()
                .success(true)
                .code(200)
                .data(response)
                .build();
    }
}
