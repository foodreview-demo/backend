package com.foodreview.domain.user.dto;

import com.foodreview.domain.user.entity.RecommendationCache;
import com.foodreview.domain.user.entity.User;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public class UserDto {

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String name;
        private String avatar;
        private String region;
        private String district;
        private String neighborhood;
        private Integer tasteScore;
        private String tasteGrade;
        private Integer reviewCount;
        private Integer receivedSympathyCount;
        private List<String> favoriteCategories;
        private Integer rank;

        public static Response from(User user) {
            return Response.builder()
                    .id(user.getId())
                    .name(user.getName())
                    .avatar(user.getAvatar())
                    .region(user.getRegion())
                    .district(user.getDistrict())
                    .neighborhood(user.getNeighborhood())
                    .tasteScore(user.getTasteScore())
                    .tasteGrade(user.getTasteGrade())
                    .reviewCount(user.getReviewCount())
                    .receivedSympathyCount(user.getReceivedSympathyCount())
                    .favoriteCategories(user.getFavoriteCategories())
                    .build();
        }

        public static Response from(User user, Integer rank) {
            return Response.builder()
                    .id(user.getId())
                    .name(user.getName())
                    .avatar(user.getAvatar())
                    .region(user.getRegion())
                    .district(user.getDistrict())
                    .neighborhood(user.getNeighborhood())
                    .tasteScore(user.getTasteScore())
                    .tasteGrade(user.getTasteGrade())
                    .reviewCount(user.getReviewCount())
                    .receivedSympathyCount(user.getReceivedSympathyCount())
                    .favoriteCategories(user.getFavoriteCategories())
                    .rank(rank)
                    .build();
        }
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class SimpleResponse {
        private Long id;
        private String name;
        private String avatar;
        private String region;
        private Integer tasteScore;
        private String tasteGrade;

        public static SimpleResponse from(User user) {
            return SimpleResponse.builder()
                    .id(user.getId())
                    .name(user.getName())
                    .avatar(user.getAvatar())
                    .region(user.getRegion())
                    .tasteScore(user.getTasteScore())
                    .tasteGrade(user.getTasteGrade())
                    .build();
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        private String name;
        private String avatar;
        private String region;
        private String district;
        private String neighborhood;
        private List<String> favoriteCategories;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class RankingResponse {
        private Long id;
        private String name;
        private String avatar;
        private String region;
        private Integer tasteScore;
        private String tasteGrade;
        private Integer rank;
        private Integer reviewCount;

        public static RankingResponse from(User user, Integer rank) {
            return RankingResponse.builder()
                    .id(user.getId())
                    .name(user.getName())
                    .avatar(user.getAvatar())
                    .region(user.getRegion())
                    .tasteScore(user.getTasteScore())
                    .tasteGrade(user.getTasteGrade())
                    .rank(rank)
                    .reviewCount(user.getReviewCount())
                    .build();
        }
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class RecommendResponse {
        private Long id;
        private String name;
        private String avatar;
        private String region;
        private Integer tasteScore;
        private String tasteGrade;
        private List<String> commonCategories;
        private String recommendReason;

        public static RecommendResponse from(User user, List<String> commonCategories, String reason) {
            return RecommendResponse.builder()
                    .id(user.getId())
                    .name(user.getName())
                    .avatar(user.getAvatar())
                    .region(user.getRegion())
                    .tasteScore(user.getTasteScore())
                    .tasteGrade(user.getTasteGrade())
                    .commonCategories(commonCategories)
                    .recommendReason(reason)
                    .build();
        }
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class BlockedUserResponse {
        private Long id;
        private String name;
        private String avatar;
        private String tasteGrade;
        private java.time.LocalDateTime blockedAt;

        public static BlockedUserResponse from(User user, java.time.LocalDateTime blockedAt) {
            return BlockedUserResponse.builder()
                    .id(user.getId())
                    .name(user.getName())
                    .avatar(user.getAvatar())
                    .tasteGrade(user.getTasteGrade())
                    .blockedAt(blockedAt)
                    .build();
        }
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class SearchResponse {
        private Long id;
        private String name;
        private String avatar;
        private String region;
        private Integer tasteScore;
        private String tasteGrade;
        private Integer reviewCount;
        private Boolean isFollowing;

        public static SearchResponse from(User user, Boolean isFollowing) {
            return SearchResponse.builder()
                    .id(user.getId())
                    .name(user.getName())
                    .avatar(user.getAvatar())
                    .region(user.getRegion())
                    .tasteScore(user.getTasteScore())
                    .tasteGrade(user.getTasteGrade())
                    .reviewCount(user.getReviewCount())
                    .isFollowing(isFollowing)
                    .build();
        }
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class NotificationSettingsResponse {
        private Boolean reviews;
        private Boolean follows;
        private Boolean messages;
        private Boolean marketing;
        private Boolean gatherings;

        public static NotificationSettingsResponse from(User user) {
            return NotificationSettingsResponse.builder()
                    .reviews(user.getNotifyReviews())
                    .follows(user.getNotifyFollows())
                    .messages(user.getNotifyMessages())
                    .marketing(user.getNotifyMarketing())
                    .gatherings(user.getNotifyGatherings())
                    .build();
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NotificationSettingsRequest {
        private Boolean reviews;
        private Boolean follows;
        private Boolean messages;
        private Boolean marketing;
        private Boolean gatherings;
    }

    // 추천 점수 상세 (디버깅/Admin용)
    @Getter
    @Builder
    @AllArgsConstructor
    public static class RecommendationScoreDetail {
        private Long userId;
        private Long recommendedUserId;
        private int totalScore;
        private int secondDegreeScore;
        private int secondDegreeCount;
        private int outgoingSympathyScore;
        private int outgoingSympathyCount;
        private int incomingSympathyScore;
        private int incomingSympathyCount;
        private int tasteScore;
        private double tasteSimilarity;
        private int commonRestaurantCount;
        private int baseScore;
        private String reason;
        private List<String> commonCategories;
        private LocalDateTime calculatedAt;

        public static RecommendationScoreDetail from(RecommendationCache cache) {
            return RecommendationScoreDetail.builder()
                    .userId(cache.getUserId())
                    .recommendedUserId(cache.getRecommendedUserId())
                    .totalScore(cache.getTotalScore())
                    .secondDegreeScore(cache.getSecondDegreeScore())
                    .secondDegreeCount(cache.getSecondDegreeCount())
                    .outgoingSympathyScore(cache.getOutgoingSympathyScore())
                    .outgoingSympathyCount(cache.getOutgoingSympathyCount())
                    .incomingSympathyScore(cache.getIncomingSympathyScore())
                    .incomingSympathyCount(cache.getIncomingSympathyCount())
                    .tasteScore(cache.getTasteScore())
                    .tasteSimilarity(cache.getTasteSimilarity())
                    .commonRestaurantCount(cache.getCommonRestaurantCount())
                    .baseScore(cache.getBaseScore())
                    .reason(cache.getReason())
                    .commonCategories(cache.getCommonCategories() != null && !cache.getCommonCategories().isEmpty()
                            ? Arrays.asList(cache.getCommonCategories().split(","))
                            : List.of())
                    .calculatedAt(cache.getCalculatedAt())
                    .build();
        }
    }
}
