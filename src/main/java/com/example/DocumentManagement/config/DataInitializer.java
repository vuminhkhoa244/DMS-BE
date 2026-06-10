package com.example.DocumentManagement.config;

import com.example.DocumentManagement.entity.Role;
import com.example.DocumentManagement.entity.User;
import com.example.DocumentManagement.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final boolean adminSeedEnabled;
    private final String adminEmail;
    private final String adminPassword;
    private final String adminFullName;

    public DataInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.seed.enabled}") boolean adminSeedEnabled,
            @Value("${app.admin.seed.email}") String adminEmail,
            @Value("${app.admin.seed.password}") String adminPassword,
            @Value("${app.admin.seed.full-name}") String adminFullName) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminSeedEnabled = adminSeedEnabled;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
        this.adminFullName = adminFullName;
    }

    @Override
    public void run(String... args) {
        if (!adminSeedEnabled) {
            return;
        }

        if (!StringUtils.hasText(adminEmail) || !StringUtils.hasText(adminPassword)) {
            throw new IllegalStateException("ADMIN_SEED_EMAIL and ADMIN_SEED_PASSWORD are required when ADMIN_SEED_ENABLED=true");
        }

        if (!userRepository.existsByEmail(adminEmail)) {
            User admin = new User();
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setFullName(adminFullName);
            admin.setRole(Role.ADMIN);
            userRepository.save(admin);
            System.out.println(">>> Admin seed account created: " + adminEmail);
        }
    }
}
