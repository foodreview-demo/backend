package com.foodreview.domain.badge.entity;

public enum BadgeConditionType {
    // 등급 관련
    SCORE,              // 맛잘알 점수 달성

    // 리뷰 관련
    REVIEW_COUNT,       // 리뷰 작성 수
    RECEIVED_SYMPATHY,  // 받은 공감 수

    // 소셜 관련
    FOLLOWER_COUNT,     // 팔로워 수
    FOLLOWING_COUNT,    // 팔로잉 수

    // 탐험 관련
    REGION_COUNT,       // 방문 지역 수 (리뷰 작성 기준)
    CATEGORY_COUNT,     // 방문 카테고리 수

    // 특별
    FIRST_REVIEW,       // 첫 리뷰 (conditionValue = 1)
    FIRST_FOLLOWER      // 첫 팔로워 (conditionValue = 1)
}
