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

    // 내가 공감한 리뷰의 작성자 + 공감 횟수 (나 자신 제외)
    @Query("SELECT s.review.user.id, COUNT(s.review.user.id) as sympathyCount " +
           "FROM Sympathy s " +
           "WHERE s.user.id = :userId AND s.review.user.id != :userId " +
           "GROUP BY s.review.user.id " +
           "ORDER BY sympathyCount DESC")
    List<Object[]> findSympathizedAuthors(@Param("userId") Long userId);

    // 내 리뷰에 공감한 사용자 + 공감 횟수 (나 자신 제외)
    @Query("SELECT s.user.id, COUNT(s.user.id) as sympathyCount " +
           "FROM Sympathy s " +
           "WHERE s.review.user.id = :userId AND s.user.id != :userId " +
           "GROUP BY s.user.id " +
           "ORDER BY sympathyCount DESC")
    List<Object[]> findSympathizers(@Param("userId") Long userId);
}
