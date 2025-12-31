package com.foodreview.domain.user.dto;

import com.foodreview.domain.user.entity.User;
import lombok.*;

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
}
