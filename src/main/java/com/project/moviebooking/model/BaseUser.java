package com.project.moviebooking.model;

import lombok.Data;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;

/**
 * BaseUser — Abstract base class for all user types
 * ================================================
 * SOLID: L — Liskov Substitution Principle
 * Both User and Admin (via role) extend this base.
 * Anywhere a BaseUser is expected, a User can be used.
 *
 * OOP: Inheritance — common fields extracted to parent
 * OOP: Abstraction — BaseUser defines the contract
 * ================================================
 */
@Data
public abstract class BaseUser {

    @Id
    protected String id;

    protected String name;

    protected String email;

    protected String password;

    protected String phone;

    protected boolean active = true;

    protected LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Abstract method — each subtype defines its role
     * SOLID: L — subclasses must honour this contract
     */
    public abstract String getRole();

    /**
     * Check if user has admin privileges
     * SOLID: L — safe to call on any BaseUser reference
     */
    public boolean isAdmin() {
        return "ADMIN".equals(getRole());
    }

    /**
     * Display name for notifications / tickets
     */
    public String getDisplayName() {
        return name + " (" + getRole() + ")";
    }
}
