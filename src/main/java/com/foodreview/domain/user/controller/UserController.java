package com.foodreview.domain.user.controller;

import com.foodreview.domain.review.dto.ReviewDto;
import com.foodreview.domain.review.service.ReviewService;
import com.foodreview.domain.user.dto.ScoreEventDto;
import com.foodreview.domain.user.dto.UserDto;
import com.foodreview.domain.user.service.UserService;
import com.foodreview.global.common.ApiResponse;
import com.foodreview.global.common.PageResponse;
import com.foodreview.global.security.CurrentUser;
import com.foodreview.global.security.CustomUserDetails;
import com.foodreview.global.exception.CustomException;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpStatus;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "User", description = "사용자 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final ReviewService reviewService;

    @Operation(summary = "내 정보 조회")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto.Response>> getMe(@CurrentUser CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new CustomException("인증되지 않은 사용자입니다", HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
        }
        UserDto.Response response = userService.getMyProfile(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "사용자 정보 조회")
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserDto.Response>> getUser(@PathVariable Long userId) {
        UserDto.Response response = userService.getUser(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "사용자 리뷰 조회")
    @GetMapping("/{userId}/reviews")
    public ResponseEntity<ApiResponse<PageResponse<ReviewDto.Response>>> getUserReviews(
            @PathVariable("userId") Long userId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Long currentUserId = userDetails != null ? userDetails.getUserId() : null;
        PageResponse<ReviewDto.Response> response = reviewService.getUserReviews(userId, currentUserId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "내 프로필 수정")
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserDto.Response>> updateProfile(
            @CurrentUser CustomUserDetails userDetails,
            @Valid @RequestBody UserDto.UpdateRequest request) {
        UserDto.Response response = userService.updateProfile(userDetails.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "팔로우")
    @PostMapping("/{userId}/follow")
    public ResponseEntity<ApiResponse<Void>> follow(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable Long userId) {
        userService.follow(userDetails.getUserId(), userId);
        return ResponseEntity.ok(ApiResponse.success(null, "팔로우했습니다"));
    }

    @Operation(summary = "언팔로우")
    @DeleteMapping("/{userId}/follow")
    public ResponseEntity<ApiResponse<Void>> unfollow(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable Long userId) {
        userService.unfollow(userDetails.getUserId(), userId);
        return ResponseEntity.ok(ApiResponse.success(null, "언팔로우했습니다"));
    }

    @Operation(summary = "팔로잉 목록 조회")
    @GetMapping("/{userId}/followings")
    public ResponseEntity<ApiResponse<PageResponse<UserDto.SimpleResponse>>> getFollowings(
            @PathVariable Long userId,
            @PageableDefault(size = 20) Pageable pageable) {
        PageResponse<UserDto.SimpleResponse> response = userService.getFollowings(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "팔로워 목록 조회")
    @GetMapping("/{userId}/followers")
    public ResponseEntity<ApiResponse<PageResponse<UserDto.SimpleResponse>>> getFollowers(
            @PathVariable Long userId,
            @PageableDefault(size = 20) Pageable pageable) {
        PageResponse<UserDto.SimpleResponse> response = userService.getFollowers(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "팔로우 여부 확인")
    @GetMapping("/{userId}/is-following")
    public ResponseEntity<ApiResponse<Boolean>> isFollowing(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable Long userId) {
        boolean isFollowing = userService.isFollowing(userDetails.getUserId(), userId);
        return ResponseEntity.ok(ApiResponse.success(isFollowing));
    }

    @Operation(summary = "점수 획득 내역 조회")
    @GetMapping("/{userId}/score-history")
    public ResponseEntity<ApiResponse<PageResponse<ScoreEventDto.Response>>> getScoreHistory(
            @PathVariable Long userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<ScoreEventDto.Response> response = userService.getScoreHistory(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "친구 추천")
    @GetMapping("/recommendations")
    public ResponseEntity<ApiResponse<List<UserDto.RecommendResponse>>> getRecommendedFriends(
            @CurrentUser CustomUserDetails userDetails,
            @RequestParam(defaultValue = "10") int limit) {
        List<UserDto.RecommendResponse> response = userService.getRecommendedFriends(userDetails.getUserId(), limit);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "사용자 영향력 통계 조회")
    @GetMapping("/{userId}/influence")
    public ResponseEntity<ApiResponse<ReviewDto.InfluenceStats>> getInfluenceStats(@PathVariable Long userId) {
        ReviewDto.InfluenceStats response = reviewService.getInfluenceStats(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "사용자 차단")
    @PostMapping("/{userId}/block")
    public ResponseEntity<ApiResponse<Void>> blockUser(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable Long userId) {
        userService.blockUser(userDetails.getUserId(), userId);
        return ResponseEntity.ok(ApiResponse.success(null, "사용자를 차단했습니다"));
    }

    @Operation(summary = "사용자 차단 해제")
    @DeleteMapping("/{userId}/block")
    public ResponseEntity<ApiResponse<Void>> unblockUser(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable Long userId) {
        userService.unblockUser(userDetails.getUserId(), userId);
        return ResponseEntity.ok(ApiResponse.success(null, "차단을 해제했습니다"));
    }

    @Operation(summary = "차단 목록 조회")
    @GetMapping("/blocked")
    public ResponseEntity<ApiResponse<PageResponse<UserDto.BlockedUserResponse>>> getBlockedUsers(
            @CurrentUser CustomUserDetails userDetails,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<UserDto.BlockedUserResponse> response = userService.getBlockedUsers(userDetails.getUserId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "차단 여부 확인")
    @GetMapping("/{userId}/is-blocked")
    public ResponseEntity<ApiResponse<Boolean>> isBlocked(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable Long userId) {
        boolean isBlocked = userService.isBlocked(userDetails.getUserId(), userId);
        return ResponseEntity.ok(ApiResponse.success(isBlocked));
    }

    @Operation(summary = "사용자 검색")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<UserDto.SearchResponse>>> searchUsers(
            @RequestParam String query,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 20, sort = "tasteScore", direction = Sort.Direction.DESC) Pageable pageable) {
        Long currentUserId = userDetails != null ? userDetails.getUserId() : null;
        PageResponse<UserDto.SearchResponse> response = userService.searchUsers(query, currentUserId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "알림 설정 조회")
    @GetMapping("/me/notifications")
    public ResponseEntity<ApiResponse<UserDto.NotificationSettingsResponse>> getNotificationSettings(
            @CurrentUser CustomUserDetails userDetails) {
        UserDto.NotificationSettingsResponse response = userService.getNotificationSettings(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "알림 설정 수정")
    @PutMapping("/me/notifications")
    public ResponseEntity<ApiResponse<UserDto.NotificationSettingsResponse>> updateNotificationSettings(
            @CurrentUser CustomUserDetails userDetails,
            @Valid @RequestBody UserDto.NotificationSettingsRequest request) {
        UserDto.NotificationSettingsResponse response = userService.updateNotificationSettings(userDetails.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
