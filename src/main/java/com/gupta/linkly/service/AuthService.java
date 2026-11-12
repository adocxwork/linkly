package com.gupta.linkly.service;

import com.gupta.linkly.dto.AuthResponse;
import com.gupta.linkly.dto.LoginRequest;
import com.gupta.linkly.dto.RegisterRequest;
import com.gupta.linkly.dto.UserProfileResponse;
import com.gupta.linkly.entity.Role;
import com.gupta.linkly.entity.User;
import com.gupta.linkly.exception.BadRequestException;
import com.gupta.linkly.exception.DuplicateResourceException;
import com.gupta.linkly.repository.UserRepository;
import com.gupta.linkly.security.CustomUserDetails;
import com.gupta.linkly.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    /** Usernames that would clash with app routes or look official. */
    private static final Set<String> RESERVED_USERNAMES = Set.of(
            "admin", "administrator", "api", "r", "p", "login", "register", "dashboard", "settings",
            "inbox", "link-error", "support", "linkly", "root", "system");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String username = request.getUsername().trim();
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);

        if (RESERVED_USERNAMES.contains(username.toLowerCase(Locale.ROOT))) {
            throw new BadRequestException("This username is reserved");
        }
        // Case-insensitive: "Alice" and "alice" would be indistinguishable to visitors.
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new DuplicateResourceException("Username is already taken");
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("Email is already in use");
        }

        String upiId = blankToNull(request.getUpiId());
        User user = User.builder()
                .name(request.getName().trim())
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.ROLE_USER)
                .upiId(upiId)
                .enableUpiPayment(Boolean.TRUE.equals(request.getEnableUpiPayment()) && upiId != null)
                .enablePublicMessaging(Boolean.TRUE.equals(request.getEnablePublicMessaging()))
                .build();
        userRepository.save(user);
        return new AuthResponse(jwtUtil.generateToken(user), UserProfileResponse.from(user));
    }

    public AuthResponse login(LoginRequest request) {
        // Throws BadCredentialsException / LockedException, mapped to 401 / 403 by the handler.
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getIdentifier().trim(), request.getPassword()));
        User user = ((CustomUserDetails) authentication.getPrincipal()).getUser();
        return new AuthResponse(jwtUtil.generateToken(user), UserProfileResponse.from(user));
    }

    static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
