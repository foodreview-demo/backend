package com.foodreview.domain.notification.repository;

import com.foodreview.domain.notification.entity.FcmToken;
import com.foodreview.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {

    Optional<FcmToken> findByToken(String token);

    List<FcmToken> findByUserAndIsActiveTrue(User user);

    List<FcmToken> findByUserIdAndIsActiveTrue(Long userId);

    @Query("SELECT f.token FROM FcmToken f WHERE f.user.id = :userId AND f.isActive = true")
    List<String> findActiveTokensByUserId(@Param("userId") Long userId);

    @Query("SELECT f.token FROM FcmToken f WHERE f.user.id IN :userIds AND f.isActive = true")
    List<String> findActiveTokensByUserIds(@Param("userIds") List<Long> userIds);

    @Modifying
    @Query("UPDATE FcmToken f SET f.isActive = false WHERE f.token = :token")
    void deactivateByToken(@Param("token") String token);

    @Modifying
    @Query("DELETE FROM FcmToken f WHERE f.user = :user")
    void deleteByUser(@Param("user") User user);

    boolean existsByToken(String token);
}
