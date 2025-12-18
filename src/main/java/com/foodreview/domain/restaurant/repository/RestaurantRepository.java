package com.foodreview.domain.restaurant.repository;

import com.foodreview.domain.restaurant.entity.Restaurant;
import com.foodreview.domain.restaurant.entity.Restaurant.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    // 카카오 Place ID로 조회
    Optional<Restaurant> findByKakaoPlaceId(String kakaoPlaceId);

    // 지역별 음식점 조회 - 시/도 단위
    Page<Restaurant> findByRegion(String region, Pageable pageable);

    // 지역별 음식점 조회 - 구/군 단위
    Page<Restaurant> findByRegionAndDistrict(String region, String district, Pageable pageable);

    // 지역별 음식점 조회 - 동 단위
    Page<Restaurant> findByRegionAndDistrictAndNeighborhood(String region, String district, String neighborhood, Pageable pageable);

    // 카테고리별 음식점 조회
    Page<Restaurant> findByCategory(Category category, Pageable pageable);

    // 지역 + 카테고리 필터 (시/도 단위)
    Page<Restaurant> findByRegionAndCategory(String region, Category category, Pageable pageable);

    // 지역 + 카테고리 필터 (구/군 단위)
    Page<Restaurant> findByRegionAndDistrictAndCategory(String region, String district, Category category, Pageable pageable);

    // 지역 + 카테고리 필터 (동 단위)
    Page<Restaurant> findByRegionAndDistrictAndNeighborhoodAndCategory(String region, String district, String neighborhood, Category category, Pageable pageable);

    // 검색 (이름, 주소 포함)
    @Query("SELECT r FROM Restaurant r WHERE " +
           "r.name LIKE %:keyword% OR r.address LIKE %:keyword%")
    Page<Restaurant> search(@Param("keyword") String keyword, Pageable pageable);

    // 검색 + 지역 필터 (동적 쿼리)
    @Query("SELECT r FROM Restaurant r WHERE " +
           "(r.name LIKE %:keyword% OR r.address LIKE %:keyword%) " +
           "AND r.region = :region " +
           "AND (:district IS NULL OR r.district = :district) " +
           "AND (:neighborhood IS NULL OR r.neighborhood = :neighborhood)")
    Page<Restaurant> searchByLocation(
            @Param("keyword") String keyword,
            @Param("region") String region,
            @Param("district") String district,
            @Param("neighborhood") String neighborhood,
            Pageable pageable);

    // 검색 + 카테고리 필터
    @Query("SELECT r FROM Restaurant r WHERE " +
           "(r.name LIKE %:keyword% OR r.address LIKE %:keyword%) " +
           "AND r.category = :category")
    Page<Restaurant> searchByCategory(@Param("keyword") String keyword, @Param("category") Category category, Pageable pageable);

    // 검색 + 지역 + 카테고리 필터 (동적 쿼리)
    @Query("SELECT r FROM Restaurant r WHERE " +
           "(r.name LIKE %:keyword% OR r.address LIKE %:keyword%) " +
           "AND r.region = :region " +
           "AND (:district IS NULL OR r.district = :district) " +
           "AND (:neighborhood IS NULL OR r.neighborhood = :neighborhood) " +
           "AND r.category = :category")
    Page<Restaurant> searchByLocationAndCategory(
            @Param("keyword") String keyword,
            @Param("region") String region,
            @Param("district") String district,
            @Param("neighborhood") String neighborhood,
            @Param("category") Category category,
            Pageable pageable);

    // 첫 리뷰 가능한 음식점 (리뷰가 없는 음식점)
    Page<Restaurant> findByReviewCount(Integer reviewCount, Pageable pageable);

    // 평점순 정렬
    Page<Restaurant> findAllByOrderByAverageRatingDesc(Pageable pageable);
}
