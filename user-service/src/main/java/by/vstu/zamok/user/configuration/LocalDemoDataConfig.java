package by.vstu.zamok.user.configuration;

import by.vstu.zamok.user.entity.Role;
import by.vstu.zamok.user.entity.User;
import by.vstu.zamok.user.repository.RoleRepository;
import by.vstu.zamok.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Set;

@Configuration
@Profile("local")
@RequiredArgsConstructor
public class LocalDemoDataConfig {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner seedLocalUsers() {
        return args -> {
            Role userRole = ensureRole("ROLE_USER");
            Role managerRole = ensureRole("ROLE_MANAGER");
            Role adminRole = ensureRole("ROLE_ADMIN");

            upsert("user@aroma.app", "Aroma Guest", "aroma123", "local-user", Set.of(userRole));
            upsert("manager@aroma.app", "Aroma Manager", "aroma123", "local-manager", Set.of(userRole, managerRole));
            upsert("admin@aroma.app", "Aroma Admin", "aroma123", "local-admin", Set.of(adminRole));

            // Keep roles in sync if users already existed from an older seed
            userRepository.findByEmail("manager@aroma.app").ifPresent(u -> {
                u.setRoles(Set.of(userRole, managerRole));
                userRepository.save(u);
            });
            userRepository.findByEmail("admin@aroma.app").ifPresent(u -> {
                u.setRoles(Set.of(adminRole));
                userRepository.save(u);
            });
            userRepository.findByEmail("user@aroma.app").ifPresent(u -> {
                u.setRoles(Set.of(userRole));
                userRepository.save(u);
            });
        };
    }

    private Role ensureRole(String name) {
        return roleRepository.findByName(name).orElseGet(() -> {
            Role r = new Role();
            r.setName(name);
            return roleRepository.save(r);
        });
    }

    private void upsert(String email, String fullName, String password, String keycloakId, Set<Role> roles) {
        userRepository.findByEmail(email).orElseGet(() -> {
            User user = new User();
            user.setEmail(email);
            user.setFullName(fullName);
            user.setPassword(passwordEncoder.encode(password));
            user.setKeycloakId(keycloakId);
            user.setRoles(roles);
            user.setCreatedAt(Timestamp.from(Instant.now()));
            user.setUpdatedAt(Timestamp.from(Instant.now()));
            return userRepository.save(user);
        });
    }
}
