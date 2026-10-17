package com.booking.keycloakprofileservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan(basePackages={"com.booking"})
@EntityScan(basePackages = {"com.booking"})
@SpringBootApplication
public class KeycloakProfileServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(KeycloakProfileServiceApplication.class, args);
    }

}
