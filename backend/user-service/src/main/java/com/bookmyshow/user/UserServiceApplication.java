package com.bookmyshow.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * User Service Application - handles authentication, authorization,
 * and user profile management for BookMyShow.
 */
@SpringBootApplication(scanBasePackages = {"com.bookmyshow.user", "com.bookmyshow.shared"})
@EnableJpaAuditing
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
