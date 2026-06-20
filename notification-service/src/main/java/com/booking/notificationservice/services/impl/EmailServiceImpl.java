package com.booking.notificationservice.services.impl;

import com.booking.notificationservice.dto.requests.SendEmail;
import com.booking.notificationservice.dto.requests.email.EmailRequest;
import com.booking.notificationservice.dto.requests.email.Sender;
import com.booking.notificationservice.dto.responses.EmailResponse;
import com.booking.notificationservice.repositories.httpclient.EmailClient;
import com.booking.notificationservice.services.EmailService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class EmailServiceImpl implements EmailService {

    EmailClient emailClient;

    @NonFinal
    String apiKey ="key";

    @Override
    public String sendEmail(SendEmail request) {
        EmailRequest emailBody = EmailRequest
                .builder()
                .sender(Sender.builder()
                        .name("ThangNT")
                        .email("nguyenthienthang2002@gmail.com")
                        .build())
                .to(request.getTo())
                .subject(request.getSubject())
                .htmlContent(request.getHtmlContent())
                .build();
        try {
            EmailResponse emailResponse;
            emailResponse = emailClient.sendEmail(apiKey, emailBody);
            return emailResponse.getMessageId();
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }
}


