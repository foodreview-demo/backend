package com.foodreview.domain.badge.controller;

import com.foodreview.domain.badge.dto.BadgeDto;
import com.foodreview.domain.badge.entity.BadgeCategory;
import com.foodreview.domain.badge.service.BadgeService;
import com.foodreview.global.common.ApiResponse;
import com.foodreview.global.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/badges")
@RequiredArgsConstructor
public class BadgeController {

    private final BadgeService badgeService;

    // 전체 배지 목록 조회 (획득 여부 포함)
    @GetMapping
    public ResponseEntity<ApiResponse<List<BadgeDto.Response>>> getAllBadges(
            @CurrentUser Long userId) {
        List<BadgeDto.Response> badges = badgeService.getAllBadges(userId);
        return ResponseEntity.ok(ApiResponse.success(badges));
    }

    // 카테고리별 배지 조회
    @GetMapping("/category/{category}")
    public ResponseEntity<ApiResponse<List<BadgeDto.Response>>> getBadgesByCategory(
            @CurrentUser Long userId,
            @PathVariable String category) {
        BadgeCategory badgeCategory = BadgeCategory.valueOf(category.toUpperCase());
        List<BadgeDto.Response> badges = badgeService.getBadgesByCategory(userId, badgeCategory);
        return ResponseEntity.ok(ApiResponse.success(badges));
    }

    // 내가 획득한 배지 목록
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<BadgeDto.Response>>> getMyBadges(
            @CurrentUser Long userId) {
        List<BadgeDto.Response> badges = badgeService.getAcquiredBadges(userId);
        return ResponseEntity.ok(ApiResponse.success(badges));
    }

    // 특정 사용자의 표시 중인 배지
    @GetMapping("/user/{targetUserId}/displayed")
    public ResponseEntity<ApiResponse<List<BadgeDto.SimpleResponse>>> getDisplayedBadges(
            @PathVariable Long targetUserId) {
        List<BadgeDto.SimpleResponse> badges = badgeService.getDisplayedBadges(targetUserId);
        return ResponseEntity.ok(ApiResponse.success(badges));
    }

    // 배지 표시 여부 토글
    @PutMapping("/{badgeId}/display")
    public ResponseEntity<ApiResponse<Void>> toggleBadgeDisplay(
            @CurrentUser Long userId,
            @PathVariable Long badgeId,
            @RequestBody BadgeDto.DisplayRequest request) {
        badgeService.toggleBadgeDisplay(userId, badgeId, request.getDisplay());
        return ResponseEntity.ok(ApiResponse.success(null, "배지 표시 설정이 변경되었습니다"));
    }
}
