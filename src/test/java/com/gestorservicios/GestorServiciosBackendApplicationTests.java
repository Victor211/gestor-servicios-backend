package com.gestorservicios;

import com.gestorservicios.support.TestJwt;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// Secreto solo para tests: el .env local no tiene por qué definir JWT_SECRET
@SpringBootTest(properties = "security.jwt.secret=" + TestJwt.SECRET)
class GestorServiciosBackendApplicationTests {

    @Test
    void contextLoads() {
    }

}
