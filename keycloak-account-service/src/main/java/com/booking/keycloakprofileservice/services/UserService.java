package com.booking.keycloakprofileservice.services;

import com.booking.keycloakprofileservice.dto.requests.UserRequest;
import com.booking.keycloakprofileservice.entities.User;

import java.util.List;
import java.util.UUID;

public interface UserService {

    User create(UserRequest request);

    User update(UserRequest request);

    User findById(UUID id);

    List<User> getAll();
}
