package com.healthcare.appointmentservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef01",
    "appointment.dev.allow-token-endpoint=false"
})
class AppointmentServiceApplicationTests {

    @Test
    void contextLoads() {
        // Test that Spring context loads successfully
    }

}
