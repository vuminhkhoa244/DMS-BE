package com.example.DocumentManagement.config;

import com.example.DocumentManagement.entity.Role;
import com.example.DocumentManagement.entity.User;
import com.example.DocumentManagement.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!userRepository.existsByEmail("admin@dm.com")) {
            User admin = new User();
            admin.setEmail("admin@dm.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setFullName("System Administrator");
            admin.setRole(Role.ADMIN);
            userRepository.save(admin);
            System.out.println(">>> Default admin account created: admin@dm.com / admin123");
        }
    }
}
