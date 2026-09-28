package com.gestorservicios.dto.request;

import com.gestorservicios.entity.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(

        @NotBlank(message = "El username es obligatorio")
        @Size(max = 100, message = "El username no puede superar los 100 caracteres")
        String username,

        @NotBlank(message = "La password es obligatoria")
        @Size(min = 8, max = 72, message = "La password debe tener entre 8 y 72 caracteres")
        String password,

        @NotBlank(message = "El nombre completo es obligatorio")
        @Size(max = 150, message = "El nombre completo no puede superar los 150 caracteres")
        String fullName,

        @NotNull(message = "El rol es obligatorio")
        UserRole role
) {
}
