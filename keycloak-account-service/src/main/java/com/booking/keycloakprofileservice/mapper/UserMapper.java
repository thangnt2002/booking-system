package com.booking.keycloakprofileservice.mapper;

import com.booking.keycloakprofileservice.dto.requests.UserRequest;
import com.booking.keycloakprofileservice.dto.responses.UserResponse;
import com.booking.keycloakprofileservice.entities.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    User toUser(UserRequest request);
    UserResponse toUserResponse(User user);
}