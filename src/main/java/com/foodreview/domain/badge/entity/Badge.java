package com.foodreview.domain.badge.entity;

import com.foodreview.domain.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "badge")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Badge extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;  // GRADE_BEGINNER, ACHIEVEMENT_FIRST_REVIEW 등

    @Column(nullable = false, length = 50)
    private String name;  // 입문자, 첫 발걸음 등

    @Column(length = 200)
    private String description;  // 배지 설명

    @Column(nullable = false, length = 10)
    private String icon;  // 이모지 아이콘 🌱, 🎉 등

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BadgeCategory category;  // GRADE, ACHIEVEMENT

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BadgeConditionType conditionType;  // SCORE, REVIEW_COUNT, FOLLOWER_COUNT 등

    @Column(nullable = false)
    private Integer conditionValue;  // 조건 값 (예: 500점, 10개 등)

    @Column(nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;  // 정렬 순서

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;  // 활성화 여부
}
