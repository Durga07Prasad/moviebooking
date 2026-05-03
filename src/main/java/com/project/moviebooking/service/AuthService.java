package com.project.moviebooking.service;

import com.project.moviebooking.config.JwtUtil;
import com.project.moviebooking.dto.LoginRequest;
import com.project.moviebooking.dto.LoginResponse;
import com.project.moviebooking.dto.RegisterRequest;
import com.project.moviebooking.model.User;
import com.project.moviebooking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * AuthService — ONLY handles authentication
 * ==========================================
 * SOLID: S — Single Responsibility Principle
 *
 * This class has ONE job: authenticate users.
 * - login()    → verify credentials → return JWT
 * - register() → create account → hash password
 *
 * Profile management (getUserById, updateProfile) lives
 * in UserProfileService — that's the SRP split.
 *
 * SOLID: D — depends on UserRepository abstraction
 * GRASP: Low Coupling — knows nothing about tickets/bookings
 * ==========================================
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil         jwtUtil;

    /**
     * Register new user account
     * OOP: Encapsulation — password is hashed before storage
     */
    public User register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered: " + request.getEmail());
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setRole(request.getRole() != null ? request.getRole().toUpperCase() : "USER");
        user.setActive(true);

        return userRepository.save(user);
    }

    /**
     * Login — verify credentials and return JWT token
     */
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException(
                        "No account found with email: " + request.getEmail()));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Incorrect password.");
        }
        if (!user.isActive()) {
            throw new RuntimeException("Account is deactivated. Contact admin.");
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole());

        return new LoginResponse(token, user.getId(), user.getName(),
                user.getEmail(), user.getRole(), "Login successful");
    }
}
