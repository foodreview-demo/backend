package com.foodreview.domain.chat.entity;

import com.foodreview.domain.common.BaseTimeEntity;
import com.foodreview.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "chat_rooms", indexes = {
    @Index(name = "idx_chat_room_uuid", columnList = "uuid", unique = true)
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChatRoom extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 36)
    private String uuid;

    // 1:1 채팅용 (하위 호환성 유지)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user1_id")
    private User user1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user2_id")
    private User user2;

    // 단체톡용 멤버 목록
    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ChatRoomMember> members = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RoomType roomType = RoomType.DIRECT;

    @Column(length = 50)
    private String name;  // 단체톡방 이름

    @Column(name = "last_message", length = 500)
    private String lastMessage;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    public enum RoomType {
        DIRECT,  // 1:1 채팅
        GROUP    // 단체톡
    }

    @PrePersist
    public void generateUuid() {
        if (this.uuid == null) {
            this.uuid = UUID.randomUUID().toString();
        }
    }

    // UUID가 없으면 생성 (기존 데이터 호환용)
    public void ensureUuid() {
        if (this.uuid == null) {
            this.uuid = UUID.randomUUID().toString();
        }
    }

    // 마지막 메시지 업데이트
    public void updateLastMessage(String message) {
        this.lastMessage = message;
        this.lastMessageAt = LocalDateTime.now();
    }

    // 상대방 유저 조회 (1:1 채팅용)
    public User getOtherUser(Long userId) {
        if (roomType == RoomType.DIRECT) {
            return user1.getId().equals(userId) ? user2 : user1;
        }
        return null;  // 단체톡은 상대방이 여러 명이므로 null 반환
    }

    // 멤버 추가
    public void addMember(ChatRoomMember member) {
        members.add(member);
    }

    // 멤버 제거
    public void removeMember(ChatRoomMember member) {
        members.remove(member);
    }

    // 특정 유저가 멤버인지 확인
    public boolean isMember(Long userId) {
        if (roomType == RoomType.DIRECT) {
            return user1.getId().equals(userId) || user2.getId().equals(userId);
        }
        return members.stream().anyMatch(m -> m.getUser().getId().equals(userId));
    }

    // 단체톡방 이름 변경
    public void updateName(String name) {
        this.name = name;
    }

    // 멤버 수 조회
    public int getMemberCount() {
        if (roomType == RoomType.DIRECT) {
            return 2;
        }
        return members.size();
    }

    // 모든 멤버 ID 조회
    public List<Long> getAllMemberIds() {
        if (roomType == RoomType.DIRECT) {
            return List.of(user1.getId(), user2.getId());
        }
        return members.stream()
                .map(m -> m.getUser().getId())
                .toList();
    }
}
