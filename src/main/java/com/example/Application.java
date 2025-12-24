package com.example;


import org.springframework.boot.SpringApplication;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

@SpringBootApplication(exclude = { SecurityAutoConfiguration.class }, 
scanBasePackages = {
        "com.example.controllers", 
        "com.example.service",
        "com.example.dataTransferObjects",
        "com.example.models",
        "com.example.repository",
        "com.example.config",
    })
public class Application {
	public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
