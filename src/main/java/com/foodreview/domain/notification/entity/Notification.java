package com.foodreview.domain.notification.entity;

import com.foodreview.domain.common.BaseTimeEntity;
import com.foodreview.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Notification extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;  // 알림을 받는 사용자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;  // 알림을 발생시킨 사용자

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(name = "reference_id")
    private Long referenceId;  // 관련 엔티티 ID (리뷰, 댓글 등)

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    public void markAsRead() {
        this.isRead = true;
    }

    public enum NotificationType {
        COMMENT,            // 내 리뷰에 댓글
        REPLY,              // 내 댓글에 대댓글
        SYMPATHY,           // 공감
        FOLLOW,             // 팔로우
        INFLUENCE,          // 내 리뷰가 참고됨
        REFERENCE,          // 리뷰 참고 (INFLUENCE와 동일, 코드 호환성)
        CHAT,               // 새 채팅 메시지
        GATHERING_REVIEWED, // 리뷰한 음식점에 번개모임 생성
        GATHERING_NEARBY,   // 내 지역 근처에 번개모임 생성
        GATHERING_REMINDER  // 참여 중인 모임 시작 1시간 전 알림
    }
}
