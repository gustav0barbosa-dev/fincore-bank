package com.fincore.auth;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.fincore.auth.repository.UserRepository;
import com.fincore.auth.security.JwtService;

@SpringBootTest
class AuthServiceApplicationTests {

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtService jwtService;

    @Test
    void contextLoads() {}
}