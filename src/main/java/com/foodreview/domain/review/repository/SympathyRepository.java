package com.foodreview.domain.review.repository;

import com.foodreview.domain.review.entity.Review;
import com.foodreview.domain.review.entity.Sympathy;
import com.foodreview.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SympathyRepository extends JpaRepository<Sympathy, Long> {

    // 공감 여부 확인
    boolean existsByUserAndReview(User user, Review review);

    // 공감 조회
    Optional<Sympathy> findByUserAndReview(User user, Review review);

    // 사용자가 공감한 리뷰 ID 목록
    @Query("SELECT s.review.id FROM Sympathy s WHERE s.user.id = :userId")
    List<Long> findReviewIdsByUserId(@Param("userId") Long userId);
}
