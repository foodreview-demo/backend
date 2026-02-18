package com.foodreview.domain.recommendation.controller;

import com.foodreview.global.common.ApiResponse;
import com.foodreview.domain.recommendation.dto.RecommendationDto.*;
import com.foodreview.domain.recommendation.service.RecommendationService;
import com.foodreview.global.security.CurrentUser;
import com.foodreview.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI 음식점 추천 API
 */
@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    /**
     * 오늘의 추천 조회
     * GET /api/recommendations/today
     */
    @GetMapping("/today")
    public ResponseEntity<ApiResponse<Response>> getTodayRecommendation(
            @CurrentUser CustomUserDetails userDetails,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) String timeSlot
    ) {
        TodayRequest request = new TodayRequest(latitude, longitude, timeSlot);
        Response response = recommendationService.getTodayRecommendation(userDetails.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * AI에게 질문하기
     * POST /api/recommendations/ask
     */
    @PostMapping("/ask")
    public ResponseEntity<ApiResponse<Response>> askRecommendation(
            @CurrentUser CustomUserDetails userDetails,
            @RequestBody AskRequest request
    ) {
        Response response = recommendationService.askRecommendation(userDetails.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 추천 히스토리 조회
     * GET /api/recommendations/history
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<HistoryItem>>> getRecommendationHistory(
            @CurrentUser CustomUserDetails userDetails
    ) {
        List<HistoryItem> history = recommendationService.getRecommendationHistory(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    /**
     * 추천 피드백 저장
     * POST /api/recommendations/feedback
     */
    @PostMapping("/feedback")
    public ResponseEntity<ApiResponse<Void>> saveFeedback(
            @CurrentUser CustomUserDetails userDetails,
            @RequestBody FeedbackRequest request
    ) {
        recommendationService.saveFeedback(userDetails.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(null, "피드백이 저장되었습니다"));
    }
}
