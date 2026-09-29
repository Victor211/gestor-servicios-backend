package com.gestorservicios.security;

import com.gestorservicios.config.PasswordEncoderConfig;
import com.gestorservicios.config.SecurityConfig;
import com.gestorservicios.controller.AuthController;
import com.gestorservicios.controller.UserController;
import com.gestorservicios.entity.User;
import com.gestorservicios.entity.UserRole;
import com.gestorservicios.repository.UserRepository;
import com.gestorservicios.service.UserService;
import com.gestorservicios.service.impl.AuthServiceImpl;
import com.gestorservicios.support.TestJwt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba la cadena de seguridad real (filtro JWT, BCrypt, AuthenticationManager)
 * con el repositorio mockeado: no toca la base de datos.
 */
@WebMvcTest(controllers = {AuthController.class, UserController.class})
@Import({SecurityConfig.class, PasswordEncoderConfig.class, JwtService.class, CustomUserDetailsService.class,
        SecurityErrorHandler.class, AuthServiceImpl.class})
@TestPropertySource(properties = {
        "security.jwt.secret=" + TestJwt.SECRET,
        "security.jwt.expiration-ms=3600000"
})
class AuthSecurityTest {

    private static final String PASSWORD = "Admin123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private UserService userService;

    private User admin;

    @BeforeEach
    void setUp() {
        admin = new User();
        admin.setId(1L);
        admin.setUsername("admin");
        admin.setPasswordHash(passwordEncoder.encode(PASSWORD));
        admin.setFullName("Administrador");
        admin.setRole(UserRole.ADMIN);
        admin.setActive(true);
        admin.setCreatedAt(Instant.now());
        admin.setUpdatedAt(Instant.now());
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(userRepository.findByUsername("noexiste")).thenReturn(Optional.empty());
    }

    @Test
    void login_withValidCredentials_returnsTokenAndUserWithoutPasswordHash() throws Exception {
        login("admin", PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600000))
                .andExpect(jsonPath("$.user.id").value(1))
                .andExpect(jsonPath("$.user.username").value("admin"))
                .andExpect(jsonPath("$.user.role").value("ADMIN"))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.user.password").doesNotExist())
                .andExpect(content().string(not(containsString(admin.getPasswordHash()))));
    }

    @Test
    void login_withWrongPassword_returns401() throws Exception {
        expectInvalidCredentials(login("admin", "incorrecta"));
    }

    @Test
    void login_withUnknownUsername_returns401() throws Exception {
        expectInvalidCredentials(login("noexiste", PASSWORD));
    }

    @Test
    void login_withInactiveUser_returns401() throws Exception {
        admin.setActive(false);
        expectInvalidCredentials(login("admin", PASSWORD));
    }

    @Test
    void login_withBlankFields_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void protectedEndpoint_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("No autenticado"));
    }

    @Test
    void protectedEndpoint_withValidToken_isAllowed() throws Exception {
        when(userService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.generateToken(admin)))
                .andExpect(status().isOk());
    }

    @Test
    void protectedEndpoint_withMalformedToken_returns401() throws Exception {
        mockMvc.perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, "Bearer esto.no.es-un-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("No autenticado"));
    }

    @Test
    void protectedEndpoint_withExpiredToken_returns401() throws Exception {
        String expired = new JwtService(TestJwt.SECRET, -1_000).generateToken(admin);

        mockMvc.perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, "Bearer " + expired))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_withTokenOfDeactivatedUser_returns401() throws Exception {
        String token = jwtService.generateToken(admin);
        admin.setActive(false);

        mockMvc.perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions login(String username, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"));
    }

    private static void expectInvalidCredentials(ResultActions result) throws Exception {
        result.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Credenciales inválidas"))
                .andExpect(jsonPath("$.accessToken").doesNotExist());
    }
}
