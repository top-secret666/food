package by.vstu.zamok.user.controller;

import by.vstu.zamok.user.auth.KeycloakAuthService;
import by.vstu.zamok.user.configuration.SecurityConfig;
import by.vstu.zamok.user.configuration.WebConfig;
import by.vstu.zamok.user.dto.UserDto;
import by.vstu.zamok.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@Import({SecurityConfig.class, WebConfig.class})
class AuthControllerSyncTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private KeycloakAuthService authService;

    @Test
    void syncEndpoint_returnsUserDto() throws Exception {
        UserDto dto = new UserDto();
        dto.setId(1L);
        dto.setEmail("google@example.com");
        when(userService.syncFromJwt(any())).thenReturn(dto);

        mockMvc.perform(post("/api/auth/sync")
                        .with(jwt().jwt(builder -> builder
                                .subject("google-kc-id")
                                .claim("email", "google@example.com")
                                .claim("email_verified", true)))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("google@example.com"));
    }
}
