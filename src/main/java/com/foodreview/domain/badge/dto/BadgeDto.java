package com.foodreview.domain.badge.dto;

import com.foodreview.domain.badge.entity.Badge;
import com.foodreview.domain.badge.entity.BadgeCategory;
import com.foodreview.domain.badge.entity.UserBadge;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

public class BadgeDto {

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String code;
        private String name;
        private String description;
        private String icon;
        private String category;
        private Integer conditionValue;
        private Boolean acquired;        // 획득 여부
        private LocalDateTime acquiredAt; // 획득 시간
        private Boolean isDisplayed;     // 표시 여부
        private Long acquiredCount;      // 전체 획득자 수 (선택)

        public static Response from(Badge badge, boolean acquired, LocalDateTime acquiredAt, Boolean isDisplayed) {
            return Response.builder()
                    .id(badge.getId())
                    .code(badge.getCode())
                    .name(badge.getName())
                    .description(badge.getDescription())
                    .icon(badge.getIcon())
                    .category(badge.getCategory().name())
                    .conditionValue(badge.getConditionValue())
                    .acquired(acquired)
                    .acquiredAt(acquiredAt)
                    .isDisplayed(isDisplayed)
                    .build();
        }

        public static Response from(UserBadge userBadge) {
            Badge badge = userBadge.getBadge();
            return Response.builder()
                    .id(badge.getId())
                    .code(badge.getCode())
                    .name(badge.getName())
                    .description(badge.getDescription())
                    .icon(badge.getIcon())
                    .category(badge.getCategory().name())
                    .conditionValue(badge.getConditionValue())
                    .acquired(true)
                    .acquiredAt(userBadge.getAcquiredAt())
                    .isDisplayed(userBadge.getIsDisplayed())
                    .build();
        }
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class SimpleResponse {
        private Long id;
        private String name;
        private String icon;
        private String category;

        public static SimpleResponse from(Badge badge) {
            return SimpleResponse.builder()
                    .id(badge.getId())
                    .name(badge.getName())
                    .icon(badge.getIcon())
                    .category(badge.getCategory().name())
                    .build();
        }
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class AcquiredNotification {
        private Long badgeId;
        private String name;
        private String icon;
        private String description;
        private LocalDateTime acquiredAt;

        public static AcquiredNotification from(Badge badge) {
            return AcquiredNotification.builder()
                    .badgeId(badge.getId())
                    .name(badge.getName())
                    .icon(badge.getIcon())
                    .description(badge.getDescription())
                    .acquiredAt(LocalDateTime.now())
                    .build();
        }
    }

    @Getter
    @AllArgsConstructor
    public static class DisplayRequest {
        private Long badgeId;
        private Boolean display;
    }
}
