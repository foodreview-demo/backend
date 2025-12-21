package com.foodreview.domain.review.service;

import com.foodreview.domain.notification.entity.Notification;
import com.foodreview.domain.notification.service.NotificationService;
import com.foodreview.domain.review.dto.CommentDto;
import com.foodreview.domain.review.entity.Comment;
import com.foodreview.domain.review.entity.Review;
import com.foodreview.domain.review.repository.CommentRepository;
import com.foodreview.domain.review.repository.ReviewRepository;
import com.foodreview.domain.user.entity.User;
import com.foodreview.domain.user.repository.UserRepository;
import com.foodreview.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    /**
     * 댓글 작성
     */
    @Transactional
    public CommentDto.Response createComment(Long reviewId, CommentDto.CreateRequest request, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException("사용자를 찾을 수 없습니다", HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new CustomException("리뷰를 찾을 수 없습니다", HttpStatus.NOT_FOUND, "REVIEW_NOT_FOUND"));

        Comment parent = null;
        if (request.getParentId() != null) {
            parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new CustomException("부모 댓글을 찾을 수 없습니다", HttpStatus.NOT_FOUND, "PARENT_COMMENT_NOT_FOUND"));

            // 부모 댓글이 같은 리뷰에 속하는지 확인
            if (!parent.getReview().getId().equals(reviewId)) {
                throw new CustomException("부모 댓글이 해당 리뷰에 속하지 않습니다", HttpStatus.BAD_REQUEST, "INVALID_PARENT_COMMENT");
            }

            // 대댓글의 대댓글은 허용하지 않음 (1단계만 허용)
            if (parent.getParent() != null) {
                throw new CustomException("대댓글에는 답글을 달 수 없습니다", HttpStatus.BAD_REQUEST, "NESTED_REPLY_NOT_ALLOWED");
            }
        }

        Comment comment = Comment.builder()
                .review(review)
                .user(user)
                .content(request.getContent())
                .parent(parent)
                .build();

        Comment savedComment = commentRepository.save(comment);
        log.debug("Comment created: reviewId={}, commentId={}, userId={}", reviewId, savedComment.getId(), user.getId());

        // 알림 생성
        if (parent != null) {
            // 대댓글인 경우: 부모 댓글 작성자에게 알림
            notificationService.createNotification(
                    parent.getUser(),
                    user,
                    Notification.NotificationType.REPLY,
                    String.format("%s님이 회원님의 댓글에 답글을 남겼습니다.", user.getName()),
                    reviewId
            );
        } else {
            // 일반 댓글인 경우: 리뷰 작성자에게 알림
            notificationService.createNotification(
                    review.getUser(),
                    user,
                    Notification.NotificationType.COMMENT,
                    String.format("%s님이 회원님의 리뷰에 댓글을 남겼습니다.", user.getName()),
                    reviewId
            );
        }

        return CommentDto.Response.from(savedComment, user.getId(), 0);
    }

    /**
     * 리뷰의 댓글 목록 조회 (최상위 댓글만)
     */
    public Page<CommentDto.Response> getComments(Long reviewId, Long currentUserId, Pageable pageable) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new CustomException("리뷰를 찾을 수 없습니다", HttpStatus.NOT_FOUND, "REVIEW_NOT_FOUND"));

        Page<Comment> comments = commentRepository.findByReviewAndParentIsNull(review, pageable);

        return comments.map(comment -> {
            int replyCount = (int) commentRepository.countByParent(comment);
            return CommentDto.Response.from(comment, currentUserId, replyCount);
        });
    }

    /**
     * 대댓글 목록 조회
     */
    public Page<CommentDto.Response> getReplies(Long commentId, Long currentUserId, Pageable pageable) {
        Comment parent = commentRepository.findById(commentId)
                .orElseThrow(() -> new CustomException("댓글을 찾을 수 없습니다", HttpStatus.NOT_FOUND, "COMMENT_NOT_FOUND"));

        Page<Comment> replies = commentRepository.findByParent(parent, pageable);

        return replies.map(reply -> CommentDto.Response.from(reply, currentUserId, 0));
    }

    /**
     * 댓글 수정
     */
    @Transactional
    public CommentDto.Response updateComment(Long commentId, CommentDto.UpdateRequest request, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException("사용자를 찾을 수 없습니다", HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CustomException("댓글을 찾을 수 없습니다", HttpStatus.NOT_FOUND, "COMMENT_NOT_FOUND"));

        // 삭제된 댓글인지 확인
        if (comment.getIsDeleted()) {
            throw new CustomException("삭제된 댓글은 수정할 수 없습니다", HttpStatus.BAD_REQUEST, "DELETED_COMMENT");
        }

        // 본인 댓글인지 확인
        if (!comment.getUser().getId().equals(user.getId())) {
            throw new CustomException("댓글을 수정할 권한이 없습니다", HttpStatus.FORBIDDEN, "FORBIDDEN");
        }

        comment.update(request.getContent());

        int replyCount = (int) commentRepository.countByParent(comment);
        return CommentDto.Response.from(comment, user.getId(), replyCount);
    }

    /**
     * 댓글 삭제
     */
    @Transactional
    public void deleteComment(Long commentId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException("사용자를 찾을 수 없습니다", HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CustomException("댓글을 찾을 수 없습니다", HttpStatus.NOT_FOUND, "COMMENT_NOT_FOUND"));

        // 이미 삭제된 댓글인지 확인
        if (comment.getIsDeleted()) {
            throw new CustomException("이미 삭제된 댓글입니다", HttpStatus.BAD_REQUEST, "ALREADY_DELETED");
        }

        // 본인 댓글 또는 리뷰 작성자인지 확인
        boolean isCommentOwner = comment.getUser().getId().equals(user.getId());
        boolean isReviewOwner = comment.getReview().getUser().getId().equals(user.getId());

        if (!isCommentOwner && !isReviewOwner) {
            throw new CustomException("댓글을 삭제할 권한이 없습니다", HttpStatus.FORBIDDEN, "FORBIDDEN");
        }

        // 대댓글이 있는지 확인
        long replyCount = commentRepository.countByParent(comment);

        if (replyCount > 0) {
            // 대댓글이 있으면 소프트 삭제 (내용만 변경)
            comment.softDelete();
            log.debug("Comment soft deleted: commentId={}, userId={}", commentId, user.getId());
        } else {
            // 대댓글이 없으면 실제 삭제
            commentRepository.delete(comment);
            log.debug("Comment hard deleted: commentId={}, userId={}", commentId, user.getId());
        }
    }

    /**
     * 리뷰의 댓글 수 조회 (삭제되지 않은 댓글만)
     */
    public long getCommentCount(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new CustomException("리뷰를 찾을 수 없습니다", HttpStatus.NOT_FOUND, "REVIEW_NOT_FOUND"));

        return commentRepository.countByReviewAndNotDeleted(review);
    }
}
