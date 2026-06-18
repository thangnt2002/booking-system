package com.booking.identityservice.dto.request;

import com.booking.identityservice.validator.DobConstraint;
import lombok.Data;

import java.time.LocalDate;
@Data
public class UserCreationRequest {
    String username;
    String password;
    String firstName;
    String lastName;
    @DobConstraint(min = 18, message = "INVALID_DOB")
    LocalDate dob;
    String avatar;
    String email;
    String city;
}
