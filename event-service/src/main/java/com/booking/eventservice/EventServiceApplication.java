package com.booking.eventservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class EventServiceApplication {

    static void main(String[] args) {
        SpringApplication.run(EventServiceApplication.class, args);
    }

}
