package com.foodreview.domain.auth.repository;

import com.foodreview.domain.auth.entity.RefreshToken;
import com.foodreview.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    Optional<RefreshToken> findByTokenAndRevokedFalse(String token);

    List<RefreshToken> findByUserAndRevokedFalse(User user);

    Optional<RefreshToken> findByUserAndDeviceIdAndRevokedFalse(User user, String deviceId);

    // 사용자의 모든 토큰 무효화 (로그아웃 전체 기기)
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user = :user AND rt.revoked = false")
    int revokeAllByUser(@Param("user") User user);

    // 특정 기기의 토큰만 무효화
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user = :user AND rt.deviceId = :deviceId AND rt.revoked = false")
    int revokeByUserAndDeviceId(@Param("user") User user, @Param("deviceId") String deviceId);

    // 특정 토큰 무효화
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.token = :token")
    int revokeByToken(@Param("token") String token);

    // 만료된 토큰 삭제 (정리용)
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now OR rt.revoked = true")
    int deleteExpiredAndRevoked(@Param("now") LocalDateTime now);

    // 사용자의 활성 세션 수
    long countByUserAndRevokedFalse(User user);
}
