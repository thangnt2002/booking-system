package com.booking.identityservice.controller;

import com.booking.identityservice.dto.ApiResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminController {

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin-data")
    private ApiResponse<Object> login(){
        return ApiResponse.builder()
                .code(200)
                .success(true)
                .data("Admin ne")
                .build();
    }
}
