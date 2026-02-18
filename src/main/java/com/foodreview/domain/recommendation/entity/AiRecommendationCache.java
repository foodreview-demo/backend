package com.foodreview.domain.recommendation.entity;

import com.foodreview.domain.common.BaseTimeEntity;
import com.foodreview.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * AI 추천 결과 캐시
 * - 하루에 한 번 생성되어 재사용
 * - 토큰 비용 절약을 위한 캐싱
 */
@Entity
@Table(name = "ai_recommendation_caches", indexes = {
    @Index(name = "idx_ai_recommendation_user_date", columnList = "user_id, recommendation_date")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AiRecommendationCache extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 추천 날짜
     */
    @Column(name = "recommendation_date", nullable = false)
    private LocalDate recommendationDate;

    /**
     * 추천 타입 (today, ask)
     */
    @Column(name = "recommendation_type", nullable = false, length = 20)
    private String recommendationType;

    /**
     * 사용자 질문 (ask 타입일 경우)
     */
    @Column(name = "user_query", length = 500)
    private String userQuery;

    /**
     * 추천 결과 JSON
     * {
     *   "restaurants": [
     *     {"id": 123, "name": "맛집", "reason": "이유", "recommendedMenu": "추천메뉴"},
     *     ...
     *   ],
     *   "summary": "오늘의 추천 요약"
     * }
     */
    @Column(name = "recommendation_result", columnDefinition = "TEXT")
    private String recommendationResult;

    /**
     * 컨텍스트 정보 (JSON)
     * {"weather": "sunny", "temperature": 20, "timeSlot": "lunch", "dayOfWeek": "FRIDAY"}
     */
    @Column(name = "context_info", columnDefinition = "TEXT")
    private String contextInfo;

    /**
     * 사용된 토큰 수 (비용 추적)
     */
    @Column(name = "tokens_used")
    private Integer tokensUsed;

    /**
     * 사용자 피드백 (좋아요/별로예요)
     * 1: 좋아요, -1: 별로예요, null: 미응답
     */
    @Column(name = "user_feedback")
    private Integer userFeedback;

    public void updateFeedback(Integer feedback) {
        this.userFeedback = feedback;
    }
}
