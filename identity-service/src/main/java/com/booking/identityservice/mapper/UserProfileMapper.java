package com.booking.identityservice.mapper;

import com.booking.identityservice.dto.request.UserCreationRequest;
import com.booking.identityservice.dto.request.UserProfileCreationRequest;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserProfileMapper {
    UserProfileCreationRequest toUserProfileRequest(UserCreationRequest request);

}
