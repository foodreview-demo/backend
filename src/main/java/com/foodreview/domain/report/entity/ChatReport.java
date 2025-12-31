package com.foodreview.domain.report.entity;

import com.foodreview.domain.chat.entity.ChatRoom;
import com.foodreview.domain.common.BaseTimeEntity;
import com.foodreview.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "chat_reports",
        indexes = {
            @Index(name = "idx_chat_report_status", columnList = "status"),
            @Index(name = "idx_chat_report_chat_room", columnList = "chat_room_id"),
            @Index(name = "idx_chat_report_reporter", columnList = "reporter_id"),
            @Index(name = "idx_chat_report_reported", columnList = "reported_user_id"),
            @Index(name = "idx_chat_report_created", columnList = "created_at DESC")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChatReport extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_user_id", nullable = false)
    private User reportedUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @Column(name = "message_id")
    private Long messageId;

    @Column(name = "message_content", length = 1000)
    private String messageContent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatReportReason reason;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ReportStatus status = ReportStatus.PENDING;

    @Column(name = "admin_note", length = 500)
    private String adminNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by")
    private User processedBy;

    public void resolve(User admin, String adminNote) {
        this.status = ReportStatus.RESOLVED;
        this.processedBy = admin;
        this.adminNote = adminNote;
    }

    public void reject(User admin, String adminNote) {
        this.status = ReportStatus.REJECTED;
        this.processedBy = admin;
        this.adminNote = adminNote;
    }
}
