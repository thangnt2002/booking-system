package com.booking.event;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventMsg {
    String channel;
    String recipient;
    String subject;
    String body;
    String templateCode;
    Map<String, String> params;
}
