package com.foodreview.domain.restaurant.entity;

import com.foodreview.domain.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "restaurants",
        indexes = {
            @Index(name = "idx_restaurant_region_category", columnList = "region, category"),
            @Index(name = "idx_restaurant_category", columnList = "category")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Restaurant extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Column(nullable = false, length = 200)
    private String address;

    @Column(nullable = false, length = 50)
    private String region;

    @Column(length = 50)
    private String district;

    @Column(length = 50)
    private String neighborhood;

    @Column(length = 500)
    private String thumbnail;

    @Column(name = "average_rating", precision = 2, scale = 1)
    @Builder.Default
    private BigDecimal averageRating = BigDecimal.ZERO;

    // 세부 평균 평점
    @Column(name = "average_taste_rating", precision = 2, scale = 1)
    @Builder.Default
    private BigDecimal averageTasteRating = BigDecimal.ZERO;

    @Column(name = "average_price_rating", precision = 2, scale = 1)
    @Builder.Default
    private BigDecimal averagePriceRating = BigDecimal.ZERO;

    @Column(name = "average_atmosphere_rating", precision = 2, scale = 1)
    @Builder.Default
    private BigDecimal averageAtmosphereRating = BigDecimal.ZERO;

    @Column(name = "average_service_rating", precision = 2, scale = 1)
    @Builder.Default
    private BigDecimal averageServiceRating = BigDecimal.ZERO;

    // 세부 별점 개수 (각각 null이 아닌 리뷰 수)
    @Column(name = "taste_rating_count", nullable = false)
    @Builder.Default
    private Integer tasteRatingCount = 0;

    @Column(name = "price_rating_count", nullable = false)
    @Builder.Default
    private Integer priceRatingCount = 0;

    @Column(name = "atmosphere_rating_count", nullable = false)
    @Builder.Default
    private Integer atmosphereRatingCount = 0;

    @Column(name = "service_rating_count", nullable = false)
    @Builder.Default
    private Integer serviceRatingCount = 0;

    @Column(name = "review_count", nullable = false)
    @Builder.Default
    private Integer reviewCount = 0;

    @Column(name = "price_range", length = 50)
    private String priceRange;

    @Column(length = 20)
    private String phone;

    @Column(name = "business_hours", length = 200)
    private String businessHours;

    // 카카오맵 연동 정보
    @Column(name = "kakao_place_id", unique = true)
    private String kakaoPlaceId;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    // 리뷰 추가 시 평점 업데이트
    public void addReview(BigDecimal newRating) {
        BigDecimal totalRating = this.averageRating.multiply(BigDecimal.valueOf(this.reviewCount));
        this.reviewCount++;
        this.averageRating = totalRating.add(newRating)
                .divide(BigDecimal.valueOf(this.reviewCount), 1, java.math.RoundingMode.HALF_UP);
    }

    // 리뷰 추가 시 세부 별점 업데이트
    public void addDetailRatings(BigDecimal tasteRating, BigDecimal priceRating,
                                  BigDecimal atmosphereRating, BigDecimal serviceRating) {
        if (tasteRating != null) {
            BigDecimal currentAvg = this.averageTasteRating != null ? this.averageTasteRating : BigDecimal.ZERO;
            int currentCount = this.tasteRatingCount != null ? this.tasteRatingCount : 0;
            BigDecimal total = currentAvg.multiply(BigDecimal.valueOf(currentCount));
            this.tasteRatingCount = currentCount + 1;
            this.averageTasteRating = total.add(tasteRating)
                    .divide(BigDecimal.valueOf(this.tasteRatingCount), 1, java.math.RoundingMode.HALF_UP);
        }
        if (priceRating != null) {
            BigDecimal currentAvg = this.averagePriceRating != null ? this.averagePriceRating : BigDecimal.ZERO;
            int currentCount = this.priceRatingCount != null ? this.priceRatingCount : 0;
            BigDecimal total = currentAvg.multiply(BigDecimal.valueOf(currentCount));
            this.priceRatingCount = currentCount + 1;
            this.averagePriceRating = total.add(priceRating)
                    .divide(BigDecimal.valueOf(this.priceRatingCount), 1, java.math.RoundingMode.HALF_UP);
        }
        if (atmosphereRating != null) {
            BigDecimal currentAvg = this.averageAtmosphereRating != null ? this.averageAtmosphereRating : BigDecimal.ZERO;
            int currentCount = this.atmosphereRatingCount != null ? this.atmosphereRatingCount : 0;
            BigDecimal total = currentAvg.multiply(BigDecimal.valueOf(currentCount));
            this.atmosphereRatingCount = currentCount + 1;
            this.averageAtmosphereRating = total.add(atmosphereRating)
                    .divide(BigDecimal.valueOf(this.atmosphereRatingCount), 1, java.math.RoundingMode.HALF_UP);
        }
        if (serviceRating != null) {
            BigDecimal currentAvg = this.averageServiceRating != null ? this.averageServiceRating : BigDecimal.ZERO;
            int currentCount = this.serviceRatingCount != null ? this.serviceRatingCount : 0;
            BigDecimal total = currentAvg.multiply(BigDecimal.valueOf(currentCount));
            this.serviceRatingCount = currentCount + 1;
            this.averageServiceRating = total.add(serviceRating)
                    .divide(BigDecimal.valueOf(this.serviceRatingCount), 1, java.math.RoundingMode.HALF_UP);
        }
    }

    // 리뷰 삭제 시 평점 업데이트
    public void removeReview(BigDecimal removedRating) {
        if (this.reviewCount <= 1) {
            this.averageRating = BigDecimal.ZERO;
            this.reviewCount = 0;
        } else {
            BigDecimal totalRating = this.averageRating.multiply(BigDecimal.valueOf(this.reviewCount));
            this.reviewCount--;
            this.averageRating = totalRating.subtract(removedRating)
                    .divide(BigDecimal.valueOf(this.reviewCount), 1, java.math.RoundingMode.HALF_UP);
        }
    }

    // 리뷰 삭제 시 세부 별점 업데이트
    public void removeDetailRatings(BigDecimal tasteRating, BigDecimal priceRating,
                                     BigDecimal atmosphereRating, BigDecimal serviceRating) {
        int tasteCount = this.tasteRatingCount != null ? this.tasteRatingCount : 0;
        if (tasteRating != null && tasteCount > 0) {
            if (tasteCount <= 1) {
                this.averageTasteRating = BigDecimal.ZERO;
                this.tasteRatingCount = 0;
            } else {
                BigDecimal currentAvg = this.averageTasteRating != null ? this.averageTasteRating : BigDecimal.ZERO;
                BigDecimal total = currentAvg.multiply(BigDecimal.valueOf(tasteCount));
                this.tasteRatingCount = tasteCount - 1;
                this.averageTasteRating = total.subtract(tasteRating)
                        .divide(BigDecimal.valueOf(this.tasteRatingCount), 1, java.math.RoundingMode.HALF_UP);
            }
        }
        int priceCount = this.priceRatingCount != null ? this.priceRatingCount : 0;
        if (priceRating != null && priceCount > 0) {
            if (priceCount <= 1) {
                this.averagePriceRating = BigDecimal.ZERO;
                this.priceRatingCount = 0;
            } else {
                BigDecimal currentAvg = this.averagePriceRating != null ? this.averagePriceRating : BigDecimal.ZERO;
                BigDecimal total = currentAvg.multiply(BigDecimal.valueOf(priceCount));
                this.priceRatingCount = priceCount - 1;
                this.averagePriceRating = total.subtract(priceRating)
                        .divide(BigDecimal.valueOf(this.priceRatingCount), 1, java.math.RoundingMode.HALF_UP);
            }
        }
        int atmosphereCount = this.atmosphereRatingCount != null ? this.atmosphereRatingCount : 0;
        if (atmosphereRating != null && atmosphereCount > 0) {
            if (atmosphereCount <= 1) {
                this.averageAtmosphereRating = BigDecimal.ZERO;
                this.atmosphereRatingCount = 0;
            } else {
                BigDecimal currentAvg = this.averageAtmosphereRating != null ? this.averageAtmosphereRating : BigDecimal.ZERO;
                BigDecimal total = currentAvg.multiply(BigDecimal.valueOf(atmosphereCount));
                this.atmosphereRatingCount = atmosphereCount - 1;
                this.averageAtmosphereRating = total.subtract(atmosphereRating)
                        .divide(BigDecimal.valueOf(this.atmosphereRatingCount), 1, java.math.RoundingMode.HALF_UP);
            }
        }
        int serviceCount = this.serviceRatingCount != null ? this.serviceRatingCount : 0;
        if (serviceRating != null && serviceCount > 0) {
            if (serviceCount <= 1) {
                this.averageServiceRating = BigDecimal.ZERO;
                this.serviceRatingCount = 0;
            } else {
                BigDecimal currentAvg = this.averageServiceRating != null ? this.averageServiceRating : BigDecimal.ZERO;
                BigDecimal total = currentAvg.multiply(BigDecimal.valueOf(serviceCount));
                this.serviceRatingCount = serviceCount - 1;
                this.averageServiceRating = total.subtract(serviceRating)
                        .divide(BigDecimal.valueOf(this.serviceRatingCount), 1, java.math.RoundingMode.HALF_UP);
            }
        }
    }

    public boolean isFirstReviewAvailable() {
        return this.reviewCount == 0;
    }

    public enum Category {
        KOREAN("한식"),
        JAPANESE("일식"),
        CHINESE("중식"),
        WESTERN("양식"),
        CAFE("카페"),
        BAKERY("베이커리"),
        SNACK("분식");

        private final String displayName;

        Category(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
