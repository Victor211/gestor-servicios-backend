package com.gestorservicios.service;

import com.gestorservicios.dto.request.LoginRequest;
import com.gestorservicios.dto.response.LoginResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);
}
