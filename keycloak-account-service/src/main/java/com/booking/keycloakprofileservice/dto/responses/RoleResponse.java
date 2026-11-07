package com.booking.keycloakprofileservice.dto.responses;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoleResponse
{
    UUID id;
    String name;
    String description;
}
