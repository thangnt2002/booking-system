package com.booking.notificationservice.kafka;

import com.booking.notificationservice.dto.requests.SendEmail;
import com.booking.notificationservice.dto.requests.email.Recipient;
import com.booking.notificationservice.event.NotificationMsg;
import com.booking.notificationservice.services.EmailService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.booking.notificationservice.common.Constant.TOPIC_USER_OB;

@Component
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class NotificationConsumer {

    EmailService emailService;

    @KafkaListener(topics = TOPIC_USER_OB)
    public void listenNotificationDelivery(NotificationMsg message) {
        log.info("Message received: {}", message);
        emailService.sendEmail(SendEmail.builder()
                .to(List.of(Recipient.builder()
                        .email(message.getRecipient())
                        .name("Recipient name")
                        .build()))
                .subject(message.getSubject())
                .htmlContent(message.getBody())
                .build());
    }
}
