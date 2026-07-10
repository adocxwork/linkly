package com.gupta.linkly.service;

import com.gupta.linkly.dto.AuthResponse;
import com.gupta.linkly.dto.LoginRequest;
import com.gupta.linkly.dto.RegisterRequest;
import com.gupta.linkly.dto.UserProfileResponse;
import com.gupta.linkly.entity.User;
import com.gupta.linkly.exception.DuplicateResourceException;
import com.gupta.linkly.repository.UserRepository;
import com.gupta.linkly.security.CustomUserDetails;
import com.gupta.linkly.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email is already in use");
        }

        User user = User.builder()
                .name(request.getName())
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        userRepository.save(user);
        
        CustomUserDetails userDetails = new CustomUserDetails(user);
        String token = jwtUtil.generateToken(userDetails);

        UserProfileResponse userProfile = UserProfileResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .username(user.getUsername())
                .email(user.getEmail())
                .bio(user.getBio())
                .role(user.getRole())
                .isSuspended(user.getIsSuspended())
                .build();

        return new AuthResponse(token, userProfile);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getIdentifier(),
                        request.getPassword()
                )
        );

        User user;
        if (request.getIdentifier().contains("@")) {
            user = userRepository.findByEmail(request.getIdentifier()).orElseThrow();
        } else {
            user = userRepository.findByUsername(request.getIdentifier()).orElseThrow();
        }

        CustomUserDetails userDetails = new CustomUserDetails(user);
        String token = jwtUtil.generateToken(userDetails);

        UserProfileResponse userProfile = UserProfileResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .username(user.getUsername())
                .email(user.getEmail())
                .bio(user.getBio())
                .role(user.getRole())
                .isSuspended(user.getIsSuspended())
                .build();

        return new AuthResponse(token, userProfile);
    }
}
