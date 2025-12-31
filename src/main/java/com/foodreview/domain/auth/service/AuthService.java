package com.foodreview.domain.auth.service;

import com.foodreview.domain.auth.dto.AuthDto;
import com.foodreview.domain.auth.entity.RefreshToken;
import com.foodreview.domain.user.dto.UserDto;
import com.foodreview.domain.user.entity.User;
import com.foodreview.domain.user.repository.UserRepository;
import com.foodreview.global.exception.CustomException;
import com.foodreview.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public UserDto.Response signUp(AuthDto.SignUpRequest request) {
        // 이메일 중복 확인
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new CustomException("이미 사용 중인 이메일입니다", HttpStatus.CONFLICT, "EMAIL_DUPLICATE");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .region(request.getRegion())
                .build();

        User savedUser = userRepository.save(user);

        Integer rank = userRepository.findRankByRegionAndScore(savedUser.getRegion(), savedUser.getTasteScore());
        return UserDto.Response.from(savedUser, rank);
    }

    @Transactional
    public AuthDto.TokenResponse login(AuthDto.LoginRequest request, String deviceId, String userAgent, String ipAddress) {
        // 인증 수행
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomException("사용자를 찾을 수 없습니다", HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));

        // Access Token 생성 (JWT)
        String accessToken = jwtTokenProvider.createAccessToken(request.getEmail());

        // Refresh Token 생성 및 DB 저장
        String refreshToken = refreshTokenService.createRefreshToken(user, deviceId, userAgent, ipAddress);

        return AuthDto.TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtTokenProvider.getAccessTokenExpiration())
                .build();
    }

    @Transactional
    public AuthDto.TokenResponse refresh(AuthDto.RefreshRequest request, String deviceId, String userAgent, String ipAddress) {
        // Refresh Token 검증 및 Rotation
        RefreshToken newRefreshToken = refreshTokenService.validateAndRotate(
                request.getRefreshToken(), deviceId, userAgent, ipAddress);

        User user = newRefreshToken.getUser();

        // 새 Access Token 발급
        String accessToken = jwtTokenProvider.createAccessToken(user.getEmail());

        return AuthDto.TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(newRefreshToken.getToken())
                .expiresIn(jwtTokenProvider.getAccessTokenExpiration())
                .build();
    }

    @Transactional
    public void logout(String email, String refreshToken, boolean allDevices) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException("사용자를 찾을 수 없습니다", HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));

        if (allDevices) {
            // 모든 기기에서 로그아웃
            refreshTokenService.revokeAllUserTokens(user);
        } else {
            // 현재 기기만 로그아웃
            refreshTokenService.revokeToken(refreshToken);
        }
    }
}
