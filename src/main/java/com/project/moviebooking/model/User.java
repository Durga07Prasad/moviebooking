package com.project.moviebooking.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

/**
 * User Model — MongoDB Document
 * =========================================================
 * SOLID: S — Single Responsibility: only holds user profile data.
 *            Auth logic lives in AuthService (SRP separation).
 *
 * SOLID: L — Liskov Substitution: the role field distinguishes
 *            USER from ADMIN. getIsAdmin() can substitute role checks
 *            anywhere — callers treat both transparently via role.
 *            (BaseUser abstract class in BaseUser.java demonstrates
 *             full LSP inheritance for your report/viva.)
 *
 * OOP: Encapsulation — @Data generates private getters/setters
 * OOP: Abstraction   — callers see only what they need (role, email)
 * =========================================================
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {

    @Id
    private String id;

    @NotBlank(message = "Name is required")
    private String name;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    @Indexed(unique = true)
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    private String phone;

    /**
     * SOLID: L — Role differentiates USER vs ADMIN.
     * Admin is a specialised User; anywhere a User is expected,
     * an ADMIN-role User can be substituted safely.
     */
    private String role = "USER";  // "USER" or "ADMIN"

    private boolean active = true;

    private LocalDateTime createdAt = LocalDateTime.now();

    /** Convenience: LSP-safe admin check without casting */
    public boolean isAdminRole() { return "ADMIN".equals(this.role); }
}
