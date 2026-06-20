package com.booking.notificationservice.controller;

import com.booking.notificationservice.dto.ApiResponse;
import com.booking.notificationservice.dto.requests.SendEmail;
import com.booking.notificationservice.services.EmailService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EmailController {
    EmailService emailService;

    @PostMapping("/email/send")
    private ApiResponse<String> sendEmail(@RequestBody SendEmail request){
        String res = emailService.sendEmail(request);
        return ApiResponse.<String>builder()
                .success(true)
                .code(200)
                .data(res)
                .build();
    }
}
