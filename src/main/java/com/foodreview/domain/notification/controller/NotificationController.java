package com.foodreview.domain.notification.controller;

import com.foodreview.domain.notification.dto.NotificationDto;
import com.foodreview.domain.notification.service.NotificationService;
import com.foodreview.global.common.ApiResponse;
import com.foodreview.global.common.PageResponse;
import com.foodreview.global.security.CurrentUser;
import com.foodreview.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Notification", description = "알림 API")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "알림 목록 조회")
    @GetMapping
    public ApiResponse<PageResponse<NotificationDto.Response>> getNotifications(
            @CurrentUser CustomUserDetails userDetails,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(notificationService.getNotifications(userDetails.getUserId(), pageable));
    }

    @Operation(summary = "읽지 않은 알림 수 조회")
    @GetMapping("/unread-count")
    public ApiResponse<NotificationDto.UnreadCountResponse> getUnreadCount(
            @CurrentUser CustomUserDetails userDetails) {
        long count = notificationService.getUnreadCount(userDetails.getUserId());
        return ApiResponse.success(NotificationDto.UnreadCountResponse.builder().count(count).build());
    }

    @Operation(summary = "알림 읽음 처리")
    @PostMapping("/{notificationId}/read")
    public ApiResponse<Void> markAsRead(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable Long notificationId) {
        notificationService.markAsRead(notificationId, userDetails.getUserId());
        return ApiResponse.success(null);
    }

    @Operation(summary = "모든 알림 읽음 처리")
    @PostMapping("/read-all")
    public ApiResponse<Void> markAllAsRead(@CurrentUser CustomUserDetails userDetails) {
        notificationService.markAllAsRead(userDetails.getUserId());
        return ApiResponse.success(null);
    }
}
