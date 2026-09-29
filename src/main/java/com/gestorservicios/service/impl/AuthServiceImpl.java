package com.gestorservicios.service.impl;

import com.gestorservicios.dto.request.LoginRequest;
import com.gestorservicios.dto.response.LoginResponse;
import com.gestorservicios.dto.response.UserResponse;
import com.gestorservicios.entity.User;
import com.gestorservicios.exception.InvalidCredentialsException;
import com.gestorservicios.repository.UserRepository;
import com.gestorservicios.security.JwtService;
import com.gestorservicios.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password()));
        } catch (AuthenticationException ex) {
            // username inexistente, password incorrecta o usuario inactivo: misma respuesta genérica
            throw new InvalidCredentialsException();
        }

        User user = userRepository.findByUsername(request.username())
                .filter(u -> Boolean.TRUE.equals(u.getActive()))
                .orElseThrow(InvalidCredentialsException::new);

        String token = jwtService.generateToken(user);
        return LoginResponse.bearer(token, jwtService.getExpirationMs(), UserResponse.from(user));
    }
}
