package com.foodreview.domain.user.entity;

import com.foodreview.domain.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "recommendation_cache",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "recommended_user_id"})
    },
    indexes = {
        @Index(name = "idx_recommendation_user_score", columnList = "user_id, total_score DESC")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class RecommendationCache extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "recommended_user_id", nullable = false)
    private Long recommendedUserId;

    // 총점
    @Column(name = "total_score", nullable = false)
    private int totalScore;

    // 2촌 관계 점수
    @Column(name = "second_degree_score")
    private int secondDegreeScore;

    @Column(name = "second_degree_count")
    private int secondDegreeCount;

    // 공감 점수
    @Column(name = "outgoing_sympathy_score")
    private int outgoingSympathyScore;

    @Column(name = "outgoing_sympathy_count")
    private int outgoingSympathyCount;

    @Column(name = "incoming_sympathy_score")
    private int incomingSympathyScore;

    @Column(name = "incoming_sympathy_count")
    private int incomingSympathyCount;

    // 취향 유사도 점수
    @Column(name = "taste_score")
    private int tasteScore;

    @Column(name = "taste_similarity")
    private double tasteSimilarity;

    @Column(name = "common_restaurant_count")
    private int commonRestaurantCount;

    // 기본 점수
    @Column(name = "base_score")
    private int baseScore;

    // 추천 이유
    @Column(name = "reason", length = 200)
    private String reason;

    // 공통 카테고리 (콤마 구분)
    @Column(name = "common_categories", length = 500)
    private String commonCategories;

    // 계산 시점
    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt;

    public void update(int totalScore, int secondDegreeScore, int secondDegreeCount,
                       int outgoingSympathyScore, int outgoingSympathyCount,
                       int incomingSympathyScore, int incomingSympathyCount,
                       int tasteScore, double tasteSimilarity, int commonRestaurantCount,
                       int baseScore, String reason, String commonCategories,
                       LocalDateTime calculatedAt) {
        this.totalScore = totalScore;
        this.secondDegreeScore = secondDegreeScore;
        this.secondDegreeCount = secondDegreeCount;
        this.outgoingSympathyScore = outgoingSympathyScore;
        this.outgoingSympathyCount = outgoingSympathyCount;
        this.incomingSympathyScore = incomingSympathyScore;
        this.incomingSympathyCount = incomingSympathyCount;
        this.tasteScore = tasteScore;
        this.tasteSimilarity = tasteSimilarity;
        this.commonRestaurantCount = commonRestaurantCount;
        this.baseScore = baseScore;
        this.reason = reason;
        this.commonCategories = commonCategories;
        this.calculatedAt = calculatedAt;
    }
}
