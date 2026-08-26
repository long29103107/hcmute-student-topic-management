package com.hcmute.topicmanagement.security;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public final class DatabaseUserPrincipal implements UserDetails {

    private final String username;
    private final String password;
    private final String fullName;
    private final String primaryRoleName;
    private final List<? extends GrantedAuthority> authorities;

    public DatabaseUserPrincipal(
            String username,
            String password,
            String fullName,
            String primaryRoleName,
            Collection<? extends GrantedAuthority> authorities) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.primaryRoleName = primaryRoleName;
        this.authorities = List.copyOf(authorities);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
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

    public String getFullName() {
        return fullName;
    }

    public String getPrimaryRoleName() {
        return primaryRoleName;
    }

    public String getInitials() {
        String value = fullName == null || fullName.isBlank() ? username : fullName.trim();
        String[] parts = value.split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, 1).toUpperCase(Locale.ROOT);
        }
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1))
                .toUpperCase(Locale.ROOT);
    }
}
