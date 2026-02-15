package com.foodreview.domain.gathering.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RefundType {
    AUTO("자동환금"),
    MANUAL("수동환금");

    private final String displayName;
}
