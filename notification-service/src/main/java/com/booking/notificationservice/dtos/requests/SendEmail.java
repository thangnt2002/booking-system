package com.booking.notificationservice.dtos.requests;

import com.booking.notificationservice.dtos.requests.email.Recipient;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class SendEmail {
    String htmlContent;
    String subject;
    List<Recipient> to;
}
