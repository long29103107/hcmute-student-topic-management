package com.hcmute.topicmanagement.security;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hcmute.topicmanagement.model.PermissionEntity;
import com.hcmute.topicmanagement.model.RoleEntity;
import com.hcmute.topicmanagement.model.RolePermissionEntity;
import com.hcmute.topicmanagement.model.UserEntity;
import com.hcmute.topicmanagement.model.UserRoleEntity;
import com.hcmute.topicmanagement.repository.UserRepository;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public DatabaseUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity user = userRepository.findByLoginIdentifier(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        if (!user.isActive()) {
            throw new DisabledException("User is inactive: " + username);
        }

        Set<SimpleGrantedAuthority> authorities = new LinkedHashSet<>();
        String primaryRoleName = null;

        for (UserRoleEntity userRole : user.getUserRoles()) {
            RoleEntity role = userRole.getRole();
            if (!userRole.isActive() || role == null || !role.isActive()) {
                continue;
            }

            if (primaryRoleName == null) {
                primaryRoleName = role.getName();
            }
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getCode().toUpperCase(Locale.ROOT)));

            for (RolePermissionEntity rolePermission : role.getRolePermissions()) {
                PermissionEntity permission = rolePermission.getPermission();
                if (rolePermission.isActive() && permission != null && permission.isActive()) {
                    authorities.add(new SimpleGrantedAuthority(permission.getCode()));
                }
            }
        }

        return new DatabaseUserPrincipal(
                user.getLoginIdentifier(),
                user.getPasswordHash(),
                user.getFullName(),
                primaryRoleName == null ? "Tài khoản" : primaryRoleName,
                authorities);
    }
}
