package com.foodreview.domain.review.repository;

import com.foodreview.domain.review.entity.Review;
import com.foodreview.domain.review.entity.ReviewReference;
import com.foodreview.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewReferenceRepository extends JpaRepository<ReviewReference, Long> {

    // 특정 리뷰가 참고한 리뷰 조회
    Optional<ReviewReference> findByReview(Review review);

    // 특정 리뷰의 참고 정보 삭제
    void deleteByReview(Review review);

    // 여러 리뷰가 참고한 리뷰들을 배치로 조회 (N+1 방지)
    @Query("SELECT rr FROM ReviewReference rr " +
           "JOIN FETCH rr.referenceReview " +
           "JOIN FETCH rr.referenceUser " +
           "WHERE rr.review.id IN :reviewIds")
    List<ReviewReference> findByReviewIds(@Param("reviewIds") List<Long> reviewIds);

    // 특정 리뷰를 참고한 리뷰들 조회
    List<ReviewReference> findByReferenceReview(Review referenceReview);

    // 특정 리뷰를 참고한 횟수
    int countByReferenceReview(Review referenceReview);

    // 여러 리뷰가 참고된 횟수를 배치로 조회 (N+1 방지)
    @Query("SELECT rr.referenceReview.id, COUNT(rr) FROM ReviewReference rr " +
           "WHERE rr.referenceReview.id IN :reviewIds GROUP BY rr.referenceReview.id")
    List<Object[]> countByReferenceReviewIds(@Param("reviewIds") List<Long> reviewIds);

    // 특정 사용자의 리뷰가 참고된 총 횟수 (영향력)
    @Query("SELECT COUNT(rr) FROM ReviewReference rr WHERE rr.referenceUser = :user")
    int countByReferenceUser(@Param("user") User user);

    // 특정 사용자가 받은 총 영향력 포인트
    @Query("SELECT COALESCE(SUM(rr.pointsAwarded), 0) FROM ReviewReference rr WHERE rr.referenceUser = :user")
    int sumPointsAwardedByReferenceUser(@Param("user") User user);

    // 사용자 영향력 통계 조회 (횟수 + 포인트 한 번에) - 쿼리 최적화
    @Query("SELECT COUNT(rr), COALESCE(SUM(rr.pointsAwarded), 0) FROM ReviewReference rr WHERE rr.referenceUser.id = :userId")
    List<Object[]> getInfluenceStatsByUserId(@Param("userId") Long userId);

    // 상호 참고 체크: A가 B의 리뷰를 참고했는지 확인 (A의 리뷰 작성자가 reviewUser, B의 리뷰 작성자가 referenceUser)
    @Query("SELECT COUNT(rr) > 0 FROM ReviewReference rr " +
           "WHERE rr.review.user = :referenceUser AND rr.referenceUser = :reviewUser")
    boolean existsMutualReference(@Param("reviewUser") User reviewUser, @Param("referenceUser") User referenceUser);
}
