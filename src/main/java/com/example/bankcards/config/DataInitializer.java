package com.example.bankcards.config;

import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.RoleRepository;
import com.example.bankcards.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Создание ролей
        if (roleRepository.findByName(Role.RoleName.ROLE_ADMIN).isEmpty()) {
            Role adminRole = new Role();
            adminRole.setName(Role.RoleName.ROLE_ADMIN);
            roleRepository.save(adminRole);
        }

        if (roleRepository.findByName(Role.RoleName.ROLE_USER).isEmpty()) {
            Role userRole = new Role();
            userRole.setName(Role.RoleName.ROLE_USER);
            roleRepository.save(userRole);
        }

        // Создание начального администратора
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setEmail("admin@example.com");
            admin.setFullName("System Administrator");

            Set<Role> roles = new HashSet<>();
            roles.add(roleRepository.findByName(Role.RoleName.ROLE_ADMIN).orElseThrow());
            roles.add(roleRepository.findByName(Role.RoleName.ROLE_USER).orElseThrow());
            admin.setRoles(roles);

            userRepository.save(admin);
            System.out.println("Initial admin user created: admin / admin123");
        }

        // Создание тестового пользователя
        if (userRepository.findByUsername("user").isEmpty()) {
            User user = new User();
            user.setUsername("user");
            user.setPassword(passwordEncoder.encode("user123"));
            user.setEmail("user@example.com");
            user.setFullName("Test User");

            Set<Role> roles = new HashSet<>();
            roles.add(roleRepository.findByName(Role.RoleName.ROLE_USER).orElseThrow());
            user.setRoles(roles);

            userRepository.save(user);
            System.out.println("Initial test user created: user / user123");
        }
    }
}

