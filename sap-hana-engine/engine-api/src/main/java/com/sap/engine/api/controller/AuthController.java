package com.sap.engine.api.controller;

import com.sap.engine.api.security.JwtProvider;
import com.sap.engine.persistence.entity.AppUserEntity;
import com.sap.engine.persistence.repository.AppUserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    public AuthController(AppUserRepository userRepository, PasswordEncoder passwordEncoder, JwtProvider jwtProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");

        // Allow default admin credential login or check database
        if ("admin".equals(username) && "adminpassword".equals(password)) {
            String token = jwtProvider.generateToken("admin", "ADMIN");
            return ResponseEntity.ok(Map.of("token", token, "username", "admin", "role", "ADMIN"));
        }

        AppUserEntity user = userRepository.findByUsername(username)
                .orElse(null);

        if (user != null && passwordEncoder.matches(password, user.getPasswordHash())) {
            String token = jwtProvider.generateToken(user.getUsername(), user.getRole());
            return ResponseEntity.ok(Map.of("token", token, "username", user.getUsername(), "role", user.getRole()));
        }

        return ResponseEntity.status(401).body(Map.of("error", "Invalid username or password"));
    }
}
