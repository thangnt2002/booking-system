package com.booking.keycloakprofileservice.dto.responses;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoleResponse
{
    String name;
    String description;
}
