package com.foodreview.domain.report.dto;

import com.foodreview.domain.report.entity.Report;
import com.foodreview.domain.report.entity.ReportReason;
import com.foodreview.domain.report.entity.ReportStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ReportResponse {

    private Long id;
    private Long reviewId;
    private String reviewContent;
    private String reviewAuthorName;
    private Long reviewAuthorId;
    private String restaurantName;
    private Long reporterId;
    private String reporterName;
    private ReportReason reason;
    private String reasonDescription;
    private String description;
    private ReportStatus status;
    private String statusDescription;
    private String adminNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ReportResponse from(Report report) {
        return ReportResponse.builder()
                .id(report.getId())
                .reviewId(report.getReview().getId())
                .reviewContent(report.getReview().getContent())
                .reviewAuthorName(report.getReview().getUser().getName())
                .reviewAuthorId(report.getReview().getUser().getId())
                .restaurantName(report.getReview().getRestaurant() != null ?
                    report.getReview().getRestaurant().getName() : null)
                .reporterId(report.getReporter().getId())
                .reporterName(report.getReporter().getName())
                .reason(report.getReason())
                .reasonDescription(report.getReason().getDescription())
                .description(report.getDescription())
                .status(report.getStatus())
                .statusDescription(report.getStatus().getDescription())
                .adminNote(report.getAdminNote())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .build();
    }
}
