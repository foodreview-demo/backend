package com.foodreview.domain.chat.entity;

import com.foodreview.domain.common.BaseTimeEntity;
import com.foodreview.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_room_members", indexes = {
    @Index(name = "idx_chat_room_member", columnList = "chat_room_id, user_id", unique = true)
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChatRoomMember extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private MemberRole role = MemberRole.MEMBER;

    @Column(name = "last_read_at")
    private LocalDateTime lastReadAt;

    @Column(name = "joined_at", nullable = false)
    @Builder.Default
    private LocalDateTime joinedAt = LocalDateTime.now();

    public enum MemberRole {
        OWNER,  // 방장
        MEMBER  // 일반 멤버
    }

    public void updateLastReadAt() {
        this.lastReadAt = LocalDateTime.now();
    }
}
