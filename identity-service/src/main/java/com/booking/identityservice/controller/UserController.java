package com.booking.identityservice.controller;

import com.booking.identityservice.dto.ApiResponse;
import com.booking.identityservice.dto.request.UserCreationRequest;
import com.booking.identityservice.dto.request.UserUpdateRequest;
import com.booking.identityservice.dto.response.UserCreationResponse;
import com.booking.identityservice.dto.response.UserSearchResponse;
import com.booking.identityservice.entity.User;
import com.booking.identityservice.mapper.UserMapper;
import com.booking.identityservice.service.UserService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserController {
    UserService userService;
    UserMapper userMapper;

    @PostMapping("/registrations")
    public ResponseEntity<ApiResponse<UserCreationResponse>> create(@RequestBody @Valid UserCreationRequest request){
        User user = userService.create(request);
        ApiResponse<UserCreationResponse> body = ApiResponse.<UserCreationResponse>builder()
                .success(true)
                .code(201)
                .data(userMapper.toUserResponse(user))
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @GetMapping("/{id}")
    private ApiResponse<UserSearchResponse> findById(@PathVariable String id){
        User user = userService.findById(id);
        return ApiResponse.<UserSearchResponse>builder()
                .success(true)
                .code(200)
                .data(userMapper.toUserSearchResponse(user))
                .build();
    }

    @PutMapping
    private ApiResponse<UserCreationResponse> update(@RequestBody UserUpdateRequest request){
        User user = userService.update(request);
        return ApiResponse.<UserCreationResponse>builder()
                .success(true)
                .code(200)
                .data(userMapper.toUserResponse(user))
                .build();
    }

}
