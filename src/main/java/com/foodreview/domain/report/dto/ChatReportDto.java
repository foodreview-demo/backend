package com.foodreview.domain.report.dto;

import com.foodreview.domain.report.entity.ChatReport;
import com.foodreview.domain.report.entity.ChatReportReason;
import com.foodreview.domain.report.entity.ReportStatus;
import lombok.*;

import java.time.LocalDateTime;

public class ChatReportDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        private Long reportedUserId;
        private Long chatRoomId;
        private Long messageId;
        private String messageContent;
        private ChatReportReason reason;
        private String description;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private Long reporterId;
        private String reporterName;
        private Long reportedUserId;
        private String reportedUserName;
        private Long chatRoomId;
        private Long messageId;
        private String messageContent;
        private ChatReportReason reason;
        private String reasonDescription;
        private String description;
        private ReportStatus status;
        private String statusDescription;
        private LocalDateTime createdAt;

        public static Response from(ChatReport report) {
            return Response.builder()
                    .id(report.getId())
                    .reporterId(report.getReporter().getId())
                    .reporterName(report.getReporter().getName())
                    .reportedUserId(report.getReportedUser().getId())
                    .reportedUserName(report.getReportedUser().getName())
                    .chatRoomId(report.getChatRoom().getId())
                    .messageId(report.getMessageId())
                    .messageContent(report.getMessageContent())
                    .reason(report.getReason())
                    .reasonDescription(report.getReason().getDescription())
                    .description(report.getDescription())
                    .status(report.getStatus())
                    .statusDescription(report.getStatus().getDescription())
                    .createdAt(report.getCreatedAt())
                    .build();
        }
    }
}
