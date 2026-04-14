package com.ayubo.telemedicine_service;

import com.ayubo.telemedicine_service.config.TelemedicineSecurityProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(TelemedicineSecurityProperties.class)
public class TelemedicineServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TelemedicineServiceApplication.class, args);
    }
}
