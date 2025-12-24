package com.foodreview.domain.report.entity;

public enum ReportStatus {
    PENDING("대기중"),
    RESOLVED("처리완료"),
    REJECTED("반려");

    private final String description;

    ReportStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
