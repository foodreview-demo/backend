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

    // 특정 리뷰를 참고한 리뷰들 조회
    List<ReviewReference> findByReferenceReview(Review referenceReview);

    // 특정 리뷰를 참고한 횟수
    int countByReferenceReview(Review referenceReview);

    // 특정 사용자의 리뷰가 참고된 총 횟수 (영향력)
    @Query("SELECT COUNT(rr) FROM ReviewReference rr WHERE rr.referenceUser = :user")
    int countByReferenceUser(@Param("user") User user);

    // 특정 사용자가 받은 총 영향력 포인트
    @Query("SELECT COALESCE(SUM(rr.pointsAwarded), 0) FROM ReviewReference rr WHERE rr.referenceUser = :user")
    int sumPointsAwardedByReferenceUser(@Param("user") User user);

    // 상호 참고 체크: A가 B의 리뷰를 참고했는지 확인 (A의 리뷰 작성자가 reviewUser, B의 리뷰 작성자가 referenceUser)
    @Query("SELECT COUNT(rr) > 0 FROM ReviewReference rr " +
           "WHERE rr.review.user = :referenceUser AND rr.referenceUser = :reviewUser")
    boolean existsMutualReference(@Param("reviewUser") User reviewUser, @Param("referenceUser") User referenceUser);
}
