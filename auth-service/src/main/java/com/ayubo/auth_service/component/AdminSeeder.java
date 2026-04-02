package com.ayubo.auth_service.component;

import com.ayubo.auth_service.model.User;
import com.ayubo.auth_service.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminSeeder implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        String adminEmail = "admin@ayubo.com";

        // Check if the admin already exists
        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            User admin = new User();
            admin.setFirstName("System");
            admin.setLastName("Admin");
            admin.setEmail(adminEmail);

            // Hash the password
            admin.setPassword(passwordEncoder.encode("Admin@1234"));

            // FIX: Pass the role as a plain String instead of an Enum!
            admin.setRole("ADMIN");
            admin.setPhone("+94000000000");

            userRepository.save(admin);
            System.out.println("✅ DEFAULT ADMIN ACCOUNT CREATED SUCCESSFULLY!");
        }
    }
}