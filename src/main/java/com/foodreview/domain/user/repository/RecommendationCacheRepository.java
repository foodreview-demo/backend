package com.foodreview.domain.user.repository;

import com.foodreview.domain.user.entity.RecommendationCache;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RecommendationCacheRepository extends JpaRepository<RecommendationCache, Long> {

    // 특정 사용자의 추천 목록 조회 (점수 높은 순)
    @Query("SELECT rc FROM RecommendationCache rc WHERE rc.userId = :userId ORDER BY rc.totalScore DESC")
    List<RecommendationCache> findByUserIdOrderByTotalScoreDesc(@Param("userId") Long userId, Pageable pageable);

    // 특정 사용자 쌍의 추천 캐시 조회
    Optional<RecommendationCache> findByUserIdAndRecommendedUserId(Long userId, Long recommendedUserId);

    // 특정 사용자의 모든 추천 캐시 삭제
    @Modifying
    @Query("DELETE FROM RecommendationCache rc WHERE rc.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    // 특정 사용자가 추천 대상인 캐시 삭제 (탈퇴 시)
    @Modifying
    @Query("DELETE FROM RecommendationCache rc WHERE rc.recommendedUserId = :userId")
    void deleteByRecommendedUserId(@Param("userId") Long userId);

    // 특정 사용자의 추천 캐시 존재 여부
    boolean existsByUserId(Long userId);

    // 차단된 사용자 제외하고 조회
    @Query("SELECT rc FROM RecommendationCache rc " +
           "WHERE rc.userId = :userId " +
           "AND rc.recommendedUserId NOT IN :blockedIds " +
           "ORDER BY rc.totalScore DESC")
    List<RecommendationCache> findByUserIdExcludingBlocked(
            @Param("userId") Long userId,
            @Param("blockedIds") List<Long> blockedIds,
            Pageable pageable);
}
