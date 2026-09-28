package com.gestorservicios.service.impl;

import com.gestorservicios.dto.request.UserCreateRequest;
import com.gestorservicios.dto.request.UserUpdateRequest;
import com.gestorservicios.dto.response.UserResponse;
import com.gestorservicios.entity.User;
import com.gestorservicios.entity.UserRole;
import com.gestorservicios.exception.ConflictException;
import com.gestorservicios.exception.ResourceNotFoundException;
import com.gestorservicios.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, passwordEncoder);
    }

    @Test
    void create_persistsActiveUserAndReturnsResponse() {
        UserCreateRequest request = new UserCreateRequest("jperez", "secreto123", "Juan Perez", UserRole.TECHNICIAN);
        when(userRepository.existsByUsername("jperez")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        UserResponse response = userService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.username()).isEqualTo("jperez");
        assertThat(response.fullName()).isEqualTo("Juan Perez");
        assertThat(response.role()).isEqualTo(UserRole.TECHNICIAN);
        assertThat(response.active()).isTrue();
    }

    @Test
    void create_storesBcryptHashInsteadOfPlainPassword() {
        UserCreateRequest request = new UserCreateRequest("admin", "secreto123", "Administrador", UserRole.ADMIN);
        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.create(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        String hash = captor.getValue().getPasswordHash();
        assertThat(hash).isNotEqualTo("secreto123").startsWith("$2");
        assertThat(passwordEncoder.matches("secreto123", hash)).isTrue();
    }

    @Test
    void create_withDuplicateUsername_throwsConflict() {
        UserCreateRequest request = new UserCreateRequest("admin", "secreto123", "Administrador", UserRole.ADMIN);
        when(userRepository.existsByUsername("admin")).thenReturn(true);

        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Ya existe un usuario con el username admin");
        verify(userRepository, never()).save(any());
    }

    @Test
    void findById_withUnknownId_throwsNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("No existe un usuario con id 99");
    }

    @Test
    void update_changesOnlyFullNameAndRole() {
        User existing = user(5L, "jperez", UserRole.TECHNICIAN, true);
        existing.setPasswordHash("hash-original");
        when(userRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(userRepository.saveAndFlush(existing)).thenReturn(existing);

        UserResponse response = userService.update(5L, new UserUpdateRequest("Juan P. Actualizado", UserRole.ADMIN));

        assertThat(response.fullName()).isEqualTo("Juan P. Actualizado");
        assertThat(response.role()).isEqualTo(UserRole.ADMIN);
        assertThat(existing.getUsername()).isEqualTo("jperez");
        assertThat(existing.getPasswordHash()).isEqualTo("hash-original");
    }

    @Test
    void deactivate_setsActiveFalseWithoutDeleting() {
        User existing = user(5L, "jperez", UserRole.TECHNICIAN, true);
        when(userRepository.findById(5L)).thenReturn(Optional.of(existing));

        userService.deactivate(5L);

        assertThat(existing.getActive()).isFalse();
        verify(userRepository).save(existing);
        verify(userRepository, never()).delete(any());
        verify(userRepository, never()).deleteById(any());
    }

    @Test
    void findActiveTechnicians_returnsOnlyActiveTechnicians() {
        when(userRepository.findByRoleAndActiveTrue(UserRole.TECHNICIAN))
                .thenReturn(List.of(user(2L, "tec1", UserRole.TECHNICIAN, true)));

        List<UserResponse> result = userService.findActiveTechnicians();

        assertThat(result).singleElement().satisfies(u -> {
            assertThat(u.username()).isEqualTo("tec1");
            assertThat(u.role()).isEqualTo(UserRole.TECHNICIAN);
            assertThat(u.active()).isTrue();
        });
        verify(userRepository).findByRoleAndActiveTrue(UserRole.TECHNICIAN);
    }

    private static User user(Long id, String username, UserRole role, boolean active) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setFullName("Nombre " + username);
        user.setPasswordHash("hash");
        user.setRole(role);
        user.setActive(active);
        return user;
    }
}
