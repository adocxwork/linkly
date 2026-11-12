package com.gupta.linkly.service;

import com.gupta.linkly.dto.AuthResponse;
import com.gupta.linkly.dto.ChangeSettingsRequest;
import com.gupta.linkly.dto.PublicProfileResponse;
import com.gupta.linkly.entity.Link;
import com.gupta.linkly.entity.Role;
import com.gupta.linkly.entity.User;
import com.gupta.linkly.exception.BadRequestException;
import com.gupta.linkly.exception.ForbiddenException;
import com.gupta.linkly.exception.ResourceNotFoundException;
import com.gupta.linkly.redis.RedisJsonCache;
import com.gupta.linkly.repository.LinkRepository;
import com.gupta.linkly.repository.UserRepository;
import com.gupta.linkly.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UserServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final LinkRepository linkRepository = mock(LinkRepository.class);
    private final RedisJsonCache cache = mock(RedisJsonCache.class);
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final JwtUtil jwtUtil = new JwtUtil("test-secret-that-is-at-least-32-characters-long", 60_000);
    private final UserService service = new UserService(userRepository, linkRepository, encoder, jwtUtil, cache);
    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().id(UUID.randomUUID()).name("Alice").username("alice").email("alice@example.com")
                .password(encoder.encode("password123")).role(Role.ROLE_USER)
                .upiId("alice@okbank").enableUpiPayment(false).enablePublicMessaging(true).build();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(cache.get(anyString(), any())).thenReturn(Optional.empty());
    }

    @Test
    void publicProfileShowsOnlyActiveLinksAndHidesUpiWhenDisabled() {
        Link link = Link.builder().id(UUID.randomUUID()).title("Site").shortUrl("abc").active(true).clickCount(42L).build();
        when(linkRepository.findByUserIdAndActiveTrueOrderBySortOrderAscCreatedAtDesc(user.getId())).thenReturn(List.of(link));

        PublicProfileResponse profile = service.getPublicProfile("alice");

        assertThat(profile.getLinks()).extracting(PublicProfileResponse.PublicLink::getShortUrl).containsExactly("abc");
        assertThat(profile.getUpiId()).isNull();
        verify(cache).put(eq("profile:v2:alice"), any(), any());
    }

    @Test
    void suspendedUserProfileIsNotFound() {
        user.setIsSuspended(true);
        assertThatThrownBy(() -> service.getPublicProfile("alice")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void changingPasswordRequiresCurrentPassword() {
        ChangeSettingsRequest request = new ChangeSettingsRequest();
        request.setNewPassword("brand-new-password");
        request.setCurrentPassword("wrong");
        assertThatThrownBy(() -> service.updateSettings("alice", request)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void passwordChangeRevokesOldTokensAndReturnsANewOne() {
        String oldToken = jwtUtil.generateToken(user);
        ChangeSettingsRequest request = new ChangeSettingsRequest();
        request.setCurrentPassword("password123");
        request.setNewPassword("brand-new-password");

        AuthResponse response = service.updateSettings("alice", request);

        assertThat(encoder.matches("brand-new-password", user.getPassword())).isTrue();
        assertThat(JwtUtil.tokenVersion(jwtUtil.parse(oldToken))).isNotEqualTo(user.getTokenVersion());
        assertThat(JwtUtil.tokenVersion(jwtUtil.parse(response.getToken()))).isEqualTo(user.getTokenVersion());
    }

    @Test
    void adminCannotBeSuspendedOrDeleted() {
        user.setRole(Role.ROLE_ADMIN);
        assertThatThrownBy(() -> service.toggleSuspendUser("alice")).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> service.deleteUserByAdmin("alice")).isInstanceOf(ForbiddenException.class);
        verify(userRepository, never()).hardDelete(any());
    }

    @Test
    void suspendingLogsTheUserOut() {
        int before = user.getTokenVersion();
        service.toggleSuspendUser("alice");
        assertThat(user.suspended()).isTrue();
        assertThat(user.getTokenVersion()).isEqualTo(before + 1);
    }
}
