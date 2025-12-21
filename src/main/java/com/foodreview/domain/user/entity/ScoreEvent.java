package com.foodreview.domain.user.entity;

import com.foodreview.domain.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "score_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ScoreEvent extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScoreEventType type;

    @Column(nullable = false, length = 200)
    private String description;

    @Column(nullable = false)
    private Integer points;

    // 공감한 유저 정보 (공감 받았을 때만)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_user_id")
    private User fromUser;

    public enum ScoreEventType {
        FIRST_REVIEW,      // 첫 리뷰 작성 (100점)
        REVIEW,            // 일반 리뷰 작성 (50점)
        SYMPATHY_RECEIVED, // 공감 받음 (상대방 점수의 0.5%)
        SYMPATHY_BONUS,    // 마스터 공감 보너스 (25점)
        INFLUENCE,         // 리뷰 영향력 (내 리뷰가 참고됨, 5점)
        INFLUENCE_FIRST_REVIEW // 첫 리뷰 영향력 (첫 리뷰가 참고됨, 10점)
    }
}
