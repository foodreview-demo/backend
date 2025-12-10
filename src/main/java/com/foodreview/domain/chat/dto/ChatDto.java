package com.foodreview.domain.chat.dto;

import com.foodreview.domain.chat.entity.ChatMessage;
import com.foodreview.domain.chat.entity.ChatRoom;
import com.foodreview.domain.user.dto.UserDto;
import com.foodreview.domain.user.entity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

public class ChatDto {

    @Getter
    @Builder
    @AllArgsConstructor
    public static class RoomResponse {
        private Long id;
        private String uuid;
        private UserDto.SimpleResponse otherUser;
        private String lastMessage;
        private LocalDateTime lastMessageAt;
        private Long unreadCount;

        public static RoomResponse from(ChatRoom room, User currentUser, Long unreadCount) {
            User otherUser = room.getOtherUser(currentUser.getId());
            return RoomResponse.builder()
                    .id(room.getId())
                    .uuid(room.getUuid())
                    .otherUser(UserDto.SimpleResponse.from(otherUser))
                    .lastMessage(room.getLastMessage())
                    .lastMessageAt(room.getLastMessageAt())
                    .unreadCount(unreadCount)
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
        private Boolean isRead;
        private Boolean isMine;

        public static MessageResponse from(ChatMessage message, Long currentUserId) {
            return MessageResponse.builder()
                    .id(message.getId())
                    .senderId(message.getSender().getId())
                    .senderName(message.getSender().getName())
                    .senderAvatar(message.getSender().getAvatar())
                    .content(message.getContent())
                    .createdAt(message.getCreatedAt())
                    .isRead(message.getIsRead())
                    .isMine(message.getSender().getId().equals(currentUserId))
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
}
