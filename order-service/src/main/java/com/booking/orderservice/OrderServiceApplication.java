package com.booking.orderservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@ComponentScan(basePackages={"com.booking"})
@EntityScan(basePackages = {"com.booking"})
@SpringBootApplication
@EnableFeignClients
@EnableScheduling
@EnableDiscoveryClient
public class OrderServiceApplication {

    static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }

}
