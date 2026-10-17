package com.booking.eventservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan(basePackages={"com.booking"})
@EntityScan(basePackages = {"com.booking"})
@SpringBootApplication
@EnableFeignClients
@EnableDiscoveryClient
public class EventServiceApplication {

    static void main(String[] args) {
        SpringApplication.run(EventServiceApplication.class, args);
    }

}
