package com.gupta.linkly.service;

import com.gupta.linkly.dto.*;
import com.gupta.linkly.entity.User;
import com.gupta.linkly.exception.BadRequestException;
import com.gupta.linkly.exception.DuplicateResourceException;
import com.gupta.linkly.exception.ForbiddenException;
import com.gupta.linkly.exception.ResourceNotFoundException;
import com.gupta.linkly.repository.LinkRepository;
import com.gupta.linkly.repository.UserRepository;
import com.gupta.linkly.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public UserProfileResponse getProfile(String username) {
        return UserProfileResponse.from(getUser(username));
    }

    @Transactional
    public UserProfileResponse updateProfile(String username, UpdateProfileRequest request) {
        User user = getUserForUpdate(username);
        user.setName(request.getName().trim());
        user.setBio(AuthService.blankToNull(request.getBio()));
        return UserProfileResponse.from(user);
    }

    public PublicProfileResponse getPublicProfile(String username) {
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
                .links(links)
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
        User user = getUserForUpdate(username);

        String newEmail = AuthService.blankToNull(request.getEmail());
        if (newEmail != null) {
            newEmail = newEmail.toLowerCase(Locale.ROOT);
        }
        boolean emailChanging = newEmail != null && !newEmail.equalsIgnoreCase(user.getEmail());
        String newPassword = request.getNewPassword();
        boolean passwordChanging = newPassword != null && !newPassword.isEmpty();
        if (passwordChanging) {
            AuthService.validatePassword(newPassword);
        }

        if (emailChanging || passwordChanging) {
            String current = request.getCurrentPassword();
            if (current == null || !AuthService.fitsBcrypt(current) || !passwordEncoder.matches(current, user.getPassword())) {
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
        if (user.getUpiId() == null) {
            user.setEnableUpiPayment(false);
        }
        if (request.getEnablePublicMessaging() != null) {
            user.setEnablePublicMessaging(request.getEnablePublicMessaging());
        }
        userRepository.saveAndFlush(user); // flush so the token below carries the new version
        return new AuthResponse(passwordChanging ? jwtUtil.generateToken(user) : null, UserProfileResponse.from(user));
    }

    /** "Log out of all devices": every token issued so far stops working. */
    @Transactional
    public void revokeAllSessions(String username) {
        getUserForUpdate(username).bumpTokenVersion();
    }

    @Transactional
    public void deleteAccount(String username) {
        User user = getUserForUpdate(username);
        if (user.isAdmin()) {
            throw new ForbiddenException("The admin account cannot be deleted");
        }
        userRepository.hardDelete(user.getId());
    }

    public List<UserProfileResponse> getAllUsers() {
        return userRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(UserProfileResponse::from)
                .toList();
    }

    @Transactional
    public UserProfileResponse toggleSuspendUser(String username) {
        User user = getUserForUpdate(username);
        if (user.isAdmin()) {
            throw new ForbiddenException("Cannot suspend an admin");
        }
        user.setIsSuspended(!user.suspended());
        user.bumpTokenVersion(); // suspended users are logged out immediately
        return UserProfileResponse.from(user);
    }

    @Transactional
    public void deleteUserByAdmin(String username) {
        User user = getUserForUpdate(username);
        if (user.isAdmin()) {
            throw new ForbiddenException("Cannot delete an admin");
        }
        userRepository.hardDelete(user.getId());
    }

    private User getUserForUpdate(String username) {
        return userRepository.findByUsernameForUpdate(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
