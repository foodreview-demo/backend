package com.foodreview.domain.report.entity;

public enum ChatReportReason {
    HARASSMENT("욕설/비방"),
    SPAM("스팸/광고"),
    SEXUAL_HARASSMENT("성희롱"),
    FRAUD("사기/피싱"),
    INAPPROPRIATE("부적절한 내용"),
    OTHER("기타");

    private final String description;

    ChatReportReason(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
