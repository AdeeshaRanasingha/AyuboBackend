package com.healthcare.appointmentservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // This maps the URL /uploads/** to the actual physical folder on your computer/docker container
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }
}