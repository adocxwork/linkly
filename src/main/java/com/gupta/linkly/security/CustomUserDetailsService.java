package com.gupta.linkly.security;

import com.gupta.linkly.entity.User;
import com.gupta.linkly.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /** Accepts a username or an email address (login form allows both). */
    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        return findByIdentifier(identifier)
                .map(CustomUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    public Optional<User> findByIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return Optional.empty();
        }
        String value = identifier.trim();
        if (value.contains("@")) {
            return userRepository.findByEmailIgnoreCase(value);
        }
        return userRepository.findByUsernameIgnoreCase(value);
    }
}
