package com.booking.eventservice.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventResponseDTO {
    String id;

    String name;

    String description;

    LocalDateTime startTime;

    LocalDateTime endTime;

    String location;

    String poster;

    Long version;
}
