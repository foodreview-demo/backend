package com.foodreview.domain.recommendation.repository;

import com.foodreview.domain.recommendation.entity.AiRecommendationCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AiRecommendationCacheRepository extends JpaRepository<AiRecommendationCache, Long> {

    /**
     * 오늘의 추천 조회
     */
    Optional<AiRecommendationCache> findByUserIdAndRecommendationDateAndRecommendationType(
            Long userId, LocalDate date, String type);

    /**
     * 사용자의 추천 히스토리 조회
     */
    List<AiRecommendationCache> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 특정 기간 내 추천 조회
     */
    List<AiRecommendationCache> findByUserIdAndRecommendationDateBetweenOrderByCreatedAtDesc(
            Long userId, LocalDate startDate, LocalDate endDate);

    /**
     * 오래된 캐시 삭제 (30일 이전)
     */
    void deleteByRecommendationDateBefore(LocalDate date);
}
