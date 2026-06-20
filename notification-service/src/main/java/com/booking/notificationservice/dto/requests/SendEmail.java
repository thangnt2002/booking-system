package com.booking.notificationservice.dto.requests;

import com.booking.notificationservice.dto.requests.email.Recipient;
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
