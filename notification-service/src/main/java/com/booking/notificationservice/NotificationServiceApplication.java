package com.booking.notificationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

// notification-service does not use Redis: skip common's Redis/Redisson beans
// (Redis auto-configuration is excluded in application.yaml)
@ComponentScan(
        basePackages = {"com.booking"},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = {
                        "com\\.booking\\.common\\.distributed\\..*",
                        "com\\.booking\\.common\\.configurations\\.Redis(son)?Config(uration)?"
                }))
@EntityScan(basePackages = {"com.booking"})
@SpringBootApplication
@EnableFeignClients
@EnableDiscoveryClient
public class NotificationServiceApplication {

    static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }

}
