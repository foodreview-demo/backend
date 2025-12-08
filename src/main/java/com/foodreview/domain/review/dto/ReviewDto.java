package com.foodreview.domain.review.dto;

import com.foodreview.domain.restaurant.dto.RestaurantDto;
import com.foodreview.domain.review.entity.Review;
import com.foodreview.domain.user.dto.UserDto;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class ReviewDto {

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private UserDto.SimpleResponse user;
        private RestaurantDto.SimpleResponse restaurant;
        private String content;
        private BigDecimal rating;
        private List<String> images;
        private String menu;
        private String price;
        private LocalDate visitDate;
        private LocalDateTime createdAt;
        private Integer sympathyCount;
        private Boolean isFirstReview;
        private Boolean hasSympathized;

        public static Response from(Review review, Boolean hasSympathized) {
            return Response.builder()
                    .id(review.getId())
                    .user(UserDto.SimpleResponse.from(review.getUser()))
                    .restaurant(RestaurantDto.SimpleResponse.from(review.getRestaurant()))
                    .content(review.getContent())
                    .rating(review.getRating())
                    .images(review.getImages())
                    .menu(review.getMenu())
                    .price(review.getPrice())
                    .visitDate(review.getVisitDate())
                    .createdAt(review.getCreatedAt())
                    .sympathyCount(review.getSympathyCount())
                    .isFirstReview(review.getIsFirstReview())
                    .hasSympathized(hasSympathized)
                    .build();
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        @NotNull(message = "음식점 ID는 필수입니다")
        private Long restaurantId;

        @NotBlank(message = "리뷰 내용은 필수입니다")
        @Size(max = 1000, message = "리뷰는 1000자 이내로 작성해주세요")
        private String content;

        @NotNull(message = "별점은 필수입니다")
        @DecimalMin(value = "1.0", message = "별점은 1점 이상이어야 합니다")
        @DecimalMax(value = "5.0", message = "별점은 5점 이하여야 합니다")
        private BigDecimal rating;

        private List<String> images;

        @Size(max = 100, message = "메뉴명은 100자 이내로 작성해주세요")
        private String menu;

        @Size(max = 50, message = "가격은 50자 이내로 작성해주세요")
        private String price;

        private LocalDate visitDate;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        @Size(max = 1000, message = "리뷰는 1000자 이내로 작성해주세요")
        private String content;

        @DecimalMin(value = "1.0", message = "별점은 1점 이상이어야 합니다")
        @DecimalMax(value = "5.0", message = "별점은 5점 이하여야 합니다")
        private BigDecimal rating;

        private List<String> images;
        private String menu;
        private String price;
        private LocalDate visitDate;
    }
}
