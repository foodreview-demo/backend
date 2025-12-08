package com.foodreview.domain.auth.service;

import com.foodreview.domain.auth.dto.AuthDto;
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

    public AuthDto.TokenResponse login(AuthDto.LoginRequest request) {
        // 인증 수행
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        // 토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(request.getEmail());
        String refreshToken = jwtTokenProvider.createRefreshToken(request.getEmail());

        return AuthDto.TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtTokenProvider.getAccessTokenExpiration())
                .build();
    }

    public AuthDto.TokenResponse refresh(AuthDto.RefreshRequest request) {
        if (!jwtTokenProvider.validateToken(request.getRefreshToken())) {
            throw new CustomException("유효하지 않은 리프레시 토큰입니다", HttpStatus.UNAUTHORIZED, "INVALID_TOKEN");
        }

        String email = jwtTokenProvider.getEmail(request.getRefreshToken());

        // 사용자 존재 확인
        if (!userRepository.existsByEmail(email)) {
            throw new CustomException("사용자를 찾을 수 없습니다", HttpStatus.NOT_FOUND, "USER_NOT_FOUND");
        }

        String accessToken = jwtTokenProvider.createAccessToken(email);
        String refreshToken = jwtTokenProvider.createRefreshToken(email);

        return AuthDto.TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtTokenProvider.getAccessTokenExpiration())
                .build();
    }
}
