package com.foodreview.domain.auth.service;

import com.foodreview.domain.auth.entity.RefreshToken;
import com.foodreview.domain.auth.repository.RefreshTokenRepository;
import com.foodreview.domain.user.entity.User;
import com.foodreview.global.exception.CustomException;
import com.foodreview.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${jwt.refresh-expiration}")
    private long refreshTokenExpirationMs;

    private static final int MAX_SESSIONS_PER_USER = 5; // 사용자당 최대 세션 수

    /**
     * 새 Refresh Token 생성 및 저장
     */
    @Transactional
    public String createRefreshToken(User user, String deviceId, String userAgent, String ipAddress) {
        // 동일 기기의 기존 토큰 무효화
        refreshTokenRepository.revokeByUserAndDeviceId(user, deviceId);

        // 최대 세션 수 체크 - 초과 시 가장 오래된 세션 무효화
        long activeSessionCount = refreshTokenRepository.countByUserAndRevokedFalse(user);
        if (activeSessionCount >= MAX_SESSIONS_PER_USER) {
            // 가장 오래된 세션 찾아서 무효화
            refreshTokenRepository.findByUserAndRevokedFalse(user).stream()
                    .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                    .limit(activeSessionCount - MAX_SESSIONS_PER_USER + 1)
                    .forEach(RefreshToken::revoke);
        }

        // 새 토큰 생성
        String token = generateSecureToken();
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(refreshTokenExpirationMs / 1000);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(token)
                .deviceId(deviceId)
                .userAgent(userAgent)
                .ipAddress(ipAddress)
                .expiresAt(expiresAt)
                .build();

        refreshTokenRepository.save(refreshToken);

        return token;
    }

    /**
     * Refresh Token 검증 및 Rotation
     * @return 새로운 Refresh Token (rotation)
     */
    @Transactional
    public RefreshToken validateAndRotate(String token, String deviceId, String userAgent, String ipAddress) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new CustomException("유효하지 않은 리프레시 토큰입니다", HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN"));

        // 이미 사용된 토큰인지 체크 (토큰 탈취 감지)
        if (refreshToken.getRevoked()) {
            // 보안 위협 감지: 이미 무효화된 토큰이 재사용됨
            log.warn("Security alert: Revoked refresh token reuse detected for user: {}, device: {}, IP: {}",
                    refreshToken.getUser().getEmail(), deviceId, ipAddress);

            // 해당 사용자의 모든 세션 무효화 (의심스러운 활동)
            refreshTokenRepository.revokeAllByUser(refreshToken.getUser());
            throw new CustomException("보안상의 이유로 모든 세션이 종료되었습니다. 다시 로그인해주세요.",
                    HttpStatus.UNAUTHORIZED, "TOKEN_REUSE_DETECTED");
        }

        // 만료 체크
        if (refreshToken.isExpired()) {
            refreshToken.revoke();
            throw new CustomException("리프레시 토큰이 만료되었습니다. 다시 로그인해주세요.",
                    HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_EXPIRED");
        }

        // 기기 ID 불일치 체크 (선택적 - 엄격한 보안)
        if (!refreshToken.getDeviceId().equals(deviceId)) {
            log.warn("Device ID mismatch for token refresh. Original: {}, Current: {}",
                    refreshToken.getDeviceId(), deviceId);
            // 주의: 이 체크를 활성화하면 기기 변경 시 재로그인 필요
            // 필요에 따라 주석 처리 가능
        }

        // 현재 토큰 무효화 (Rotation)
        refreshToken.revoke();
        refreshToken.updateLastUsed();

        // 새 토큰 생성
        String newToken = generateSecureToken();
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(refreshTokenExpirationMs / 1000);

        RefreshToken newRefreshToken = RefreshToken.builder()
                .user(refreshToken.getUser())
                .token(newToken)
                .deviceId(deviceId)
                .userAgent(userAgent)
                .ipAddress(ipAddress)
                .expiresAt(expiresAt)
                .build();

        return refreshTokenRepository.save(newRefreshToken);
    }

    /**
     * 특정 사용자의 모든 세션 로그아웃
     */
    @Transactional
    public void revokeAllUserTokens(User user) {
        refreshTokenRepository.revokeAllByUser(user);
    }

    /**
     * 특정 기기만 로그아웃
     */
    @Transactional
    public void revokeByDeviceId(User user, String deviceId) {
        refreshTokenRepository.revokeByUserAndDeviceId(user, deviceId);
    }

    /**
     * 특정 토큰 무효화
     */
    @Transactional
    public void revokeToken(String token) {
        refreshTokenRepository.revokeByToken(token);
    }

    /**
     * 만료 및 무효화된 토큰 정리 (매일 새벽 3시)
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        refreshTokenRepository.deleteExpiredAndRevoked(LocalDateTime.now());
    }

    /**
     * 보안성 높은 랜덤 토큰 생성
     */
    private String generateSecureToken() {
        return UUID.randomUUID() + "-" + UUID.randomUUID();
    }
}
