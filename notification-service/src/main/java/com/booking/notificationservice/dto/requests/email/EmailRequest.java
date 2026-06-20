package com.booking.notificationservice.dto.requests.email;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class EmailRequest {
    String htmlContent;
    String subject;
    Sender sender;
    List<Recipient> to;
}
