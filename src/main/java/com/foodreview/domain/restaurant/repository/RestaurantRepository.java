package com.foodreview.domain.restaurant.repository;

import com.foodreview.domain.restaurant.entity.Restaurant;
import com.foodreview.domain.restaurant.entity.Restaurant.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    // 지역별 음식점 조회
    Page<Restaurant> findByRegion(String region, Pageable pageable);

    // 카테고리별 음식점 조회
    Page<Restaurant> findByCategory(Category category, Pageable pageable);

    // 지역 + 카테고리 필터
    Page<Restaurant> findByRegionAndCategory(String region, Category category, Pageable pageable);

    // 검색 (이름, 주소, 메뉴 포함)
    @Query("SELECT r FROM Restaurant r WHERE " +
           "r.name LIKE %:keyword% OR r.address LIKE %:keyword%")
    Page<Restaurant> search(@Param("keyword") String keyword, Pageable pageable);

    // 검색 + 지역 필터
    @Query("SELECT r FROM Restaurant r WHERE " +
           "(r.name LIKE %:keyword% OR r.address LIKE %:keyword%) " +
           "AND r.region = :region")
    Page<Restaurant> searchByRegion(@Param("keyword") String keyword, @Param("region") String region, Pageable pageable);

    // 검색 + 카테고리 필터
    @Query("SELECT r FROM Restaurant r WHERE " +
           "(r.name LIKE %:keyword% OR r.address LIKE %:keyword%) " +
           "AND r.category = :category")
    Page<Restaurant> searchByCategory(@Param("keyword") String keyword, @Param("category") Category category, Pageable pageable);

    // 검색 + 지역 + 카테고리 필터
    @Query("SELECT r FROM Restaurant r WHERE " +
           "(r.name LIKE %:keyword% OR r.address LIKE %:keyword%) " +
           "AND r.region = :region AND r.category = :category")
    Page<Restaurant> searchByRegionAndCategory(
            @Param("keyword") String keyword,
            @Param("region") String region,
            @Param("category") Category category,
            Pageable pageable);

    // 첫 리뷰 가능한 음식점 (리뷰가 없는 음식점)
    Page<Restaurant> findByReviewCount(Integer reviewCount, Pageable pageable);

    // 평점순 정렬
    Page<Restaurant> findAllByOrderByAverageRatingDesc(Pageable pageable);
}
