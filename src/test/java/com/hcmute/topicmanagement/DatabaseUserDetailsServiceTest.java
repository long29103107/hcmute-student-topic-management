package com.hcmute.topicmanagement;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.DisabledException;

import com.hcmute.topicmanagement.model.UserEntity;
import com.hcmute.topicmanagement.repository.UserRepository;
import com.hcmute.topicmanagement.security.DatabaseUserDetailsService;

@ExtendWith(MockitoExtension.class)
class DatabaseUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Test
    void accountWithoutAnActiveRoleCannotAuthenticate() {
        UserEntity user = new UserEntity("student", "Student", "{bcrypt}hash");
        user.setEmailOrCode("student@example.com");
        when(userRepository.findByEmailIgnoreCase("student@example.com")).thenReturn(Optional.of(user));

        DatabaseUserDetailsService service = new DatabaseUserDetailsService(userRepository);

        assertThrows(DisabledException.class, () -> service.loadUserByUsername("student@example.com"));
    }

    @Test
    void authenticatesByEmailAndNotByInternalLoginIdentifier() {
        UserEntity user = new UserEntity("student-internal", "Student", "{bcrypt}hash");
        user.setEmailOrCode("student@example.com");
        user.addRole(new com.hcmute.topicmanagement.model.RoleEntity("STUDENT", "Student", "Student role."));
        when(userRepository.findByEmailIgnoreCase("student@example.com")).thenReturn(Optional.of(user));

        DatabaseUserDetailsService service = new DatabaseUserDetailsService(userRepository);

        org.springframework.security.core.userdetails.UserDetails principal =
                service.loadUserByUsername(" student@example.com ");

        assertEquals("student@example.com", principal.getUsername());
        verify(userRepository).findByEmailIgnoreCase("student@example.com");
        verify(userRepository, never()).findByLoginIdentifier(anyString());
    }
}
