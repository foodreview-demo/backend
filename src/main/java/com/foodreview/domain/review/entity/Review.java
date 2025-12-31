package com.foodreview.domain.review.entity;

import com.foodreview.domain.common.BaseTimeEntity;
import com.foodreview.domain.restaurant.entity.Restaurant;
import com.foodreview.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "reviews",
        indexes = {
            @Index(name = "idx_review_restaurant_created", columnList = "restaurant_id, created_at DESC"),
            @Index(name = "idx_review_user_created", columnList = "user_id, created_at DESC")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Review extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(nullable = false, precision = 2, scale = 1)
    private BigDecimal rating;

    // 세부 별점
    @Column(name = "taste_rating", precision = 2, scale = 1)
    private BigDecimal tasteRating;

    @Column(name = "price_rating", precision = 2, scale = 1)
    private BigDecimal priceRating;

    @Column(name = "atmosphere_rating", precision = 2, scale = 1)
    private BigDecimal atmosphereRating;

    @Column(name = "service_rating", precision = 2, scale = 1)
    private BigDecimal serviceRating;

    @ElementCollection
    @CollectionTable(name = "review_images", joinColumns = @JoinColumn(name = "review_id"))
    @Column(name = "image_url", length = 500)
    @Builder.Default
    private List<String> images = new ArrayList<>();

    @Column(length = 100)
    private String menu;

    @Column(length = 50)
    private String price;

    @Column(name = "visit_date")
    private LocalDate visitDate;

    @Column(name = "sympathy_count", nullable = false)
    @Builder.Default
    private Integer sympathyCount = 0;

    @Column(name = "is_first_review", nullable = false)
    @Builder.Default
    private Boolean isFirstReview = false;

    // 영수증 인증 이미지 URL (신규 리뷰는 필수, 기존 리뷰는 null 가능)
    @Column(name = "receipt_image_url", length = 500)
    private String receiptImageUrl;

    // 공감 수 증가
    public void addSympathy() {
        this.sympathyCount++;
    }

    // 공감 수 감소
    public void removeSympathy() {
        if (this.sympathyCount > 0) {
            this.sympathyCount--;
        }
    }

    // 리뷰 수정
    public void update(String content, BigDecimal rating, BigDecimal tasteRating, BigDecimal priceRating,
                       BigDecimal atmosphereRating, BigDecimal serviceRating,
                       List<String> images, String menu, String price, LocalDate visitDate,
                       String receiptImageUrl) {
        if (content != null) this.content = content;
        if (rating != null) this.rating = rating;
        if (tasteRating != null) this.tasteRating = tasteRating;
        if (priceRating != null) this.priceRating = priceRating;
        if (atmosphereRating != null) this.atmosphereRating = atmosphereRating;
        if (serviceRating != null) this.serviceRating = serviceRating;
        if (images != null) this.images = images;
        if (menu != null) this.menu = menu;
        if (price != null) this.price = price;
        if (visitDate != null) this.visitDate = visitDate;
        if (receiptImageUrl != null) this.receiptImageUrl = receiptImageUrl;
    }
}
