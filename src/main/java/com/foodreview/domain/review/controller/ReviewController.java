package com.foodreview.domain.review.controller;

import com.foodreview.domain.review.dto.ReviewDto;
import com.foodreview.domain.review.service.ReviewService;
import com.foodreview.global.common.ApiResponse;
import com.foodreview.global.common.PageResponse;
import com.foodreview.global.security.CurrentUser;
import com.foodreview.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Review", description = "리뷰 API")
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "리뷰 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ReviewDto.Response>>> getReviews(
            @RequestParam(name = "region", required = false) String region,
            @RequestParam(name = "category", required = false) String category,
            @CurrentUser CustomUserDetails userDetails,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Long userId = userDetails != null ? userDetails.getUserId() : null;
        PageResponse<ReviewDto.Response> response = reviewService.getReviews(region, category, userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "리뷰 상세 조회")
    @GetMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<ReviewDto.Response>> getReview(
            @PathVariable("reviewId") Long reviewId,
            @CurrentUser CustomUserDetails userDetails) {
        Long userId = userDetails != null ? userDetails.getUserId() : null;
        ReviewDto.Response response = reviewService.getReview(reviewId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "음식점 리뷰 조회")
    @GetMapping("/restaurant/{restaurantId}")
    public ResponseEntity<ApiResponse<PageResponse<ReviewDto.Response>>> getRestaurantReviews(
            @PathVariable("restaurantId") Long restaurantId,
            @CurrentUser CustomUserDetails userDetails,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Long userId = userDetails != null ? userDetails.getUserId() : null;
        PageResponse<ReviewDto.Response> response = reviewService.getRestaurantReviews(restaurantId, userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "사용자 리뷰 조회")
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<PageResponse<ReviewDto.Response>>> getUserReviews(
            @PathVariable("userId") Long userId,
            @CurrentUser CustomUserDetails userDetails,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Long currentUserId = userDetails != null ? userDetails.getUserId() : null;
        PageResponse<ReviewDto.Response> response = reviewService.getUserReviews(userId, currentUserId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "리뷰 작성")
    @PostMapping
    public ResponseEntity<ApiResponse<ReviewDto.Response>> createReview(
            @CurrentUser CustomUserDetails userDetails,
            @Valid @RequestBody ReviewDto.CreateRequest request) {
        ReviewDto.Response response = reviewService.createReview(userDetails.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response, "리뷰가 등록되었습니다"));
    }

    @Operation(summary = "리뷰 수정")
    @PutMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<ReviewDto.Response>> updateReview(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable("reviewId") Long reviewId,
            @Valid @RequestBody ReviewDto.UpdateRequest request) {
        ReviewDto.Response response = reviewService.updateReview(userDetails.getUserId(), reviewId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "리뷰가 수정되었습니다"));
    }

    @Operation(summary = "리뷰 삭제")
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable("reviewId") Long reviewId) {
        reviewService.deleteReview(userDetails.getUserId(), reviewId);
        return ResponseEntity.ok(ApiResponse.success(null, "리뷰가 삭제되었습니다"));
    }

    @Operation(summary = "리뷰 공감")
    @PostMapping("/{reviewId}/sympathy")
    public ResponseEntity<ApiResponse<Void>> addSympathy(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable("reviewId") Long reviewId) {
        reviewService.addSympathy(userDetails.getUserId(), reviewId);
        return ResponseEntity.ok(ApiResponse.success(null, "공감했습니다"));
    }

    @Operation(summary = "리뷰 공감 취소")
    @DeleteMapping("/{reviewId}/sympathy")
    public ResponseEntity<ApiResponse<Void>> removeSympathy(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable("reviewId") Long reviewId) {
        reviewService.removeSympathy(userDetails.getUserId(), reviewId);
        return ResponseEntity.ok(ApiResponse.success(null, "공감을 취소했습니다"));
    }
}
