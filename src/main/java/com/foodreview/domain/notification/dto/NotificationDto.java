package com.foodreview.domain.notification.dto;

import com.foodreview.domain.notification.entity.Notification;
import lombok.*;

import java.time.LocalDateTime;

public class NotificationDto {

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String type;
        private String message;
        private Long referenceId;
        private ActorInfo actor;
        private Boolean isRead;
        private LocalDateTime createdAt;

        public static Response from(Notification notification) {
            ActorInfo actorInfo = null;
            if (notification.getActor() != null) {
                actorInfo = ActorInfo.builder()
                        .id(notification.getActor().getId())
                        .name(notification.getActor().getName())
                        .avatar(notification.getActor().getAvatar())
                        .build();
            }

            return Response.builder()
                    .id(notification.getId())
                    .type(notification.getType().name())
                    .message(notification.getMessage())
                    .referenceId(notification.getReferenceId())
                    .actor(actorInfo)
                    .isRead(notification.getIsRead())
                    .createdAt(notification.getCreatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class ActorInfo {
        private Long id;
        private String name;
        private String avatar;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class UnreadCountResponse {
        private long count;
    }
}
