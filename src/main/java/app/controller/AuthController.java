package app.controller;

import app.dto.Auth.*;
import app.entity.RefreshToken;
import app.entity.User;
import app.repository.UserRepository;

import app.security.JwtUtil;
import app.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;

    /** POST /auth/register */
    @PostMapping("/register")
    public ResponseEntity<MessageResponse> register(@RequestBody RegisterRequest request) {
        if (userRepository.findByUsername(request.username()).isPresent()) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Username already taken"));
        }

        User user = User.builder()
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .roles(Set.of("ROLE_USER"))
                .build();

        userRepository.save(user);
        return ResponseEntity.ok(new MessageResponse("User registered successfully"));
    }

    /** POST /auth/login */
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.username());
        User user = userRepository.findByUsername(request.username()).orElseThrow();

        String accessToken = jwtUtil.generateAccessToken(userDetails);
        RefreshToken refreshToken = refreshTokenService.create(user);

        return ResponseEntity.ok(new TokenResponse(accessToken, refreshToken.getToken()));
    }

    /** POST /auth/refresh */
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@RequestBody RefreshRequest request) {
        RefreshToken refreshToken = refreshTokenService.verify(request.refreshToken());
        UserDetails userDetails = userDetailsService.loadUserByUsername(
                refreshToken.getUser().getUsername());

        String newAccessToken = jwtUtil.generateAccessToken(userDetails);

        // Rotate refresh token
        RefreshToken newRefreshToken = refreshTokenService.create(refreshToken.getUser());

        return ResponseEntity.ok(new TokenResponse(newAccessToken, newRefreshToken.getToken()));
    }

    /** POST /auth/logout */
    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(@RequestBody RefreshRequest request) {
        RefreshToken token = refreshTokenService.verify(request.refreshToken());
        refreshTokenService.revokeByUser(token.getUser());
        return ResponseEntity.ok(new MessageResponse("Logged out successfully"));
    }
}
