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
                .build();
    }
}
