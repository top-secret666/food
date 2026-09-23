package by.vstu.zamok.user.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Profile("local")
public class LocalJwtService {

    private final SecretKey key;
    private final long accessTtlSeconds;
    private final long refreshTtlSeconds;

    public LocalJwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-ttl-seconds:3600}") long accessTtlSeconds,
            @Value("${app.jwt.refresh-ttl-seconds:604800}") long refreshTtlSeconds) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(bytes, 0, padded, 0, bytes.length);
            bytes = padded;
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.accessTtlSeconds = accessTtlSeconds;
        this.refreshTtlSeconds = refreshTtlSeconds;
    }

    public Map<String, Object> issueTokens(String subject, String email, List<String> roles) {
        Instant now = Instant.now();
        String access = buildToken(subject, email, roles, now, accessTtlSeconds, "access");
        String refresh = buildToken(subject, email, roles, now, refreshTtlSeconds, "refresh");

        Map<String, Object> body = new HashMap<>();
        body.put("access_token", access);
        body.put("refresh_token", refresh);
        body.put("token_type", "Bearer");
        body.put("expires_in", accessTtlSeconds);
        body.put("scope", "openid profile email");
        return body;
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    private String buildToken(String subject, String email, List<String> roles, Instant now, long ttl, String typ) {
        Map<String, Object> realmAccess = Map.of("roles", roles);
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttl)))
                .claim("email", email)
                .claim("preferred_username", email)
                .claim("email_verified", true)
                .claim("realm_access", realmAccess)
                .claim("typ", typ)
                .signWith(key)
                .compact();
    }
}
