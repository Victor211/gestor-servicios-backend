package com.gestorservicios.security;

import com.gestorservicios.entity.User;
import com.gestorservicios.entity.UserRole;
import com.gestorservicios.support.TestJwt;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService(TestJwt.SECRET, 3_600_000);

    @Test
    void generateToken_containsSubjectRoleAndDates() {
        String token = jwtService.generateToken(user("admin", UserRole.ADMIN));

        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(TestJwt.SECRET)))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertThat(claims.getSubject()).isEqualTo("admin");
        assertThat(claims.get(JwtService.ROLE_CLAIM, String.class)).isEqualTo("ADMIN");
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
        assertThat(jwtService.extractUsername(token)).isEqualTo("admin");
    }

    @Test
    void isTokenValid_withMatchingUser_returnsTrue() {
        String token = jwtService.generateToken(user("tec1", UserRole.TECHNICIAN));

        assertThat(jwtService.isTokenValid(token, userDetails("tec1"))).isTrue();
        assertThat(jwtService.isTokenValid(token, userDetails("otro"))).isFalse();
    }

    @Test
    void expiredToken_isInvalid() {
        JwtService expiredService = new JwtService(TestJwt.SECRET, -1_000);
        String token = expiredService.generateToken(user("admin", UserRole.ADMIN));

        assertThat(jwtService.isTokenValid(token, userDetails("admin"))).isFalse();
        assertThatThrownBy(() -> jwtService.extractUsername(token)).isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void tokenSignedWithOtherKey_isInvalid() {
        String otherSecret = "c2VjcmV0by1kaXN0aW50by1wYXJhLXRlc3RzLWRlLWZpcm1hLWludmFsaWRhLTEyMzQ1Ng==";
        String token = new JwtService(otherSecret, 3_600_000).generateToken(user("admin", UserRole.ADMIN));

        assertThat(jwtService.isTokenValid(token, userDetails("admin"))).isFalse();
        assertThat(jwtService.isTokenValid("no-es-un-jwt", userDetails("admin"))).isFalse();
    }

    @Test
    void weakSecret_isRejectedAtStartup() {
        assertThatThrownBy(() -> new JwtService("c2hvcnQ=", 3_600_000))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new JwtService("", 3_600_000))
                .isInstanceOf(IllegalStateException.class);
    }

    private static User user(String username, UserRole role) {
        User user = new User();
        user.setUsername(username);
        user.setRole(role);
        return user;
    }

    private static UserDetails userDetails(String username) {
        return org.springframework.security.core.userdetails.User
                .withUsername(username).password("x").authorities("ROLE_ADMIN").build();
    }
}
