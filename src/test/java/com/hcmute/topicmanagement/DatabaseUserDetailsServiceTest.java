package com.hcmute.topicmanagement;

import static org.junit.jupiter.api.Assertions.assertThrows;
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
        when(userRepository.findByLoginIdentifier("student")).thenReturn(Optional.of(user));

        DatabaseUserDetailsService service = new DatabaseUserDetailsService(userRepository);

        assertThrows(DisabledException.class, () -> service.loadUserByUsername("student"));
    }
}
