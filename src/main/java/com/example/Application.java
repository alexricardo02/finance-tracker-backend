package com.example;


import org.springframework.boot.SpringApplication;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = { SecurityAutoConfiguration.class }, 
scanBasePackages = {
        "com.example.controllers", 
        "com.example.service",
        "com.example.dataTransferObjects",
        "com.example.models",
        "com.example.repository",
        "com.example.config",
    })
@ComponentScan(basePackages = {"com.example"})
@EnableCaching
@EnableJpaAuditing
@EnableScheduling   
@EnableAsync
public class Application {
	public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
