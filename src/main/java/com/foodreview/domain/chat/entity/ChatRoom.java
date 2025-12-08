package com.foodreview.domain.chat.entity;

import com.foodreview.domain.common.BaseTimeEntity;
import com.foodreview.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_rooms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChatRoom extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user1_id", nullable = false)
    private User user1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user2_id", nullable = false)
    private User user2;

    @Column(name = "last_message", length = 500)
    private String lastMessage;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    // 마지막 메시지 업데이트
    public void updateLastMessage(String message) {
        this.lastMessage = message;
        this.lastMessageAt = LocalDateTime.now();
    }

    // 상대방 유저 조회
    public User getOtherUser(Long userId) {
        return user1.getId().equals(userId) ? user2 : user1;
    }
}
