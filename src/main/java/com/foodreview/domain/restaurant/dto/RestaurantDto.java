package com.foodreview.domain.restaurant.dto;

import com.foodreview.domain.restaurant.entity.Restaurant;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

public class RestaurantDto {

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String name;
        private String category;
        private String categoryDisplay;
        private String address;
        private String region;
        private String district;
        private String neighborhood;
        private String thumbnail;
        private BigDecimal averageRating;
        private Integer reviewCount;
        private String priceRange;
        private String phone;
        private String businessHours;
        private Boolean isFirstReviewAvailable;
        private String kakaoPlaceId;
        private Double latitude;
        private Double longitude;

        public static Response from(Restaurant restaurant) {
            return Response.builder()
                    .id(restaurant.getId())
                    .name(restaurant.getName())
                    .category(restaurant.getCategory().name())
                    .categoryDisplay(restaurant.getCategory().getDisplayName())
                    .address(restaurant.getAddress())
                    .region(restaurant.getRegion())
                    .district(restaurant.getDistrict())
                    .neighborhood(restaurant.getNeighborhood())
                    .thumbnail(restaurant.getThumbnail())
                    .averageRating(restaurant.getAverageRating())
                    .reviewCount(restaurant.getReviewCount())
                    .priceRange(restaurant.getPriceRange())
                    .phone(restaurant.getPhone())
                    .businessHours(restaurant.getBusinessHours())
                    .isFirstReviewAvailable(restaurant.isFirstReviewAvailable())
                    .kakaoPlaceId(restaurant.getKakaoPlaceId())
                    .latitude(restaurant.getLatitude())
                    .longitude(restaurant.getLongitude())
                    .build();
        }
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class SimpleResponse {
        private Long id;
        private String name;
        private String category;
        private String categoryDisplay;
        private String address;
        private String region;
        private String district;
        private String neighborhood;
        private String thumbnail;
        private BigDecimal averageRating;
        private Integer reviewCount;

        public static SimpleResponse from(Restaurant restaurant) {
            return SimpleResponse.builder()
                    .id(restaurant.getId())
                    .name(restaurant.getName())
                    .category(restaurant.getCategory().name())
                    .categoryDisplay(restaurant.getCategory().getDisplayName())
                    .address(restaurant.getAddress())
                    .region(restaurant.getRegion())
                    .district(restaurant.getDistrict())
                    .neighborhood(restaurant.getNeighborhood())
                    .thumbnail(restaurant.getThumbnail())
                    .averageRating(restaurant.getAverageRating())
                    .reviewCount(restaurant.getReviewCount())
                    .build();
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        @NotBlank(message = "음식점 이름은 필수입니다")
        private String name;

        @NotNull(message = "카테고리는 필수입니다")
        private String category;

        @NotBlank(message = "주소는 필수입니다")
        private String address;

        @NotBlank(message = "지역은 필수입니다")
        private String region;

        private String district;
        private String neighborhood;

        private String thumbnail;
        private String priceRange;
        private String phone;
        private String businessHours;

        // 카카오맵 연동 정보
        private String kakaoPlaceId;
        private Double latitude;
        private Double longitude;
    }
}
