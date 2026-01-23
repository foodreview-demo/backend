package com.foodreview.domain.badge.repository;

import com.foodreview.domain.badge.entity.UserBadge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UserBadgeRepository extends JpaRepository<UserBadge, Long> {

    // 사용자의 모든 배지 조회
    @Query("SELECT ub FROM UserBadge ub JOIN FETCH ub.badge WHERE ub.user.id = :userId ORDER BY ub.acquiredAt DESC")
    List<UserBadge> findByUserIdWithBadge(@Param("userId") Long userId);

    // 사용자가 특정 배지를 보유하는지 확인
    boolean existsByUserIdAndBadgeId(Long userId, Long badgeId);

    // 사용자가 보유한 배지 ID 목록
    @Query("SELECT ub.badge.id FROM UserBadge ub WHERE ub.user.id = :userId")
    Set<Long> findBadgeIdsByUserId(@Param("userId") Long userId);

    // 사용자의 표시 중인 배지 조회
    @Query("SELECT ub FROM UserBadge ub JOIN FETCH ub.badge WHERE ub.user.id = :userId AND ub.isDisplayed = true")
    List<UserBadge> findDisplayedBadgesByUserId(@Param("userId") Long userId);

    // 특정 사용자-배지 조합 조회
    Optional<UserBadge> findByUserIdAndBadgeId(Long userId, Long badgeId);

    // 사용자의 최근 획득 배지 조회
    @Query("SELECT ub FROM UserBadge ub JOIN FETCH ub.badge WHERE ub.user.id = :userId ORDER BY ub.acquiredAt DESC LIMIT :limit")
    List<UserBadge> findRecentBadgesByUserId(@Param("userId") Long userId, @Param("limit") int limit);

    // 배지별 획득자 수
    @Query("SELECT COUNT(ub) FROM UserBadge ub WHERE ub.badge.id = :badgeId")
    Long countByBadgeId(@Param("badgeId") Long badgeId);
}
