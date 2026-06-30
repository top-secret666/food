package by.vstu.zamok.user.service;

import by.vstu.zamok.user.dto.UserDto;
import by.vstu.zamok.user.dto.UpdateUserRequest;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Map;

public interface UserService {
    UserDto findById(Long id);
    UserDto findByEmail(String email);
    UserDto save(UserDto userDto);
    void deleteById(Long id);
    UserDto registerNewUserAccount(UserDto userDto);
    UserDto findByKeycloakId(String keycloakId);
    UserDto findByKeycloakIdOrSync(Jwt jwt);
    UserDto syncFromJwt(Jwt jwt);
    UserDto updateById(Long id, UpdateUserRequest request);
    List<UserDto> search(String query);
    Map<String, Long> registrationStats(int days);
}
