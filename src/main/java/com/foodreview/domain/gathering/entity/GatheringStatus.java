package com.foodreview.domain.gathering.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GatheringStatus {
    RECRUITING("모집중"),
    CONFIRMED("확정"),
    IN_PROGRESS("진행중"),
    COMPLETED("완료"),
    CANCELLED("취소");

    private final String displayName;
}
