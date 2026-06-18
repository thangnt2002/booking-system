package com.booking.identityservice.dto.request;

import com.booking.identityservice.validator.DobConstraint;
import lombok.Data;

import java.time.LocalDate;
import java.util.Set;

@Data
public class UserUpdateRequest {
    String id;
    String password;
    String firstName;
    String lastName;
    @DobConstraint(min = 18, message = "INVALID_DOB")
    LocalDate dob;
    Set<String> roles;
}
