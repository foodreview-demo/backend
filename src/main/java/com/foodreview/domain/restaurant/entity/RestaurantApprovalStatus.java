package com.foodreview.domain.restaurant.entity;

public enum RestaurantApprovalStatus {
    PENDING("승인 대기"),
    APPROVED("승인됨"),
    REJECTED("거부됨");

    private final String displayName;

    RestaurantApprovalStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
