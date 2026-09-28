package com.gestorservicios.repository;

import com.gestorservicios.entity.User;
import com.gestorservicios.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    List<User> findByRoleAndActiveTrue(UserRole role);
}
