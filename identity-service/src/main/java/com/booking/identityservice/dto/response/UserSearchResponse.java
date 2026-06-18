package com.booking.identityservice.dto.response;

import lombok.Data;

import java.time.LocalDate;
import java.util.Set;

@Data
public class UserSearchResponse {
    String id;
    String userName;
    String password;
    String firstName;
    String lastName;
    LocalDate dob;
    Set<RoleResponse> roles;
}
