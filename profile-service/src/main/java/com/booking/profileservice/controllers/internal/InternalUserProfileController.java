package com.booking.profileservice.controllers.internal;

import com.booking.profileservice.dto.ApiResponse;
import com.booking.profileservice.dto.requests.UserProfileRequest;
import com.booking.profileservice.dto.responses.UserProfileResponse;
import com.booking.profileservice.services.UserProfileService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/profiles")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class InternalUserProfileController
{
    UserProfileService userProfileService;
    @PostMapping("/registration")
    private ApiResponse<UserProfileResponse> create(@RequestBody UserProfileRequest request){
        return userProfileService.create(request);
    }

    @PutMapping
    private ApiResponse<UserProfileResponse> update(@RequestBody UserProfileRequest request){
        return userProfileService.update(request);
    }

}
