package by.vstu.zamok.user.auth;

import by.vstu.zamok.user.auth.dto.LoginRequest;
import by.vstu.zamok.user.auth.dto.RegisterRequest;
import by.vstu.zamok.user.dto.UserDto;
import by.vstu.zamok.user.entity.Role;
import by.vstu.zamok.user.entity.User;
import by.vstu.zamok.user.exception.ResourceNotFoundException;
import by.vstu.zamok.user.mapper.UserMapper;
import by.vstu.zamok.user.repository.RoleRepository;
import by.vstu.zamok.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Profile("local")
@RequiredArgsConstructor
public class LocalAuthService implements AuthGateway {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final LocalJwtService jwtService;
    private final RestTemplate restTemplate;

    @Value("${app.google.client-id:}")
    private String googleClientId;

    @Override
    public UserDto register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email is already in use");
        }
        Role roleUser = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new ResourceNotFoundException("Role ROLE_USER not found"));

        User user = new User();
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setKeycloakId("local-" + UUID.randomUUID());
        user.setRoles(Set.of(roleUser));
        user.setCreatedAt(Timestamp.from(Instant.now()));
        user.setUpdatedAt(Timestamp.from(Instant.now()));
        return userMapper.toDto(userRepository.save(user));
    }

    @Override
    public Map<String, Object> login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid credentials");
        }
        return jwtService.issueTokens(user.getKeycloakId(), user.getEmail(), realmRoles(user));
    }

    @Override
    public Map<String, Object> refresh(String refreshToken) {
        try {
            Claims claims = jwtService.parse(refreshToken);
            if (!"refresh".equals(claims.get("typ", String.class))) {
                throw new BadCredentialsException("Invalid refresh token");
            }
            User user = userRepository.findByKeycloakId(claims.getSubject())
                    .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
            return jwtService.issueTokens(user.getKeycloakId(), user.getEmail(), realmRoles(user));
        } catch (BadCredentialsException e) {
            throw e;
        } catch (Exception e) {
            throw new BadCredentialsException("Invalid refresh token", e);
        }
    }

    @Override
    public Map<String, Object> loginWithGoogle(String idToken) {
        Map<String, Object> payload = verifyGoogleIdToken(idToken);
        String email = String.valueOf(payload.get("email"));
        if (email == null || email.isBlank() || "null".equals(email)) {
            throw new BadCredentialsException("Google token does not contain email");
        }
        boolean emailVerified = Boolean.TRUE.equals(payload.get("email_verified"))
                || "true".equalsIgnoreCase(String.valueOf(payload.get("email_verified")));
        if (!emailVerified) {
            throw new BadCredentialsException("Google email is not verified");
        }

        String sub = String.valueOf(payload.get("sub"));
        String name = payload.get("name") != null ? String.valueOf(payload.get("name")) : email;

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            Role roleUser = roleRepository.findByName("ROLE_USER")
                    .orElseThrow(() -> new ResourceNotFoundException("Role ROLE_USER not found"));
            User created = new User();
            created.setEmail(email);
            created.setFullName(name);
            created.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            created.setKeycloakId("google-" + sub);
            created.setRoles(Set.of(roleUser));
            created.setCreatedAt(Timestamp.from(Instant.now()));
            created.setUpdatedAt(Timestamp.from(Instant.now()));
            return userRepository.save(created);
        });

        return jwtService.issueTokens(user.getKeycloakId(), user.getEmail(), realmRoles(user));
    }

    private Map<String, Object> verifyGoogleIdToken(String idToken) {
        String url = UriComponentsBuilder
                .fromHttpUrl("https://oauth2.googleapis.com/tokeninfo")
                .queryParam("id_token", idToken)
                .toUriString();
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.GET, null, new ParameterizedTypeReference<>() {});
            Map<String, Object> body = response.getBody();
            if (body == null || body.get("sub") == null) {
                throw new BadCredentialsException("Invalid Google token");
            }
            if (googleClientId != null && !googleClientId.isBlank()) {
                Object aud = body.get("aud");
                if (aud == null || !googleClientId.equals(String.valueOf(aud))) {
                    throw new BadCredentialsException("Google token audience mismatch");
                }
            }
            return body;
        } catch (BadCredentialsException e) {
            throw e;
        } catch (Exception e) {
            throw new BadCredentialsException("Invalid Google token", e);
        }
    }

    private List<String> realmRoles(User user) {
        return user.getRoles().stream()
                .map(Role::getName)
                .map(name -> name.startsWith("ROLE_") ? name.substring(5) : name)
                .collect(Collectors.toList());
    }
}
