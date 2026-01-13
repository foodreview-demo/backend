package com.foodreview.domain.notification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class FcmTokenDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RegisterRequest {
        @NotBlank(message = "FCM 토큰은 필수입니다")
        private String token;

        private String deviceType; // "ANDROID", "IOS", "WEB"

        private String deviceId;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UnregisterRequest {
        @NotBlank(message = "FCM 토큰은 필수입니다")
        private String token;
    }
}
