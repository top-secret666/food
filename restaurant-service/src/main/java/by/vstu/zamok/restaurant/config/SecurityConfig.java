package by.vstu.zamok.restaurant.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.HttpMethod;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource) {
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz
                    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/actuator/health", "/actuator/info").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/restaurants", "/api/restaurants/*").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/restaurants/search", "/api/restaurants/popular", "/api/restaurants/trending").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/restaurants/*/dishes").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/dishes/*").permitAll()
                    .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
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
