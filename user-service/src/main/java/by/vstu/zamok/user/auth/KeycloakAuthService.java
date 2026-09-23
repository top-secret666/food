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
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@Profile("!local")
@RequiredArgsConstructor
public class KeycloakAuthService implements AuthGateway {

    private final KeycloakProperties props;
    private final Keycloak keycloakAdminClient;
    private final RestTemplate restTemplate;
    private final PasswordEncoder passwordEncoder;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;

    public UserDto register(RegisterRequest request) {
        String realm = props.getRealm();

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email is already in use");
        }

        // 1) Create user in Keycloak
        UserRepresentation rep = new UserRepresentation();
        rep.setEnabled(true);
        rep.setUsername(request.getEmail());
        rep.setEmail(request.getEmail());
        rep.setEmailVerified(false);
        rep.setRequiredActions(new ArrayList<>(List.of("VERIFY_EMAIL")));
        rep.setFirstName(null);
        rep.setLastName(null);

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            rep.setFirstName(request.getFullName());
        }

        Response response = keycloakAdminClient.realm(realm).users().create(rep);
        try {
            if (response.getStatus() == 409) {
                throw new IllegalArgumentException("Email is already in use");
            }
            if (response.getStatus() != 201) {
                throw new IllegalStateException("Keycloak user creation failed with status: " + response.getStatus());
            }

            URI location = response.getLocation();
            if (location == null) {
                throw new IllegalStateException("Keycloak did not return Location header for created user");
            }
            String path = location.getPath();
            String keycloakUserId = path.substring(path.lastIndexOf('/') + 1);

            // 2) Set password in Keycloak
            CredentialRepresentation cred = new CredentialRepresentation();
            cred.setType(CredentialRepresentation.PASSWORD);
            cred.setValue(request.getPassword());
            cred.setTemporary(false);
            keycloakAdminClient.realm(realm).users().get(keycloakUserId).resetPassword(cred);

            // 3) Assign realm role USER (must exist in realm)
            RoleRepresentation userRoleKc = keycloakAdminClient.realm(realm).roles().get("USER").toRepresentation();
            keycloakAdminClient.realm(realm).users().get(keycloakUserId).roles().realmLevel().add(List.of(userRoleKc));

            // 3.1) Send verification email + enforce required action
            try {
                Integer lifespan = Optional.ofNullable(props.getVerifyEmailLifespanSeconds()).orElse(3600);
                keycloakAdminClient.realm(realm)
                    .users()
                    .get(keycloakUserId)
                    .executeActionsEmail(
                        props.getVerifyEmailRedirectUri(),
                        props.getClientId(),
                        lifespan,
                        List.of("VERIFY_EMAIL")
                    );
            } catch (Exception e) {
                throw new IllegalStateException("Failed to send verification email. Check Keycloak SMTP settings.", e);
            }

            // 4) Sync to local DB
            Role roleUser = roleRepository.findByName("ROLE_USER")
                    .orElseThrow(() -> new ResourceNotFoundException("Role ROLE_USER not found"));

            User user = new User();
            user.setEmail(request.getEmail());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setFullName(request.getFullName());
            user.setKeycloakId(keycloakUserId);
            user.setCreatedAt(Timestamp.from(Instant.now()));
            user.setUpdatedAt(Timestamp.from(Instant.now()));
            user.setRoles(Set.of(roleUser));

            User saved = userRepository.save(user);
            return userMapper.toDto(saved);
        } finally {
            response.close();
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> login(LoginRequest request) {
        String tokenUrl = props.getBaseUrl() + "/realms/" + props.getRealm() + "/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", props.getClientId());
        form.add("username", request.getEmail());
        form.add("password", request.getPassword());
        form.add("scope", "openid profile email");

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, new HttpEntity<>(form, headers), Map.class);
            Map<String, Object> body = response.getBody();
            if (body == null) {
                throw new BadCredentialsException("Invalid credentials");
            }
            return body;
        } catch (HttpClientErrorException e) {
            throw new BadCredentialsException("Invalid credentials", e);
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("refreshToken must not be blank");
        }

        String tokenUrl = props.getBaseUrl() + "/realms/" + props.getRealm() + "/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("client_id", props.getClientId());
        form.add("refresh_token", refreshToken);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, new HttpEntity<>(form, headers), Map.class);
            Map<String, Object> body = response.getBody();
            if (body == null) {
                throw new BadCredentialsException("Invalid refresh token");
            }
            return body;
        } catch (HttpClientErrorException e) {
            throw new BadCredentialsException("Invalid refresh token", e);
        }
    }

    @Override
    public Map<String, Object> loginWithGoogle(String idToken) {
        throw new UnsupportedOperationException(
                "Use Keycloak Google identity provider in non-local mode, or switch to the local profile.");
    }
}
