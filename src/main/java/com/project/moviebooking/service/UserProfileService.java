package com.project.moviebooking.service;

import com.project.moviebooking.model.User;
import com.project.moviebooking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * UserProfileService — manages user profile operations
 * =====================================================
 * SOLID: S — Single Responsibility Principle
 *
 * WHY SPLIT FROM AuthService?
 * - AuthService handles ONLY: login, register, token validation
 * - UserProfileService handles ONLY: profile read/update/delete
 *
 * This is the textbook example of SRP:
 * "A class should have only ONE reason to change."
 * - AuthService changes when auth logic changes
 * - UserProfileService changes when profile logic changes
 * They never interfere with each other.
 *
 * SOLID: D — Depends on UserRepository (abstraction), not MongoTemplate directly
 * GRASP: Low Coupling — completely independent of AuthService
 * =====================================================
 */
@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;

    /**
     * Get user profile by ID
     * Used by: BookingController, TicketController (for display name)
     */
    public User getUserById(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));
    }

    /**
     * Get user profile by email
     */
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }

    /**
     * Get all users — admin view
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Update user profile (name, phone only — NOT password or role)
     * SOLID: S — password changes handled elsewhere (AuthService)
     */
    public User updateProfile(String userId, String name, String phone) {
        User user = getUserById(userId);
        if (name  != null && !name.isBlank())  user.setName(name);
        if (phone != null && !phone.isBlank()) user.setPhone(phone);
        return userRepository.save(user);
    }

    /**
     * Deactivate user account (soft delete)
     */
    public void deactivateUser(String userId) {
        User user = getUserById(userId);
        user.setActive(false);
        userRepository.save(user);
    }

    /**
     * Reactivate account — admin operation
     */
    public void reactivateUser(String userId) {
        User user = getUserById(userId);
        user.setActive(true);
        userRepository.save(user);
    }

    /**
     * Count total registered users
     */
    public long countUsers() {
        return userRepository.count();
    }
}
