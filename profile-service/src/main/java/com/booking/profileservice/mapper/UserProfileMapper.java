package com.booking.profileservice.mapper;

import com.booking.profileservice.dto.requests.UserProfileRequest;
import com.booking.profileservice.dto.responses.UserProfileResponse;
import com.booking.profileservice.entities.UserProfile;
import com.booking.profileservice.event.ProfileRevertEvent;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserProfileMapper {
    UserProfile toUserProfile(UserProfileRequest request);
    UserProfileResponse toUserProfileResponse(UserProfile userProfile);
    ProfileRevertEvent toProfileRevertEvent(UserProfileRequest userProfile);

}