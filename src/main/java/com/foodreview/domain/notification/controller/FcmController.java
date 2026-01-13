package com.foodreview.domain.notification.controller;

import com.foodreview.domain.notification.dto.FcmTokenDto;
import com.foodreview.domain.notification.service.FcmService;
import com.foodreview.global.common.ApiResponse;
import com.foodreview.global.security.CurrentUser;
import com.foodreview.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "FCM", description = "FCM 푸시 알림 API")
@RestController
@RequestMapping("/api/fcm")
@RequiredArgsConstructor
public class FcmController {

    private final FcmService fcmService;

    @Operation(summary = "FCM 토큰 등록")
    @PostMapping("/token")
    public ResponseEntity<ApiResponse<Void>> registerToken(
            @CurrentUser CustomUserDetails userDetails,
            @Valid @RequestBody FcmTokenDto.RegisterRequest request) {
        fcmService.registerToken(userDetails.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(null, "FCM 토큰이 등록되었습니다"));
    }

    @Operation(summary = "FCM 토큰 해제")
    @DeleteMapping("/token")
    public ResponseEntity<ApiResponse<Void>> unregisterToken(
            @Valid @RequestBody FcmTokenDto.UnregisterRequest request) {
        fcmService.unregisterToken(request.getToken());
        return ResponseEntity.ok(ApiResponse.success(null, "FCM 토큰이 해제되었습니다"));
    }
}
