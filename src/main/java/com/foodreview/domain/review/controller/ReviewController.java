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

import java.util.List;

@Tag(name = "Review", description = "리뷰 API")
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "리뷰 목록 조회 (동/구/시 단위 필터링 지원)")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ReviewDto.Response>>> getReviews(
            @RequestParam(name = "region", required = false) String region,
            @RequestParam(name = "district", required = false) String district,
            @RequestParam(name = "neighborhood", required = false) String neighborhood,
            @RequestParam(name = "category", required = false) String category,
            @CurrentUser CustomUserDetails userDetails,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Long userId = userDetails != null ? userDetails.getUserId() : null;
        PageResponse<ReviewDto.Response> response = reviewService.getReviews(region, district, neighborhood, category, userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "동별 리뷰 수 조회 (지도 마커용)")
    @GetMapping("/count-by-neighborhood")
    public ResponseEntity<ApiResponse<List<ReviewDto.NeighborhoodCount>>> getReviewCountByNeighborhood(
            @RequestParam(name = "region") String region,
            @RequestParam(name = "district") String district) {
        List<ReviewDto.NeighborhoodCount> response = reviewService.getReviewCountByNeighborhood(region, district);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "구별 리뷰 수 조회")
    @GetMapping("/count-by-district")
    public ResponseEntity<ApiResponse<List<ReviewDto.DistrictCount>>> getReviewCountByDistrict(
            @RequestParam(name = "region") String region) {
        List<ReviewDto.DistrictCount> response = reviewService.getReviewCountByDistrict(region);
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
    @PostMapping("/{reviewId}/sympathize")
    public ResponseEntity<ApiResponse<ReviewDto.SympathyResponse>> addSympathy(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable("reviewId") Long reviewId) {
        ReviewDto.SympathyResponse response = reviewService.addSympathy(userDetails.getUserId(), reviewId);
        return ResponseEntity.ok(ApiResponse.success(response, "공감했습니다"));
    }

    @Operation(summary = "리뷰 공감 취소")
    @DeleteMapping("/{reviewId}/sympathize")
    public ResponseEntity<ApiResponse<ReviewDto.SympathyResponse>> removeSympathy(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable("reviewId") Long reviewId) {
        ReviewDto.SympathyResponse response = reviewService.removeSympathy(userDetails.getUserId(), reviewId);
        return ResponseEntity.ok(ApiResponse.success(response, "공감을 취소했습니다"));
    }
}
