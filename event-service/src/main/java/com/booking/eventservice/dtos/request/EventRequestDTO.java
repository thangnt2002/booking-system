package com.booking.eventservice.dtos.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventRequestDTO {

    String name;

    String description;

    Instant startTime;

    Instant endTime;

    String location;

    String poster;
}
