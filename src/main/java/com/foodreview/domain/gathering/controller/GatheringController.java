package com.foodreview.domain.gathering.controller;

import com.foodreview.domain.gathering.dto.GatheringDto;
import com.foodreview.domain.gathering.entity.GatheringStatus;
import com.foodreview.domain.gathering.service.GatheringService;
import com.foodreview.domain.user.entity.User;
import com.foodreview.domain.user.repository.UserRepository;
import com.foodreview.global.common.ApiResponse;
import com.foodreview.global.exception.CustomException;
import com.foodreview.global.security.CurrentUser;
import com.foodreview.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/gatherings")
@RequiredArgsConstructor
public class GatheringController {

    private final GatheringService gatheringService;
    private final UserRepository userRepository;

    /**
     * 모임 생성
     */
    @PostMapping
    public ResponseEntity<ApiResponse<GatheringDto.Response>> createGathering(
            @RequestBody GatheringDto.CreateRequest request,
            @CurrentUser CustomUserDetails userDetails) {
        User user = getUser(userDetails.getUserId());
        GatheringDto.Response response = gatheringService.createGathering(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    /**
     * 모임 상세 조회
     */
    @GetMapping("/{uuid}")
    public ResponseEntity<ApiResponse<GatheringDto.DetailResponse>> getGathering(
            @PathVariable String uuid,
            @CurrentUser CustomUserDetails userDetails) {
        Long currentUserId = userDetails != null ? userDetails.getUserId() : null;
        GatheringDto.DetailResponse response = gatheringService.getGathering(uuid, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 음식점별 모임 조회
     */
    @GetMapping("/restaurant/{restaurantId}")
    public ResponseEntity<ApiResponse<List<GatheringDto.Response>>> getGatheringsByRestaurant(
            @PathVariable Long restaurantId,
            @CurrentUser CustomUserDetails userDetails) {
        Long currentUserId = userDetails != null ? userDetails.getUserId() : null;
        List<GatheringDto.Response> responses = gatheringService.getGatheringsByRestaurant(restaurantId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * 지역별 모임 조회
     */
    @GetMapping("/region")
    public ResponseEntity<ApiResponse<Page<GatheringDto.Response>>> getGatheringsByRegion(
            @RequestParam String region,
            @RequestParam(required = false) String district,
            @CurrentUser CustomUserDetails userDetails,
            @PageableDefault(size = 20) Pageable pageable) {
        Long currentUserId = userDetails != null ? userDetails.getUserId() : null;
        Page<GatheringDto.Response> responses = gatheringService.getGatheringsByRegion(region, district, currentUserId, pageable);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * 내가 생성한 모임 조회
     */
    @GetMapping("/my/created")
    public ResponseEntity<ApiResponse<Page<GatheringDto.Response>>> getMyCreatedGatherings(
            @CurrentUser CustomUserDetails userDetails,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<GatheringDto.Response> responses = gatheringService.getMyCreatedGatherings(userDetails.getUserId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * 내가 참여한 모임 조회
     */
    @GetMapping("/my/joined")
    public ResponseEntity<ApiResponse<Page<GatheringDto.Response>>> getMyJoinedGatherings(
            @CurrentUser CustomUserDetails userDetails,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<GatheringDto.Response> responses = gatheringService.getMyJoinedGatherings(userDetails.getUserId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * 모임 참여 (결제 전 예약)
     */
    @PostMapping("/{uuid}/join")
    public ResponseEntity<ApiResponse<GatheringDto.ParticipantInfo>> joinGathering(
            @PathVariable String uuid,
            @CurrentUser CustomUserDetails userDetails) {
        User user = getUser(userDetails.getUserId());
        GatheringDto.ParticipantInfo response = gatheringService.joinGathering(uuid, user);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 결제 검증 및 참여 확정
     */
    @PostMapping("/{uuid}/deposit/verify")
    public ResponseEntity<ApiResponse<GatheringDto.ParticipantInfo>> verifyDeposit(
            @PathVariable String uuid,
            @RequestBody GatheringDto.JoinRequest request,
            @CurrentUser CustomUserDetails userDetails) {
        User user = getUser(userDetails.getUserId());
        GatheringDto.ParticipantInfo response = gatheringService.verifyDeposit(uuid, request, user);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 환금 처리 (호스트용)
     */
    @PostMapping("/{uuid}/deposit/refund")
    public ResponseEntity<ApiResponse<Void>> refundParticipant(
            @PathVariable String uuid,
            @RequestBody GatheringDto.RefundRequest request,
            @CurrentUser CustomUserDetails userDetails) {
        User user = getUser(userDetails.getUserId());
        gatheringService.refundParticipant(uuid, request.getParticipantId(), user);
        return ResponseEntity.ok(ApiResponse.success(null, "환금 처리가 완료되었습니다"));
    }

    /**
     * 모임 완료 처리 (호스트용)
     */
    @PostMapping("/{uuid}/complete")
    public ResponseEntity<ApiResponse<Void>> completeGathering(
            @PathVariable String uuid,
            @CurrentUser CustomUserDetails userDetails) {
        User user = getUser(userDetails.getUserId());
        gatheringService.completeGathering(uuid, user);
        return ResponseEntity.ok(ApiResponse.success(null, "모임이 완료되었습니다"));
    }

    /**
     * 모임 취소 처리 (호스트용)
     */
    @PostMapping("/{uuid}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelGathering(
            @PathVariable String uuid,
            @CurrentUser CustomUserDetails userDetails) {
        User user = getUser(userDetails.getUserId());
        gatheringService.cancelGathering(uuid, user);
        return ResponseEntity.ok(ApiResponse.success(null, "모임이 취소되었습니다"));
    }

    /**
     * 모임 상태 변경 (호스트용)
     */
    @PutMapping("/{uuid}/status")
    public ResponseEntity<ApiResponse<GatheringDto.Response>> updateStatus(
            @PathVariable String uuid,
            @RequestBody GatheringDto.StatusUpdateRequest request,
            @CurrentUser CustomUserDetails userDetails) {
        User user = getUser(userDetails.getUserId());
        GatheringDto.Response response = gatheringService.updateStatus(uuid, request.getStatus(), user);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException("사용자를 찾을 수 없습니다", HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));
    }
}
