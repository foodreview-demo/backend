package com.foodreview.domain.review.controller;

import com.foodreview.domain.review.dto.CommentDto;
import com.foodreview.domain.review.service.CommentService;
import com.foodreview.global.common.ApiResponse;
import com.foodreview.global.common.PageResponse;
import com.foodreview.global.security.CurrentUser;
import com.foodreview.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Comment", description = "댓글 API")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "댓글 작성")
    @PostMapping("/reviews/{reviewId}/comments")
    public ResponseEntity<ApiResponse<CommentDto.Response>> createComment(
            @PathVariable Long reviewId,
            @Valid @RequestBody CommentDto.CreateRequest request,
            @CurrentUser CustomUserDetails userDetails) {

        CommentDto.Response response = commentService.createComment(reviewId, request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "댓글이 작성되었습니다"));
    }

    @Operation(summary = "리뷰의 댓글 목록 조회")
    @GetMapping("/reviews/{reviewId}/comments")
    public ResponseEntity<ApiResponse<PageResponse<CommentDto.Response>>> getComments(
            @PathVariable Long reviewId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @CurrentUser CustomUserDetails userDetails) {

        Long currentUserId = userDetails != null ? userDetails.getUser().getId() : null;
        Pageable pageable = PageRequest.of(page, size);
        Page<CommentDto.Response> comments = commentService.getComments(reviewId, currentUserId, pageable);

        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(comments)));
    }

    @Operation(summary = "대댓글 목록 조회")
    @GetMapping("/comments/{commentId}/replies")
    public ResponseEntity<ApiResponse<PageResponse<CommentDto.Response>>> getReplies(
            @PathVariable Long commentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @CurrentUser CustomUserDetails userDetails) {

        Long currentUserId = userDetails != null ? userDetails.getUser().getId() : null;
        Pageable pageable = PageRequest.of(page, size);
        Page<CommentDto.Response> replies = commentService.getReplies(commentId, currentUserId, pageable);

        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(replies)));
    }

    @Operation(summary = "댓글 수정")
    @PutMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<CommentDto.Response>> updateComment(
            @PathVariable Long commentId,
            @Valid @RequestBody CommentDto.UpdateRequest request,
            @CurrentUser CustomUserDetails userDetails) {

        CommentDto.Response response = commentService.updateComment(commentId, request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response, "댓글이 수정되었습니다"));
    }

    @Operation(summary = "댓글 삭제")
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable Long commentId,
            @CurrentUser CustomUserDetails userDetails) {

        commentService.deleteComment(commentId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(null, "댓글이 삭제되었습니다"));
    }

    @Operation(summary = "리뷰의 댓글 수 조회")
    @GetMapping("/reviews/{reviewId}/comments/count")
    public ResponseEntity<ApiResponse<Long>> getCommentCount(@PathVariable Long reviewId) {
        long count = commentService.getCommentCount(reviewId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }
}
