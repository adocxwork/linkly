package com.gupta.linkly.service;

import com.gupta.linkly.dto.AuthResponse;
import com.gupta.linkly.dto.LoginRequest;
import com.gupta.linkly.dto.RegisterRequest;
import com.gupta.linkly.dto.UserProfileResponse;
import com.gupta.linkly.entity.Role;
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
    private final com.gupta.linkly.repository.PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final org.springframework.core.env.Environment env;

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
                .role(Role.ROLE_USER)
                .upiId(request.getUpiId())
                .enableUpiPayment(request.getEnableUpiPayment() != null ? request.getEnableUpiPayment() : false)
                .enablePublicMessaging(request.getEnablePublicMessaging() != null ? request.getEnablePublicMessaging() : false)
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
                .upiId(user.getUpiId())
                .enableUpiPayment(user.getEnableUpiPayment())
                .enablePublicMessaging(user.getEnablePublicMessaging())
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
                .upiId(user.getUpiId())
                .enableUpiPayment(user.getEnableUpiPayment())
                .enablePublicMessaging(user.getEnablePublicMessaging())
                .build();

        return new AuthResponse(token, userProfile);
    }

    @org.springframework.transaction.annotation.Transactional
    public void forgotPassword(String identifier) {
        User user = null;
        if (identifier.contains("@")) {
            user = userRepository.findByEmail(identifier).orElse(null);
        } else {
            user = userRepository.findByUsername(identifier).orElse(null);
        }

        if (user != null) {
            tokenRepository.deleteByUser(user); // Remove old tokens
            
            String tokenValue = java.util.UUID.randomUUID().toString();
            com.gupta.linkly.entity.PasswordResetToken token = com.gupta.linkly.entity.PasswordResetToken.builder()
                    .token(tokenValue)
                    .user(user)
                    .expiryDate(java.time.LocalDateTime.now().plusMinutes(30))
                    .build();
            
            tokenRepository.save(token);
            
            String baseUrl = env.getProperty("VITE_BACKEND_URL", "http://localhost:5173");
            String resetLink = baseUrl + "/reset-password?token=" + tokenValue;
            
            emailService.sendPasswordResetEmail(user.getEmail(), resetLink);
        }
    }

    @org.springframework.transaction.annotation.Transactional
    public void resetPassword(String token, String newPassword) {
        com.gupta.linkly.entity.PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid token"));
        
        if (resetToken.getExpiryDate().isBefore(java.time.LocalDateTime.now())) {
            tokenRepository.delete(resetToken);
            throw new IllegalArgumentException("Token has expired");
        }
        
        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        tokenRepository.delete(resetToken);
    }
}
