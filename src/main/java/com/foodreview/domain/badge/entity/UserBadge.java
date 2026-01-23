package com.foodreview.domain.badge.entity;

import com.foodreview.domain.common.BaseTimeEntity;
import com.foodreview.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_badge",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "badge_id"})
    },
    indexes = {
        @Index(name = "idx_user_badge_user", columnList = "user_id"),
        @Index(name = "idx_user_badge_acquired", columnList = "acquired_at DESC")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserBadge extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "badge_id", nullable = false)
    private Badge badge;

    @Column(name = "acquired_at", nullable = false)
    private LocalDateTime acquiredAt;

    @Column(name = "is_displayed", nullable = false)
    @Builder.Default
    private Boolean isDisplayed = false;  // 프로필에 표시 여부

    public static UserBadge create(User user, Badge badge) {
        return UserBadge.builder()
                .user(user)
                .badge(badge)
                .acquiredAt(LocalDateTime.now())
                .isDisplayed(false)
                .build();
    }

    public void setDisplayed(boolean displayed) {
        this.isDisplayed = displayed;
    }
}
