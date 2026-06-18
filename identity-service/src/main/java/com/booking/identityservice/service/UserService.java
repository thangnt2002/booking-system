package com.booking.identityservice.service;

import com.booking.identityservice.dto.ApiResponse;
import com.booking.identityservice.dto.request.UserCreationRequest;
import com.booking.identityservice.dto.request.UserUpdateRequest;
import com.booking.identityservice.dto.response.UserCreationResponse;
import com.booking.identityservice.entity.User;

public interface UserService {

    User create(UserCreationRequest userCreationRequest);

    User findById(String id);

    User update(UserUpdateRequest userUpdateRequest);

    ApiResponse<UserCreationResponse> getAll();


}
