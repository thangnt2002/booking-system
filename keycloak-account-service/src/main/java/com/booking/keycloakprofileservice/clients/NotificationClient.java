package com.booking.keycloakprofileservice.clients;

import com.booking.common.configurations.ClientAuthenRequestInterceptor;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(value = "booking-notification-service",
        path = "/notification",
        configuration = {ClientAuthenRequestInterceptor.class})
public interface NotificationClient {
}
