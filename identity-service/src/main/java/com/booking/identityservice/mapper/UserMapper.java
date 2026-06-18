package com.booking.identityservice.mapper;

import com.booking.identityservice.dto.request.UserCreationRequest;
import com.booking.identityservice.dto.request.UserUpdateRequest;
import com.booking.identityservice.dto.response.UserCreationResponse;
import com.booking.identityservice.dto.response.UserSearchResponse;
import com.booking.identityservice.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    User toUser(UserCreationRequest userCreationRequest);
    UserCreationResponse toUserResponse(User user);
    UserSearchResponse toUserSearchResponse(User user);
    @Mapping(target = "roles", ignore = true)
    User toUser(UserUpdateRequest userUpdateRequest);
}
