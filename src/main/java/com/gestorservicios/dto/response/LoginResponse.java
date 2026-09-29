package com.gestorservicios.dto.response;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserResponse user
) {

    public static final String BEARER = "Bearer";

    public static LoginResponse bearer(String accessToken, long expiresIn, UserResponse user) {
        return new LoginResponse(accessToken, BEARER, expiresIn, user);
    }
}
