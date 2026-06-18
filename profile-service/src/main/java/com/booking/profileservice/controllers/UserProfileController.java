package com.booking.profileservice.controllers;

import com.booking.profileservice.dto.ApiResponse;
import com.booking.profileservice.dto.responses.UserProfileResponse;
import com.booking.profileservice.entities.UserProfile;
import com.booking.profileservice.mapper.UserProfileMapper;
import com.booking.profileservice.services.UserProfileService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/profiles")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class UserProfileController
{
    UserProfileService userProfileService;

    UserProfileMapper userProfileMapper;

    @GetMapping("/{id}")
    private ApiResponse<UserProfileResponse> findById(@PathVariable String id){
        UserProfile userProfile = userProfileService.findById(id);
        return ApiResponse.<UserProfileResponse>builder()
                .success(true)
                .code(200)
                .data(userProfileMapper.toUserProfileResponse(userProfile))
                .build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
     ApiResponse<List<UserProfileResponse>> getAll(){

        List<UserProfileResponse> response =
                userProfileService.getAll()
                .stream()
                .map(userProfileMapper::toUserProfileResponse)
                .toList();

        return ApiResponse.<List<UserProfileResponse>>builder()
                .success(true)
                .code(200)
                .data(response)
                .build();
    }
}
