package by.vstu.zamok.user.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource) {
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Order(1)
    public SecurityFilterChain publicEndpointsSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher(
                "/swagger-ui/**",
                "/v3/api-docs/**",
                "/actuator/health",
                "/actuator/info",
                "/api/auth/register",
                "/api/auth/login",
                "/api/auth/refresh",
                "/api/auth/verified"
            )
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource));
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain privateEndpointsSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource));
        return http.build();
    }

    @org.springframework.beans.factory.annotation.Value("${security.require-email-verified:true}")
    private boolean requireVerified;

    private Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter() {
        JwtAuthenticationConverter delegate = new JwtAuthenticationConverter();
        // Подхват ролей realm_access (Keycloak) для @PreAuthorize
        delegate.setJwtGrantedAuthoritiesConverter(jwt -> {
            Object realmAccess = jwt.getClaims().get("realm_access");
            if (!(realmAccess instanceof java.util.Map<?, ?> map)) return java.util.List.of();
            Object roles = map.get("roles");
            if (!(roles instanceof java.util.Collection<?> coll)) return java.util.List.of();
            return coll.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .map(r -> (org.springframework.security.core.GrantedAuthority)
                            new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + r.toUpperCase()))
                    .collect(java.util.stream.Collectors.toList());
        });
        return jwt -> {
            if (requireVerified) {
                Boolean verified = jwt.getClaim("email_verified");
                if (verified == null || !verified) {
                    throw new BadCredentialsException("Email is not verified");
                }
            }
            return delegate.convert(jwt);
        };
    }
}
