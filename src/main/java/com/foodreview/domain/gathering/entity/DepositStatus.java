package com.foodreview.domain.gathering.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DepositStatus {
    PENDING("결제대기"),
    DEPOSITED("보증금완료"),
    REFUNDED("환금완료"),
    REFUND_FAILED("환금실패");

    private final String displayName;
}
