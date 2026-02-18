package com.foodreview.domain.review.repository;

import com.foodreview.domain.restaurant.entity.Restaurant;
import com.foodreview.domain.review.entity.ReceiptVerificationStatus;
import com.foodreview.domain.review.entity.Review;
import com.foodreview.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

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

    // 동(neighborhood) 단위 필터링 - 단일 동
    @Query("SELECT r FROM Review r WHERE r.restaurant.neighborhood = :neighborhood ORDER BY r.createdAt DESC")
    Page<Review> findByNeighborhood(@Param("neighborhood") String neighborhood, Pageable pageable);

    // 동 + 카테고리 필터링 - 단일 동
    @Query("SELECT r FROM Review r WHERE r.restaurant.neighborhood = :neighborhood AND r.restaurant.category = :category ORDER BY r.createdAt DESC")
    Page<Review> findByNeighborhoodAndCategory(
            @Param("neighborhood") String neighborhood,
            @Param("category") Restaurant.Category category,
            Pageable pageable);

    // 동(neighborhood) 단위 필터링 - 복수 동 (IN 절)
    @Query("SELECT r FROM Review r WHERE r.restaurant.neighborhood IN :neighborhoods ORDER BY r.createdAt DESC")
    Page<Review> findByNeighborhoodIn(@Param("neighborhoods") List<String> neighborhoods, Pageable pageable);

    // 동 + 카테고리 필터링 - 복수 동 (IN 절)
    @Query("SELECT r FROM Review r WHERE r.restaurant.neighborhood IN :neighborhoods AND r.restaurant.category = :category ORDER BY r.createdAt DESC")
    Page<Review> findByNeighborhoodInAndCategory(
            @Param("neighborhoods") List<String> neighborhoods,
            @Param("category") Restaurant.Category category,
            Pageable pageable);

    // 구(district) 단위 필터링
    @Query("SELECT r FROM Review r WHERE r.restaurant.district = :district ORDER BY r.createdAt DESC")
    Page<Review> findByDistrict(@Param("district") String district, Pageable pageable);

    // 구 + 카테고리 필터링
    @Query("SELECT r FROM Review r WHERE r.restaurant.district = :district AND r.restaurant.category = :category ORDER BY r.createdAt DESC")
    Page<Review> findByDistrictAndCategory(
            @Param("district") String district,
            @Param("category") Restaurant.Category category,
            Pageable pageable);

    // 동별 리뷰 수 집계 (지도 마커용)
    @Query("SELECT r.restaurant.neighborhood, COUNT(r) FROM Review r " +
           "WHERE r.restaurant.region = :region AND r.restaurant.district = :district " +
           "AND r.restaurant.neighborhood IS NOT NULL " +
           "GROUP BY r.restaurant.neighborhood")
    List<Object[]> countByNeighborhood(@Param("region") String region, @Param("district") String district);

    // 구별 리뷰 수 집계
    @Query("SELECT r.restaurant.district, COUNT(r) FROM Review r " +
           "WHERE r.restaurant.region = :region " +
           "AND r.restaurant.district IS NOT NULL " +
           "GROUP BY r.restaurant.district")
    List<Object[]> countByDistrict(@Param("region") String region);

    // 영수증 검증 상태별 리뷰 조회 (Admin용)
    @Query("SELECT r FROM Review r WHERE r.receiptVerificationStatus = :status ORDER BY r.createdAt DESC")
    Page<Review> findByReceiptVerificationStatus(
            @Param("status") ReceiptVerificationStatus status,
            Pageable pageable);

    // 영수증 검증 대기 리뷰 수
    @Query("SELECT COUNT(r) FROM Review r WHERE r.receiptVerificationStatus = :status")
    long countByReceiptVerificationStatus(@Param("status") ReceiptVerificationStatus status);

    // 팔로잉 사용자의 리뷰 조회
    @Query("SELECT r FROM Review r WHERE r.user.id IN :userIds ORDER BY r.createdAt DESC")
    Page<Review> findByUserIdIn(@Param("userIds") List<Long> userIds, Pageable pageable);

    // 팔로잉 사용자의 리뷰 조회 + 카테고리 필터
    @Query("SELECT r FROM Review r WHERE r.user.id IN :userIds AND r.restaurant.category = :category ORDER BY r.createdAt DESC")
    Page<Review> findByUserIdInAndCategory(
            @Param("userIds") List<Long> userIds,
            @Param("category") Restaurant.Category category,
            Pageable pageable);

    // 같은 음식점에 리뷰를 작성한 다른 사용자 목록 + 공통 음식점 수 (나 자신 제외)
    @Query("SELECT r2.user.id, COUNT(DISTINCT r2.restaurant.id) as commonCount " +
           "FROM Review r1 " +
           "JOIN Review r2 ON r1.restaurant = r2.restaurant " +
           "WHERE r1.user.id = :userId AND r2.user.id != :userId " +
           "GROUP BY r2.user.id " +
           "HAVING COUNT(DISTINCT r2.restaurant.id) >= :minCommon " +
           "ORDER BY commonCount DESC")
    List<Object[]> findUsersWithCommonRestaurants(@Param("userId") Long userId, @Param("minCommon") int minCommon);

    // 두 사용자 간 공통 음식점의 별점 정보 조회 (취향 유사도 계산용)
    @Query("SELECT r1.restaurant.id, r1.rating, r2.rating " +
           "FROM Review r1 " +
           "JOIN Review r2 ON r1.restaurant = r2.restaurant " +
           "WHERE r1.user.id = :userId1 AND r2.user.id = :userId2")
    List<Object[]> findCommonRestaurantRatings(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    // 사용자가 리뷰한 음식점 ID 목록
    @Query("SELECT r.restaurant.id FROM Review r WHERE r.user.id = :userId")
    List<Long> findRestaurantIdsByUserId(@Param("userId") Long userId);

    // 팔로잉 사용자들이 리뷰한 음식점의 kakaoPlaceId 목록 (중복 제거)
    @Query("SELECT DISTINCT r.restaurant.kakaoPlaceId FROM Review r WHERE r.user.id IN :userIds AND r.restaurant.kakaoPlaceId IS NOT NULL")
    List<String> findKakaoPlaceIdsByUserIds(@Param("userIds") List<Long> userIds);

    // 특정 음식점에 리뷰를 남긴 사용자 ID 목록 (중복 제거)
    @Query("SELECT DISTINCT r.user.id FROM Review r WHERE r.restaurant.id = :restaurantId")
    List<Long> findReviewerIdsByRestaurantId(@Param("restaurantId") Long restaurantId);

    // 취향 분석용 리뷰 데이터 조회 (배치용)
    @Query("SELECT r.restaurant.id, r.restaurant.category, r.rating, r.createdAt, r.visitDate, r.content, r.priceRating " +
           "FROM Review r " +
           "WHERE r.user.id = :userId AND r.createdAt >= :since " +
           "ORDER BY r.createdAt DESC")
    List<Object[]> findReviewDataForTasteAnalysis(@Param("userId") Long userId, @Param("since") java.time.LocalDateTime since);

    // 사용자별 전체 리뷰 조회 (취향 프로필 배치용)
    @Query("SELECT r FROM Review r WHERE r.user.id = :userId ORDER BY r.createdAt DESC")
    List<Review> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);
}
