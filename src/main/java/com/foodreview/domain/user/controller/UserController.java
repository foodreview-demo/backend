package com.foodreview.domain.user.controller;

import com.foodreview.domain.user.dto.ScoreEventDto;
import com.foodreview.domain.user.dto.UserDto;
import com.foodreview.domain.user.service.UserService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "User", description = "사용자 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "사용자 정보 조회")
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserDto.Response>> getUser(@PathVariable Long userId) {
        UserDto.Response response = userService.getUser(userId);
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
}
