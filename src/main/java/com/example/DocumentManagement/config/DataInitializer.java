package com.example.DocumentManagement.config;

import com.example.DocumentManagement.entity.Role;
import com.example.DocumentManagement.entity.User;
import com.example.DocumentManagement.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!userRepository.existsByEmail("admin@dm.com")) {
            User admin = new User();
            admin.setEmail("admin@dm.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setFullName("System Administrator");
            admin.setRole(Role.ADMIN);
            userRepository.save(admin);
            log.info(">>> Default admin account created: admin@dm.com / admin123");
        }

        if (!userRepository.existsByEmail("manager@dm.com")) {
            User manager = new User();
            manager.setEmail("manager@dm.com");
            manager.setPassword(passwordEncoder.encode("manager123"));
            manager.setFullName("Sample Manager");
            manager.setRole(Role.MANAGER);
            userRepository.save(manager);
            log.info(">>> Default manager account created: manager@dm.com / manager123");
        }
    }
}
