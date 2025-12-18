package com.foodreview.domain.review.entity;

import com.foodreview.domain.common.BaseTimeEntity;
import com.foodreview.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "comments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Comment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 500)
    private String content;

    // 대댓글을 위한 부모 댓글 (null이면 최상위 댓글)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;

    // 삭제 여부 (soft delete)
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    // 댓글 수정
    public void update(String content) {
        if (content != null) {
            this.content = content;
        }
    }

    // 소프트 삭제
    public void softDelete() {
        this.isDeleted = true;
        this.content = "삭제된 댓글입니다";
    }
}
