package com.foodreview.domain.chat.dto;

import com.foodreview.domain.chat.entity.ChatMessage;
import com.foodreview.domain.chat.entity.ChatRoom;
import com.foodreview.domain.chat.entity.ChatRoomMember;
import com.foodreview.domain.user.dto.UserDto;
import com.foodreview.domain.user.entity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

public class ChatDto {

    @Getter
    @Builder
    @AllArgsConstructor
    public static class RoomResponse {
        private Long id;
        private String uuid;
        private String roomType;  // DIRECT or GROUP
        private String name;      // 단체톡방 이름 (1:1은 null)
        private UserDto.SimpleResponse otherUser;  // 1:1 채팅용
        private List<MemberResponse> members;      // 단체톡용 멤버 목록
        private Integer memberCount;
        private String lastMessage;
        private LocalDateTime lastMessageAt;
        private Long unreadCount;

        // 1:1 채팅용
        public static RoomResponse from(ChatRoom room, User currentUser, Long unreadCount) {
            User otherUser = room.getOtherUser(currentUser.getId());
            return RoomResponse.builder()
                    .id(room.getId())
                    .uuid(room.getUuid())
                    .roomType(room.getRoomType().name())
                    .name(room.getName())
                    .otherUser(otherUser != null ? UserDto.SimpleResponse.from(otherUser) : null)
                    .members(null)
                    .memberCount(room.getMemberCount())
                    .lastMessage(room.getLastMessage())
                    .lastMessageAt(room.getLastMessageAt())
                    .unreadCount(unreadCount)
                    .build();
        }

        // 단체톡용 (멤버 목록 포함)
        public static RoomResponse fromGroup(ChatRoom room, List<ChatRoomMember> members, Long unreadCount) {
            return RoomResponse.builder()
                    .id(room.getId())
                    .uuid(room.getUuid())
                    .roomType(room.getRoomType().name())
                    .name(room.getName())
                    .otherUser(null)
                    .members(members.stream().map(MemberResponse::from).toList())
                    .memberCount(members.size())
                    .lastMessage(room.getLastMessage())
                    .lastMessageAt(room.getLastMessageAt())
                    .unreadCount(unreadCount)
                    .build();
        }
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class MemberResponse {
        private Long id;
        private UserDto.SimpleResponse user;
        private String role;  // OWNER or MEMBER
        private LocalDateTime joinedAt;

        public static MemberResponse from(ChatRoomMember member) {
            return MemberResponse.builder()
                    .id(member.getId())
                    .user(UserDto.SimpleResponse.from(member.getUser()))
                    .role(member.getRole().name())
                    .joinedAt(member.getJoinedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class MessageResponse {
        private Long id;
        private Long senderId;
        private String senderName;
        private String senderAvatar;
        private String content;
        private LocalDateTime createdAt;
        private Boolean isRead;      // 1:1 채팅용 (하위 호환)
        private Boolean isMine;
        private Integer readCount;   // 읽은 사람 수 (단체톡용, 본인 제외)
        private Integer memberCount; // 전체 멤버 수 (단체톡용, 본인 제외)
        private String messageType;  // NORMAL or SYSTEM

        // 1:1 채팅용 (기존 호환)
        public static MessageResponse from(ChatMessage message, Long currentUserId) {
            boolean isSystem = message.isSystemMessage();
            return MessageResponse.builder()
                    .id(message.getId())
                    .senderId(isSystem ? null : message.getSender().getId())
                    .senderName(isSystem ? null : message.getSender().getName())
                    .senderAvatar(isSystem ? null : message.getSender().getAvatar())
                    .content(message.getContent())
                    .createdAt(message.getCreatedAt())
                    .isRead(message.getIsRead())
                    .isMine(!isSystem && message.getSender().getId().equals(currentUserId))
                    .readCount(null)
                    .memberCount(null)
                    .messageType(message.getMessageType().name())
                    .build();
        }

        // 단체톡용 (읽음 수 포함)
        public static MessageResponse fromWithReadCount(ChatMessage message, Long currentUserId, int readCount, int memberCount) {
            boolean isSystem = message.isSystemMessage();
            boolean isMine = !isSystem && message.getSender().getId().equals(currentUserId);
            return MessageResponse.builder()
                    .id(message.getId())
                    .senderId(isSystem ? null : message.getSender().getId())
                    .senderName(isSystem ? null : message.getSender().getName())
                    .senderAvatar(isSystem ? null : message.getSender().getAvatar())
                    .content(message.getContent())
                    .createdAt(message.getCreatedAt())
                    .isRead(readCount > 0)  // 1명이라도 읽었으면 true
                    .isMine(isMine)
                    .readCount(isSystem ? null : readCount)
                    .memberCount(isSystem ? null : memberCount)
                    .messageType(message.getMessageType().name())
                    .build();
        }

        // 시스템 메시지용
        public static MessageResponse fromSystem(ChatMessage message) {
            return MessageResponse.builder()
                    .id(message.getId())
                    .senderId(null)
                    .senderName(null)
                    .senderAvatar(null)
                    .content(message.getContent())
                    .createdAt(message.getCreatedAt())
                    .isRead(true)
                    .isMine(false)
                    .readCount(null)
                    .memberCount(null)
                    .messageType(ChatMessage.MessageType.SYSTEM.name())
                    .build();
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRoomRequest {
        @NotNull(message = "상대방 ID는 필수입니다")
        private Long otherUserId;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SendMessageRequest {
        @NotBlank(message = "메시지 내용은 필수입니다")
        @Size(max = 1000, message = "메시지는 1000자 이내로 작성해주세요")
        private String content;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WebSocketMessage {
        private String content;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NewMessageNotification {
        private String roomUuid;
        private MessageResponse message;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReadNotification {
        private String roomUuid;
        private Long readByUserId;
        private Long messageId;     // 어떤 메시지까지 읽었는지 (단체톡용)
        private Integer readCount;  // 해당 메시지를 읽은 총 인원 (단체톡용)

        // 1:1 채팅용 생성자
        public ReadNotification(String roomUuid, Long readByUserId) {
            this.roomUuid = roomUuid;
            this.readByUserId = readByUserId;
            this.messageId = null;
            this.readCount = null;
        }
    }

    // 단체톡방 생성 요청
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateGroupRoomRequest {
        @Size(max = 50, message = "채팅방 이름은 50자 이내로 작성해주세요")
        private String name;  // 선택적 채팅방 이름

        @NotEmpty(message = "초대할 사용자를 선택해주세요")
        @Size(min = 2, message = "단체톡은 최소 2명 이상이어야 합니다")
        private List<Long> memberIds;
    }

    // 채팅방 초대 요청
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InviteRequest {
        @NotEmpty(message = "초대할 사용자를 선택해주세요")
        private List<Long> userIds;
    }

    // 채팅방 이름 변경 요청
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRoomNameRequest {
        @NotBlank(message = "채팅방 이름은 필수입니다")
        @Size(max = 50, message = "채팅방 이름은 50자 이내로 작성해주세요")
        private String name;
    }
}
