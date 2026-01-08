package com.foodreview.domain.review.dto;

import com.foodreview.domain.restaurant.dto.RestaurantDto;
import com.foodreview.domain.review.entity.ReceiptVerificationStatus;
import com.foodreview.domain.review.entity.ReferenceType;
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
        private BigDecimal tasteRating;
        private BigDecimal priceRating;
        private BigDecimal atmosphereRating;
        private BigDecimal serviceRating;
        private List<String> images;
        private String menu;
        private String price;
        private LocalDate visitDate;
        private LocalDateTime createdAt;
        private Integer sympathyCount;
        private Boolean isFirstReview;
        private Boolean hasSympathized;
        // 영수증 인증 이미지
        private String receiptImageUrl;
        // 영수증 검증 상태
        private ReceiptVerificationStatus receiptVerificationStatus;
        // 영수증이 실제로 인증되었는지 (VERIFIED 또는 MANUALLY_APPROVED)
        private Boolean isReceiptVerified;
        // 음식점을 알게 된 경로
        private ReferenceType referenceType;
        // 참고 정보 (referenceType이 REVIEW인 경우)
        private ReferenceInfo referenceInfo;
        private Integer referenceCount; // 이 리뷰를 참고한 횟수

        public static Response from(Review review, Boolean hasSympathized) {
            return from(review, hasSympathized, null, 0);
        }

        public static Response from(Review review, Boolean hasSympathized, ReferenceInfo referenceInfo, Integer referenceCount) {
            return Response.builder()
                    .id(review.getId())
                    .user(UserDto.SimpleResponse.from(review.getUser()))
                    .restaurant(RestaurantDto.SimpleResponse.from(review.getRestaurant()))
                    .content(review.getContent())
                    .rating(review.getRating())
                    .tasteRating(review.getTasteRating())
                    .priceRating(review.getPriceRating())
                    .atmosphereRating(review.getAtmosphereRating())
                    .serviceRating(review.getServiceRating())
                    .images(review.getImages())
                    .menu(review.getMenu())
                    .price(review.getPrice())
                    .visitDate(review.getVisitDate())
                    .createdAt(review.getCreatedAt())
                    .sympathyCount(review.getSympathyCount())
                    .isFirstReview(review.getIsFirstReview())
                    .hasSympathized(hasSympathized)
                    .receiptImageUrl(review.getReceiptImageUrl())
                    .receiptVerificationStatus(review.getReceiptVerificationStatus())
                    .isReceiptVerified(review.isReceiptVerified())
                    .referenceType(review.getReferenceType())
                    .referenceInfo(referenceInfo)
                    .referenceCount(referenceCount)
                    .build();
        }
    }

    // 참고한 리뷰 정보
    @Getter
    @Builder
    @AllArgsConstructor
    public static class ReferenceInfo {
        private Long reviewId;
        private UserDto.SimpleResponse user;

        public static ReferenceInfo from(Long reviewId, com.foodreview.domain.user.entity.User user) {
            return ReferenceInfo.builder()
                    .reviewId(reviewId)
                    .user(UserDto.SimpleResponse.from(user))
                    .build();
        }
    }

    // 영향력 통계
    @Getter
    @Builder
    @AllArgsConstructor
    public static class InfluenceStats {
        private Integer totalReferenceCount; // 내 리뷰가 참고된 총 횟수
        private Integer totalInfluencePoints; // 영향력으로 받은 총 포인트
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

        // 세부 별점 (1-5, 선택 사항)
        @DecimalMin(value = "1.0", message = "맛 별점은 1점 이상이어야 합니다")
        @DecimalMax(value = "5.0", message = "맛 별점은 5점 이하여야 합니다")
        private BigDecimal tasteRating;

        @DecimalMin(value = "1.0", message = "가격 별점은 1점 이상이어야 합니다")
        @DecimalMax(value = "5.0", message = "가격 별점은 5점 이하여야 합니다")
        private BigDecimal priceRating;

        @DecimalMin(value = "1.0", message = "분위기 별점은 1점 이상이어야 합니다")
        @DecimalMax(value = "5.0", message = "분위기 별점은 5점 이하여야 합니다")
        private BigDecimal atmosphereRating;

        @DecimalMin(value = "1.0", message = "친절도 별점은 1점 이상이어야 합니다")
        @DecimalMax(value = "5.0", message = "친절도 별점은 5점 이하여야 합니다")
        private BigDecimal serviceRating;

        private List<String> images;

        @Size(max = 100, message = "메뉴명은 100자 이내로 작성해주세요")
        private String menu;

        @Size(max = 50, message = "가격은 50자 이내로 작성해주세요")
        private String price;

        private LocalDate visitDate;

        // 영수증 인증 이미지 URL (선택)
        private String receiptImageUrl;

        // 음식점을 알게 된 경로 (NONE, PASSING, FRIEND, REVIEW)
        private ReferenceType referenceType;

        // 참고한 리뷰 ID (referenceType이 REVIEW인 경우)
        private Long referenceReviewId;
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

        // 세부 별점 (1-5, 선택 사항)
        @DecimalMin(value = "1.0", message = "맛 별점은 1점 이상이어야 합니다")
        @DecimalMax(value = "5.0", message = "맛 별점은 5점 이하여야 합니다")
        private BigDecimal tasteRating;

        @DecimalMin(value = "1.0", message = "가격 별점은 1점 이상이어야 합니다")
        @DecimalMax(value = "5.0", message = "가격 별점은 5점 이하여야 합니다")
        private BigDecimal priceRating;

        @DecimalMin(value = "1.0", message = "분위기 별점은 1점 이상이어야 합니다")
        @DecimalMax(value = "5.0", message = "분위기 별점은 5점 이하여야 합니다")
        private BigDecimal atmosphereRating;

        @DecimalMin(value = "1.0", message = "친절도 별점은 1점 이상이어야 합니다")
        @DecimalMax(value = "5.0", message = "친절도 별점은 5점 이하여야 합니다")
        private BigDecimal serviceRating;

        private List<String> images;
        private String menu;
        private String price;
        private LocalDate visitDate;

        // 영수증 인증 이미지 URL
        private String receiptImageUrl;

        // 음식점을 알게 된 경로 (NONE, PASSING, FRIEND, REVIEW)
        private ReferenceType referenceType;

        // 참고한 리뷰 ID (referenceType이 REVIEW인 경우)
        private Long referenceReviewId;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class SympathyResponse {
        private Long reviewId;
        private Integer sympathyCount;
        private Boolean hasSympathized;
    }

    // 동별 리뷰 수 (지도 마커용)
    @Getter
    @AllArgsConstructor
    public static class NeighborhoodCount {
        private String neighborhood;
        private Long count;
    }

    // 구별 리뷰 수
    @Getter
    @AllArgsConstructor
    public static class DistrictCount {
        private String district;
        private Long count;
    }
}
