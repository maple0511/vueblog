package com.campusblog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class CampusBlogApplication {
    public static void main(String[] args) {
        SpringApplication.run(CampusBlogApplication.class, args);
    }
}
