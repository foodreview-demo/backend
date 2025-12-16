package com.foodreview.domain.review.repository;

import com.foodreview.domain.restaurant.entity.Restaurant;
import com.foodreview.domain.review.entity.Review;
import com.foodreview.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    // 사용자별 리뷰 조회
    Page<Review> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    // 음식점별 리뷰 조회
    Page<Review> findByRestaurantOrderByCreatedAtDesc(Restaurant restaurant, Pageable pageable);

    // 지역별 리뷰 조회 (최신순) - region이 "서울"이면 "서울 강남구" 등도 매칭
    @Query("SELECT r FROM Review r WHERE r.restaurant.region LIKE :region% ORDER BY r.createdAt DESC")
    Page<Review> findByRegion(@Param("region") String region, Pageable pageable);

    // 카테고리별 리뷰 조회
    @Query("SELECT r FROM Review r WHERE r.restaurant.category = :category ORDER BY r.createdAt DESC")
    Page<Review> findByCategory(@Param("category") Restaurant.Category category, Pageable pageable);

    // 지역 + 카테고리 필터
    @Query("SELECT r FROM Review r WHERE r.restaurant.region LIKE :region% AND r.restaurant.category = :category ORDER BY r.createdAt DESC")
    Page<Review> findByRegionAndCategory(
            @Param("region") String region,
            @Param("category") Restaurant.Category category,
            Pageable pageable);

    // 전체 리뷰 조회 (최신순)
    Page<Review> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // 음식점에 사용자가 이미 리뷰를 작성했는지 확인
    boolean existsByUserAndRestaurant(User user, Restaurant restaurant);
}
