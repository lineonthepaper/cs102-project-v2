package com.smartattendance.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration class for OpenAPI/Swagger documentation.
 * Provides interactive API documentation at /swagger-ui.html
 */
@Configuration
public class OpenApiConfig {
    
    /**
     * Configures the OpenAPI documentation for the Smart Attendance API.
     * 
     * @return configured OpenAPI object
     */
    @Bean
    public OpenAPI smartAttendanceOpenAPI() {
        Server localServer = new Server();
        localServer.setUrl("http://localhost:8080");
        localServer.setDescription("Local development server");
        
        Contact contact = new Contact();
        contact.setName("Smart Attendance Team");
        contact.setEmail("support@smartattendance.com");
        
        License license = new License();
        license.setName("MIT License");
        license.setUrl("https://opensource.org/licenses/MIT");
        
        Info info = new Info()
                .title("Smart Attendance Management API")
                .version("1.0.0")
                .description("RESTful API for managing student attendance, courses, sections, and users")
                .contact(contact)
                .license(license);
        
        return new OpenAPI()
                .info(info)
                .servers(List.of(localServer));
    }
}

