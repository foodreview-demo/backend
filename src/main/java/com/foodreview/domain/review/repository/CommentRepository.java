package com.foodreview.domain.review.repository;

import com.foodreview.domain.review.entity.Comment;
import com.foodreview.domain.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    // 리뷰의 최상위 댓글 조회 (부모가 없는 댓글) - User JOIN FETCH 추가
    @Query("SELECT c FROM Comment c JOIN FETCH c.user WHERE c.review = :review AND c.parent IS NULL ORDER BY c.createdAt ASC")
    Page<Comment> findByReviewAndParentIsNull(@Param("review") Review review, Pageable pageable);

    // 리뷰의 전체 댓글 수 조회 (삭제되지 않은 댓글만)
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.review = :review AND c.isDeleted = false")
    long countByReviewAndNotDeleted(@Param("review") Review review);

    // 특정 댓글의 대댓글 조회 - User JOIN FETCH 추가
    @Query("SELECT c FROM Comment c JOIN FETCH c.user WHERE c.parent = :parent ORDER BY c.createdAt ASC")
    Page<Comment> findByParent(@Param("parent") Comment parent, Pageable pageable);

    // 특정 댓글의 대댓글 수 조회
    long countByParent(Comment parent);
}
