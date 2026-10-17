package com.booking.notificationservice.services;

import com.booking.notificationservice.dtos.requests.SendEmail;

public interface EmailService {
    String sendEmail(SendEmail request);
}
