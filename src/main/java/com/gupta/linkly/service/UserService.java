package com.gupta.linkly.service;

import com.gupta.linkly.dto.LinkResponse;
import com.gupta.linkly.dto.PublicProfileResponse;
import com.gupta.linkly.dto.UpdateProfileRequest;
import com.gupta.linkly.dto.UserProfileResponse;
import com.gupta.linkly.entity.User;
import com.gupta.linkly.exception.ResourceNotFoundException;
import com.gupta.linkly.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    public UserProfileResponse getProfile(String username) {
        User user = getUserByUsername(username);
        return mapToUserProfileResponse(user);
    }

    public UserProfileResponse updateProfile(String username, UpdateProfileRequest request) {
        User user = getUserByUsername(username);
        user.setName(request.getName());
        user.setBio(request.getBio());
        userRepository.save(user);
        return mapToUserProfileResponse(user);
    }

    public PublicProfileResponse getPublicProfile(String username) {
        User user = getUserByUsername(username);
        List<LinkResponse> activeLinks = user.getLinks().stream()
                .filter(link -> Boolean.TRUE.equals(link.getActive()))
                .map(link -> LinkResponse.builder()
                        .id(link.getId())
                        .title(link.getTitle())
                        .originalUrl(link.getOriginalUrl())
                        .shortUrl(link.getShortUrl())
                        .active(link.getActive())
                        .clickCount(link.getClickCount())
                        .build())
                .collect(Collectors.toList());

        return PublicProfileResponse.builder()
                .name(user.getName())
                .username(user.getUsername())
                .bio(user.getBio())
                .links(activeLinks)
                .build();
    }

    public void deleteAccount(String username) {
        User user = getUserByUsername(username);
        userRepository.delete(user);
    }

    public void updateSettings(String username, com.gupta.linkly.dto.ChangeSettingsRequest request) {
        User user = getUserByUsername(username);
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            if (!user.getEmail().equals(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
                throw new com.gupta.linkly.exception.DuplicateResourceException("Email is already taken");
            }
            user.setEmail(request.getEmail());
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        userRepository.save(user);
    }

    public List<UserProfileResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToUserProfileResponse)
                .collect(Collectors.toList());
    }

    public void toggleSuspendUser(String username) {
        User user = getUserByUsername(username);
        if (user.getRole() == com.gupta.linkly.entity.Role.ROLE_ADMIN) {
            throw new IllegalArgumentException("Cannot suspend an admin");
        }
        user.setIsSuspended(!user.getIsSuspended());
        userRepository.save(user);
    }

    public void deleteUserByAdmin(String username) {
        User user = getUserByUsername(username);
        if (user.getRole() == com.gupta.linkly.entity.Role.ROLE_ADMIN) {
            throw new IllegalArgumentException("Cannot delete an admin");
        }
        userRepository.delete(user);
    }

    private User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
    }

    private UserProfileResponse mapToUserProfileResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .username(user.getUsername())
                .email(user.getEmail())
                .bio(user.getBio())
                .role(user.getRole())
                .isSuspended(user.getIsSuspended())
                .build();
    }
}
