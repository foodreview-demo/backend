package com.foodreview.domain.recommendation.entity;

import com.foodreview.domain.common.BaseTimeEntity;
import com.foodreview.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

/**
 * 사용자 취향 프로필 (AI 추천용 캐싱)
 * - 배치 Job으로 매일 갱신
 * - 리뷰 히스토리 기반 선호도 분석 결과 저장
 */
@Entity
@Table(name = "user_taste_profiles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserTasteProfile extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /**
     * 선호 카테고리 Top 3 (JSON)
     * 예: ["KOREAN", "JAPANESE", "CAFE"]
     */
    @Column(name = "preferred_categories", columnDefinition = "TEXT")
    private String preferredCategories;

    /**
     * 비선호 카테고리 (JSON)
     * 예: ["CHINESE"]
     */
    @Column(name = "disliked_categories", columnDefinition = "TEXT")
    private String dislikedCategories;

    /**
     * 평균 평점
     */
    @Column(name = "avg_rating")
    private Double avgRating;

    /**
     * 총 리뷰 수
     */
    @Column(name = "total_reviews")
    @Builder.Default
    private Integer totalReviews = 0;

    /**
     * 시간대별 선호 (JSON)
     * 예: {"morning": ["CAFE", "BAKERY"], "lunch": ["KOREAN", "JAPANESE"], "dinner": ["KOREAN", "WESTERN"], "lateNight": ["SNACK"]}
     */
    @Column(name = "time_slot_preferences", columnDefinition = "TEXT")
    private String timeSlotPreferences;

    /**
     * 요일+시간대별 선호 (JSON)
     * 예: {"fri_dinner": ["KOREAN", "WESTERN"], "sat_morning": ["CAFE"], "sat_lunch": ["KOREAN"]}
     */
    @Column(name = "day_time_preferences", columnDefinition = "TEXT")
    private String dayTimePreferences;

    /**
     * 연속 패턴 - 이전 식사 → 다음 식사 예측 (JSON)
     * 예: {"고기집": ["해장국", "짬뽕"], "술집": ["해장국", "국밥"]}
     */
    @Column(name = "sequence_patterns", columnDefinition = "TEXT")
    private String sequencePatterns;

    /**
     * 자주 언급하는 키워드 (JSON)
     * 예: ["맛있어요", "가성비", "분위기"]
     */
    @Column(name = "keywords", columnDefinition = "TEXT")
    private String keywords;

    /**
     * 최근 방문 음식점 ID 목록 (JSON)
     * 예: [123, 456, 789]
     */
    @Column(name = "recent_restaurants", columnDefinition = "TEXT")
    private String recentRestaurants;

    /**
     * 가격대 선호 (low, mid, high)
     */
    @Column(name = "price_preference", length = 10)
    private String pricePreference;

    /**
     * 프로필 업데이트
     */
    public void updateProfile(String preferredCategories, String dislikedCategories,
                              Double avgRating, Integer totalReviews,
                              String timeSlotPreferences, String dayTimePreferences,
                              String sequencePatterns, String keywords,
                              String recentRestaurants, String pricePreference) {
        this.preferredCategories = preferredCategories;
        this.dislikedCategories = dislikedCategories;
        this.avgRating = avgRating;
        this.totalReviews = totalReviews;
        this.timeSlotPreferences = timeSlotPreferences;
        this.dayTimePreferences = dayTimePreferences;
        this.sequencePatterns = sequencePatterns;
        this.keywords = keywords;
        this.recentRestaurants = recentRestaurants;
        this.pricePreference = pricePreference;
    }
}
