package by.vstu.zamok.user.controller;

import by.vstu.zamok.user.dto.UpdateUserRequest;
import by.vstu.zamok.user.dto.UserDto;
import by.vstu.zamok.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    private static boolean isAdmin(JwtAuthenticationToken authentication) {
        return authentication.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    private Long currentUserId(JwtAuthenticationToken authentication) {
        String keycloakId = authentication.getToken().getSubject();
        return userService.findByKeycloakId(keycloakId).getId();
    }

    // Ваш существующий endpoint для получения текущего пользователя
    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(JwtAuthenticationToken authentication) {
        return ResponseEntity.ok(userService.findByKeycloakIdOrSync(authentication.getToken()));
    }

    // Ваш существующий endpoint для обновления пользователя
    @PutMapping("/me")
    public ResponseEntity<UserDto> updateMe(JwtAuthenticationToken authentication, @RequestBody @Valid UpdateUserRequest request) {
        Long id = currentUserId(authentication);
        return ResponseEntity.ok(userService.updateById(id, request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<UserDto> getById(@PathVariable Long id, JwtAuthenticationToken authentication) {
        if (!isAdmin(authentication)) {
            Long currentId = currentUserId(authentication);
            if (!id.equals(currentId)) {
                throw new AccessDeniedException("You do not have permission to access this user");
            }
        }
        return ResponseEntity.ok(userService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<UserDto> updateById(@PathVariable Long id,
                                              JwtAuthenticationToken authentication,
                                              @RequestBody @Valid UpdateUserRequest request) {
        if (!isAdmin(authentication)) {
            Long currentId = currentUserId(authentication);
            if (!id.equals(currentId)) {
                throw new AccessDeniedException("You do not have permission to update this user");
            }
        }
        return ResponseEntity.ok(userService.updateById(id, request));
    }

    // Новый endpoint, который мы добавляем для связи сервисов
    @GetMapping("/by-keycloak-id/{keycloakId}")
    public ResponseEntity<UserDto> getUserByKeycloakId(@PathVariable String keycloakId, JwtAuthenticationToken authentication) {
        String subject = authentication.getToken().getSubject();
        boolean isAdmin = authentication.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (!isAdmin && (subject == null || !subject.equals(keycloakId))) {
            throw new AccessDeniedException("You do not have permission to access this user");
        }
        return ResponseEntity.ok(userService.findByKeycloakId(keycloakId));
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> searchUsers(@RequestParam("query") String query) {
        return ResponseEntity.ok(userService.search(query));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> registrationStats(@RequestParam(name = "days", defaultValue = "30") int days) {
        return ResponseEntity.ok(userService.registrationStats(days));
    }
}