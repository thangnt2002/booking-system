package com.booking.eventservice.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventRequestDTO {

    String name;

    String description;

    LocalDateTime startTime;

    LocalDateTime endTime;

    String location;

    String poster;
}
