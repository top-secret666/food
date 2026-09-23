package by.vstu.zamok.user.auth;

import by.vstu.zamok.user.auth.dto.LoginRequest;
import by.vstu.zamok.user.auth.dto.RegisterRequest;
import by.vstu.zamok.user.dto.UserDto;

import java.util.Map;

public interface AuthGateway {
    UserDto register(RegisterRequest request);

    Map<String, Object> login(LoginRequest request);

    Map<String, Object> refresh(String refreshToken);

    Map<String, Object> loginWithGoogle(String idToken);
}
