package com.project.moviebooking.controller;

import com.project.moviebooking.dto.ApiResponse;
import com.project.moviebooking.dto.LoginRequest;
import com.project.moviebooking.dto.LoginResponse;
import com.project.moviebooking.dto.RegisterRequest;
import com.project.moviebooking.model.User;
import com.project.moviebooking.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * AuthController — REST endpoints for authentication
 * ====================================================
 * SOLID: S — ONLY auth endpoints (no profile, no booking)
 * SOLID: D — Depends on AuthService abstraction
 * GRASP: Controller — delegates to AuthService, no business logic here
 *
 * Prasad's module: Auth + User Management
 * ====================================================
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "http://localhost:5173"})
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/auth/register
     * Creates a new USER or ADMIN account
     * Body: { name, email, password, phone, role }
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserSafeResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        User user = authService.register(request);

        // Never return password — wrap in safe DTO
        UserSafeResponse safe = new UserSafeResponse(
                user.getId(), user.getName(), user.getEmail(),
                user.getPhone(), user.getRole(), user.getCreatedAt().toString()
        );
        return ResponseEntity.ok(ApiResponse.success(
                "Registration successful! Welcome to CineBook.", safe));
    }

    /**
     * POST /api/auth/login
     * Authenticates user and returns JWT token
     * Body: { email, password }
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful!", response));
    }

    /** Safe user DTO — never exposes password in API response */
    public record UserSafeResponse(
            String id, String name, String email,
            String phone, String role, String createdAt
    ) {}
}
