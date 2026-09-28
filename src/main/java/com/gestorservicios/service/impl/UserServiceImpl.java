package com.gestorservicios.service.impl;

import com.gestorservicios.dto.request.UserCreateRequest;
import com.gestorservicios.dto.request.UserUpdateRequest;
import com.gestorservicios.dto.response.UserResponse;
import com.gestorservicios.entity.User;
import com.gestorservicios.entity.UserRole;
import com.gestorservicios.exception.ConflictException;
import com.gestorservicios.exception.ResourceNotFoundException;
import com.gestorservicios.repository.UserRepository;
import com.gestorservicios.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserResponse create(UserCreateRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ConflictException("Ya existe un usuario con el username " + request.username());
        }

        User user = new User();
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.setRole(request.role());
        user.setActive(true);

        return UserResponse.from(userRepository.save(user));
    }

    @Override
    public List<UserResponse> findAll() {
        return userRepository.findAll(Sort.by("id")).stream()
                .map(UserResponse::from)
                .toList();
    }

    @Override
    public UserResponse findById(Long id) {
        return UserResponse.from(getUser(id));
    }

    @Override
    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request) {
        User user = getUser(id);
        user.setFullName(request.fullName());
        user.setRole(request.role());
        // flush para que @PreUpdate actualice updatedAt antes de construir la respuesta
        return UserResponse.from(userRepository.saveAndFlush(user));
    }

    @Override
    @Transactional
    public void deactivate(Long id) {
        User user = getUser(id);
        user.setActive(false);
        userRepository.save(user);
    }

    @Override
    public List<UserResponse> findActiveTechnicians() {
        return userRepository.findByRoleAndActiveTrue(UserRole.TECHNICIAN).stream()
                .map(UserResponse::from)
                .toList();
    }

    private User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe un usuario con id " + id));
    }
}
