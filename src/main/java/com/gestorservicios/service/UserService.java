package com.gestorservicios.service;

import com.gestorservicios.dto.request.UserCreateRequest;
import com.gestorservicios.dto.request.UserUpdateRequest;
import com.gestorservicios.dto.response.UserResponse;

import java.util.List;

public interface UserService {

    UserResponse create(UserCreateRequest request);

    List<UserResponse> findAll();

    UserResponse findById(Long id);

    UserResponse update(Long id, UserUpdateRequest request);

    void deactivate(Long id);

    List<UserResponse> findActiveTechnicians();
}
