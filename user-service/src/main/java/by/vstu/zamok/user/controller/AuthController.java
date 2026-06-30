package by.vstu.zamok.user.controller;

import by.vstu.zamok.user.auth.KeycloakAuthService;
import by.vstu.zamok.user.auth.dto.LoginRequest;
import by.vstu.zamok.user.auth.dto.RefreshRequest;
import by.vstu.zamok.user.auth.dto.RegisterRequest;
import by.vstu.zamok.user.dto.UserDto;
import by.vstu.zamok.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final KeycloakAuthService authService;
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<UserDto> register(@RequestBody @Valid RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody @Valid RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request.getRefreshToken()));
    }

    @PostMapping("/sync")
    public ResponseEntity<UserDto> sync(JwtAuthenticationToken authentication) {
        return ResponseEntity.ok(userService.syncFromJwt(authentication.getToken()));
    }

    @GetMapping("/verified")
    public ResponseEntity<?> verified() {
        return ResponseEntity.ok(java.util.Map.of("message", "Email verified"));
    }
}
