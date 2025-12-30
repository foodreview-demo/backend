package com.foodreview.domain.report.entity;

public enum ReportReason {
    SPAM("스팸/광고"),
    INAPPROPRIATE("부적절한 내용"),
    FAKE_REVIEW("허위 리뷰"),
    NO_RECEIPT("영수증 미첨부"),
    HARASSMENT("비방/욕설"),
    COPYRIGHT("저작권 침해"),
    OTHER("기타");

    private final String description;

    ReportReason(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
