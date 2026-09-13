package com.example.backend.config;

import com.example.backend.entities.PermanentRole;
import com.example.backend.entities.User;
import com.example.backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder encoder;

    public DataLoader(UserRepository userRepository, BCryptPasswordEncoder encoder) {
        this.userRepository = userRepository;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setFullname("Admin User");
            admin.setPermanentRole(PermanentRole.ADMIN);
            admin.setActive(true);
            admin.setFailedAuthCount(0);
            admin.setPasswordHash(encoder.encode("admin123"));
            admin.setFailedPasswordChangeCount(0);

            userRepository.save(admin);

            System.out.println("✅ Seeded admin user: admin / admin123");
        }
    }
}

