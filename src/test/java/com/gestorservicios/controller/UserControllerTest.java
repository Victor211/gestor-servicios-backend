package com.gestorservicios.controller;

import com.gestorservicios.dto.request.UserCreateRequest;
import com.gestorservicios.dto.response.UserResponse;
import com.gestorservicios.entity.UserRole;
import com.gestorservicios.exception.ConflictException;
import com.gestorservicios.exception.ResourceNotFoundException;
import com.gestorservicios.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// La seguridad se prueba en AuthSecurityTest; aquí solo se prueba la capa web del controlador.
@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    void create_returns201WithoutPasswordFields() throws Exception {
        Instant now = Instant.now();
        when(userService.create(any(UserCreateRequest.class)))
                .thenReturn(new UserResponse(1L, "admin", "Administrador", UserRole.ADMIN, true, now, now));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"secreto123","fullName":"Administrador","role":"ADMIN"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void create_withDuplicateUsername_returns409() throws Exception {
        when(userService.create(any(UserCreateRequest.class)))
                .thenThrow(new ConflictException("Ya existe un usuario con el username admin"));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"secreto123","fullName":"Administrador","role":"ADMIN"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Ya existe un usuario con el username admin"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void create_withInvalidBody_returns400() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"","password":"corta","fullName":"Administrador"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void findById_withUnknownId_returns404() throws Exception {
        when(userService.findById(99L)).thenThrow(new ResourceNotFoundException("No existe un usuario con id 99"));

        mockMvc.perform(get("/api/users/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("No existe un usuario con id 99"));
    }

    @Test
    void deactivate_returns204() throws Exception {
        mockMvc.perform(delete("/api/users/5"))
                .andExpect(status().isNoContent());

        verify(userService).deactivate(5L);
    }

    @Test
    void findActiveTechnicians_returns200() throws Exception {
        Instant now = Instant.now();
        when(userService.findActiveTechnicians())
                .thenReturn(List.of(new UserResponse(2L, "tec1", "Tecnico Uno", UserRole.TECHNICIAN, true, now, now)));

        mockMvc.perform(get("/api/users/technicians"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("tec1"))
                .andExpect(jsonPath("$[0].role").value("TECHNICIAN"));
    }
}
