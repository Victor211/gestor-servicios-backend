package com.gestorservicios.dto.response;

import com.gestorservicios.entity.User;
import com.gestorservicios.entity.UserRole;

import java.time.Instant;

public record UserResponse(
        Long id,
        String username,
        String fullName,
        UserRole role,
        Boolean active,
        Instant createdAt,
        Instant updatedAt
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getRole(),
                user.getActive(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
