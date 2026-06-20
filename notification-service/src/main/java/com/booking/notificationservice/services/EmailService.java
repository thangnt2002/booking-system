package com.booking.notificationservice.services;

import com.booking.notificationservice.dto.requests.SendEmail;

public interface EmailService {
    String sendEmail(SendEmail request);
}
