package com.foodreview.domain.restaurant.entity;

import com.foodreview.domain.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "restaurants")
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
