package com.booking.identityservice.dto.response;

import com.booking.identityservice.entity.Permission;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.Set;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoleResponse
{
    String name;
    String description;
    Set<Permission> permissions;
}
