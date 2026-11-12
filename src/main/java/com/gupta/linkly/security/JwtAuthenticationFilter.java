package com.gupta.linkly.security;

import com.gupta.linkly.entity.User;
import com.gupta.linkly.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Authenticates requests carrying {@code Authorization: Bearer <jwt>}.
 *
 * <p>Only the header is accepted (no cookies). Because the browser never attaches the token
 * automatically, cross-site request forgery (CSRF) is not possible, which is why CSRF protection
 * can safely stay disabled.
 *
 * <p>Invalid, expired or revoked tokens simply leave the request unauthenticated; Spring Security
 * then returns 401 for protected endpoints, while public endpoints keep working.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final SecurityErrorWriter errorWriter;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ") || SecurityContextHolder.getContext().getAuthentication() != null) {
            chain.doFilter(request, response);
            return;
        }

        Claims claims;
        try {
            claims = jwtUtil.parse(header.substring(7).trim());
        } catch (JwtException | IllegalArgumentException e) {
            chain.doFilter(request, response);
            return;
        }

        Optional<User> user = userRepository.findByUsername(claims.getSubject());
        if (user.isEmpty() || JwtUtil.tokenVersion(claims) != user.get().getTokenVersion()) {
            chain.doFilter(request, response); // deleted user, or token revoked
            return;
        }
        if (user.get().suspended()) {
            errorWriter.write(request, response, HttpServletResponse.SC_UNAUTHORIZED, "Your account has been suspended");
            return;
        }

        CustomUserDetails details = new CustomUserDetails(user.get());
        var authentication = new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        chain.doFilter(request, response);
    }
}
