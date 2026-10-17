package com.booking.notificationservice.dtos.requests.email;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class Recipient {
    String email;
    String name;
}
