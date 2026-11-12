package com.gupta.linkly.service;

import com.gupta.linkly.dto.*;
import com.gupta.linkly.entity.User;
import com.gupta.linkly.exception.BadRequestException;
import com.gupta.linkly.exception.DuplicateResourceException;
import com.gupta.linkly.exception.ForbiddenException;
import com.gupta.linkly.exception.ResourceNotFoundException;
import com.gupta.linkly.redis.CacheKeys;
import com.gupta.linkly.redis.RedisJsonCache;
import com.gupta.linkly.repository.LinkRepository;
import com.gupta.linkly.repository.UserRepository;
import com.gupta.linkly.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final LinkRepository linkRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RedisJsonCache cache;

    public UserProfileResponse getProfile(String username) {
        return UserProfileResponse.from(getUser(username));
    }

    @Transactional
    public UserProfileResponse updateProfile(String username, UpdateProfileRequest request) {
        User user = getUser(username);
        user.setName(request.getName().trim());
        user.setBio(AuthService.blankToNull(request.getBio()));
        evictProfile(username);
        return UserProfileResponse.from(user);
    }

    /**
     * Public creator page. Cache-aside: try Redis, otherwise build from PostgreSQL and cache it.
     * If Redis is down this silently becomes a database read.
     *
     * <p>NOT_SUPPORTED: no surrounding transaction, so a cache hit never borrows a database
     * connection from the pool. The repository calls on a miss run in their own short transactions.
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    public PublicProfileResponse getPublicProfile(String username) {
        String key = CacheKeys.profile(username);
        return cache.get(key, PublicProfileResponse.class).orElseGet(() -> {
            PublicProfileResponse profile = buildPublicProfile(username);
            cache.put(key, profile, CacheKeys.PROFILE_TTL);
            return profile;
        });
    }

    private PublicProfileResponse buildPublicProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.suspended()) {
            throw new ResourceNotFoundException("User not found");
        }
        List<PublicProfileResponse.PublicLink> links = linkRepository
                .findByUserIdAndActiveTrueOrderBySortOrderAscCreatedAtDesc(user.getId()).stream()
                .map(l -> new PublicProfileResponse.PublicLink(l.getId(), l.getTitle(), l.getShortUrl()))
                .toList();
        boolean upiOn = Boolean.TRUE.equals(user.getEnableUpiPayment()) && user.getUpiId() != null;
        return PublicProfileResponse.builder()
                .name(user.getName())
                .username(user.getUsername())
                .bio(user.getBio())
                .links(new ArrayList<>(links))
                .upiId(upiOn ? user.getUpiId() : null)
                .enableUpiPayment(upiOn)
                .enablePublicMessaging(Boolean.TRUE.equals(user.getEnablePublicMessaging()))
                .build();
    }

    /**
     * Partial settings update. Changing the email or password requires the current password, and
     * a password change revokes every existing session (token version bump). A fresh token for
     * the current session is returned so the user stays logged in here.
     */
    @Transactional
    public AuthResponse updateSettings(String username, ChangeSettingsRequest request) {
        User user = getUser(username);

        String newEmail = AuthService.blankToNull(request.getEmail());
        if (newEmail != null) {
            newEmail = newEmail.toLowerCase(Locale.ROOT);
        }
        boolean emailChanging = newEmail != null && !newEmail.equalsIgnoreCase(user.getEmail());
        String newPassword = AuthService.blankToNull(request.getNewPassword());
        boolean passwordChanging = newPassword != null;

        if (emailChanging || passwordChanging) {
            String current = request.getCurrentPassword();
            if (current == null || !passwordEncoder.matches(current, user.getPassword())) {
                throw new BadRequestException("Current password is incorrect");
            }
        }
        if (emailChanging) {
            if (userRepository.existsByEmailIgnoreCase(newEmail)) {
                throw new DuplicateResourceException("Email is already in use");
            }
            user.setEmail(newEmail);
        }
        if (passwordChanging) {
            user.setPassword(passwordEncoder.encode(newPassword));
            user.bumpTokenVersion();
        }
        if (request.getName() != null && !request.getName().isBlank()) {
            user.setName(request.getName().trim());
        }
        if (request.getBio() != null) {
            user.setBio(AuthService.blankToNull(request.getBio()));
        }
        if (request.getUpiId() != null) {
            user.setUpiId(AuthService.blankToNull(request.getUpiId()));
        }
        if (request.getEnableUpiPayment() != null) {
            if (request.getEnableUpiPayment() && user.getUpiId() == null) {
                throw new BadRequestException("Add a UPI ID before enabling UPI payments");
            }
            user.setEnableUpiPayment(request.getEnableUpiPayment());
        }
        if (request.getEnablePublicMessaging() != null) {
            user.setEnablePublicMessaging(request.getEnablePublicMessaging());
        }
        userRepository.saveAndFlush(user); // flush so the token below carries the new version
        evictProfile(username);
        return new AuthResponse(jwtUtil.generateToken(user), UserProfileResponse.from(user));
    }

    /** "Log out of all devices": every token issued so far stops working. */
    @Transactional
    public void revokeAllSessions(String username) {
        getUser(username).bumpTokenVersion();
    }

    @Transactional
    public void deleteAccount(String username) {
        User user = getUser(username);
        if (user.isAdmin()) {
            throw new ForbiddenException("The admin account cannot be deleted");
        }
        deleteUserAndCaches(user);
    }

    public List<UserProfileResponse> getAllUsers() {
        return userRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(UserProfileResponse::from)
                .toList();
    }

    @Transactional
    public UserProfileResponse toggleSuspendUser(String username) {
        User user = getUser(username);
        if (user.isAdmin()) {
            throw new ForbiddenException("Cannot suspend an admin");
        }
        user.setIsSuspended(!user.suspended());
        user.bumpTokenVersion(); // suspended users are logged out immediately
        evictUserCaches(user);
        return UserProfileResponse.from(user);
    }

    @Transactional
    public void deleteUserByAdmin(String username) {
        User user = getUser(username);
        if (user.isAdmin()) {
            throw new ForbiddenException("Cannot delete an admin");
        }
        deleteUserAndCaches(user);
    }

    private void deleteUserAndCaches(User user) {
        evictUserCaches(user); // collect keys before the rows are gone
        userRepository.hardDelete(user.getId());
    }

    private void evictUserCaches(User user) {
        List<String> keys = new ArrayList<>(CacheKeys.redirects(linkRepository.findShortUrlsByUserId(user.getId())));
        keys.add(CacheKeys.profile(user.getUsername()));
        cache.evictAfterCommit(keys);
    }

    private void evictProfile(String username) {
        cache.evictAfterCommit(List.of(CacheKeys.profile(username)));
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
