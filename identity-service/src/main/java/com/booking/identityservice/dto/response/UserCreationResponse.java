package com.booking.identityservice.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserCreationResponse {
    String id;
    String firstName;
    String lastName;
    LocalDate dob;
    Set<RoleResponse> roles;
}
