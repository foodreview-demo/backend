package com.foodreview.domain.review.entity;

import com.foodreview.domain.common.BaseTimeEntity;
import com.foodreview.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "review_references", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"review_id", "reference_review_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ReviewReference extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 작성된 리뷰 (참고를 한 리뷰)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    // 참고한 리뷰 (참고를 받은 리뷰)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reference_review_id", nullable = false)
    private Review referenceReview;

    // 참고한 리뷰 작성자 (역정규화, 조회 편의)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reference_user_id", nullable = false)
    private User referenceUser;

    // 지급된 포인트
    @Column(name = "points_awarded", nullable = false)
    private Integer pointsAwarded;
}
