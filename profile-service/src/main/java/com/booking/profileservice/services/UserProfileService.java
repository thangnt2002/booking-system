package com.booking.profileservice.services;

import com.booking.profileservice.dto.ApiResponse;
import com.booking.profileservice.dto.requests.UserProfileRequest;
import com.booking.profileservice.dto.responses.UserProfileResponse;
import com.booking.profileservice.entities.UserProfile;

import java.util.List;

public interface UserProfileService {

    ApiResponse<UserProfileResponse> create(UserProfileRequest request);

    ApiResponse<UserProfileResponse> update(UserProfileRequest request);

    UserProfile findById(String id);

    List<UserProfile> getAll();
}
