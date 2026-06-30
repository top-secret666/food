package by.vstu.zamok.user.service.impl;

import by.vstu.zamok.user.dto.UserDto;
import by.vstu.zamok.user.entity.Role;
import by.vstu.zamok.user.entity.User;
import by.vstu.zamok.user.exception.UserConflictException;
import by.vstu.zamok.user.mapper.UserMapper;
import by.vstu.zamok.user.repository.RoleRepository;
import by.vstu.zamok.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplSyncTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void syncFromJwt_returnsExistingUser() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("kc-1")
                .claim("email", "user@example.com")
                .build();

        User user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");
        UserDto dto = new UserDto();
        dto.setId(1L);
        dto.setEmail("user@example.com");

        when(userRepository.findByKeycloakId("kc-1")).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(dto);

        UserDto result = userService.syncFromJwt(jwt);

        assertThat(result.getEmail()).isEqualTo("user@example.com");
        verify(userRepository, never()).save(any());
    }

    @Test
    void syncFromJwt_createsUserWhenMissing() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("kc-new")
                .claim("email", "new@example.com")
                .claim("name", "New User")
                .build();

        Role role = new Role();
        role.setName("ROLE_USER");

        User saved = new User();
        saved.setId(2L);
        saved.setEmail("new@example.com");

        UserDto dto = new UserDto();
        dto.setId(2L);
        dto.setEmail("new@example.com");

        when(userRepository.findByKeycloakId("kc-new")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(role));
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(userMapper.toDto(saved)).thenReturn(dto);

        UserDto result = userService.syncFromJwt(jwt);

        assertThat(result.getEmail()).isEqualTo("new@example.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void syncFromJwt_throwsConflictWhenEmailTakenByAnotherKeycloakId() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("kc-new")
                .claim("email", "taken@example.com")
                .build();

        User existing = new User();
        existing.setKeycloakId("other-kc");

        when(userRepository.findByKeycloakId("kc-new")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("taken@example.com")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> userService.syncFromJwt(jwt))
                .isInstanceOf(UserConflictException.class);
    }
}
