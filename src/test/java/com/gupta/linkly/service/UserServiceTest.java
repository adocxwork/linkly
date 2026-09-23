package com.gupta.linkly.service;

import com.gupta.linkly.dto.PublicProfileResponse;
import com.gupta.linkly.entity.Role;
import com.gupta.linkly.entity.User;
import com.gupta.linkly.exception.ResourceNotFoundException;
import com.gupta.linkly.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .name("Aditya")
                .username("adityagupta")
                .email("test@example.com")
                .role(Role.ROLE_USER)
                .isSuspended(false)
                .enablePublicMessaging(true)
                .build();
    }

    @Test
    void testGetPublicProfile_Success() {
        when(userRepository.findByUsername("adityagupta")).thenReturn(Optional.of(testUser));

        PublicProfileResponse response = userService.getPublicProfile("adityagupta");

        assertNotNull(response);
        assertEquals("Aditya", response.getName());
        assertEquals("adityagupta", response.getUsername());
    }

    @Test
    void testGetPublicProfile_UserSuspended() {
        testUser.setIsSuspended(true);
        when(userRepository.findByUsername("adityagupta")).thenReturn(Optional.of(testUser));

        assertThrows(ResourceNotFoundException.class, () -> userService.getPublicProfile("adityagupta"));
    }

    @Test
    void testGetPublicProfile_UserNotFound() {
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getPublicProfile("adityagupta"));
    }
}
