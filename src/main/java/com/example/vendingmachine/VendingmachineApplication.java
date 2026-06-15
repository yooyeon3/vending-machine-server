package com.example.vendingmachine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

// Spring Security를 일시적으로 비활성화하는 설정입니다.
@SpringBootApplication(exclude = {SecurityAutoConfiguration.class})
public class VendingmachineApplication {

    public static void main(String[] args) {
        SpringApplication.run(VendingmachineApplication.class, args);
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}