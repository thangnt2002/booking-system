package com.booking.profileservice.services.impl;

import com.booking.profileservice.dto.ApiResponse;
import com.booking.profileservice.dto.requests.UserProfileRequest;
import com.booking.profileservice.dto.responses.UserProfileResponse;
import com.booking.profileservice.entities.UserProfile;
import com.booking.profileservice.exception.ErrorCode;
import com.booking.profileservice.exception.NotFoundException;
import com.booking.profileservice.mapper.UserProfileMapper;
import com.booking.profileservice.repositories.UserProfileRepository;
import com.booking.profileservice.services.UserProfileService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    UserProfileRepository userProfileRepository;
    UserProfileMapper userProfileMapper;

    @Override
    public ApiResponse<UserProfileResponse> create(UserProfileRequest request) {
        UserProfile userProfile = userProfileMapper.toUserProfile(request);
        userProfile = userProfileRepository.save(userProfile);
        return ApiResponse.<UserProfileResponse>builder()
                .success(true)
                .code(201)
                .data(userProfileMapper.toUserProfileResponse(userProfile))
                .build();
    }

    @Override
    public ApiResponse<UserProfileResponse> update(UserProfileRequest request) {
        return null;
    }

    @Override
    public UserProfile findById(String id) {
        return userProfileRepository.findById(id).orElseThrow(()-> new NotFoundException(ErrorCode.USER_NOT_FOUND.getCode()));
    }

    @Override
    public List<UserProfile> getAll() {
        return userProfileRepository.findAll();
    }
}
