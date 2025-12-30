package com.foodreview.domain.report.entity;

import com.foodreview.domain.common.BaseTimeEntity;
import com.foodreview.domain.review.entity.Review;
import com.foodreview.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "reports",
        indexes = {
            @Index(name = "idx_report_status", columnList = "status"),
            @Index(name = "idx_report_review", columnList = "review_id"),
            @Index(name = "idx_report_created", columnList = "created_at DESC")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Report extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportReason reason;

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
