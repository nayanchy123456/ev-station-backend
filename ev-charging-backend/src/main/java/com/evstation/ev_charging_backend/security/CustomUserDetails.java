package com.evstation.ev_charging_backend.security;

import com.evstation.ev_charging_backend.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * Custom UserDetails implementation for Spring Security.
 * 
 * Wraps the User entity and provides methods required by Spring Security.
 * Also exposes additional user information like userId and role.
 */
public class CustomUserDetails implements UserDetails {

    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singleton(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    /**
     * Get the user's role as a string.
     * 
     * @return Role name (e.g., "USER", "HOST", "ADMIN")
     */
    public String getRole() {
        return user.getRole().name();
    }

    /**
     * Get the user's ID.
     * This is required for chat functionality and other features.
     * 
     * @return User ID (Long)
     */
    public Long getUserId() {
        return user.getUserId();
    }

    /**
     * Get the underlying User entity.
     * Use with caution to avoid exposing sensitive data.
     * 
     * @return User entity
     */
    public User getUser() {
        return user;
    }

    /**
     * Get user's first name.
     * 
     * @return First name
     */
    public String getFirstName() {
        return user.getFirstName();
    }

    /**
     * Get user's last name.
     * 
     * @return Last name
     */
    public String getLastName() {
        return user.getLastName();
    }

    /**
     * Get user's email.
     * Same as getUsername() but more explicit.
     * 
     * @return Email address
     */
    public String getEmail() {
        return user.getEmail();
    }
}