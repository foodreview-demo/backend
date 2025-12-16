package com.foodreview.domain.auth.controller;

import com.foodreview.domain.auth.dto.AuthDto;
import com.foodreview.domain.auth.dto.KakaoOAuthDto;
import com.foodreview.domain.auth.service.AuthService;
import com.foodreview.domain.auth.service.KakaoOAuthService;
import com.foodreview.domain.user.dto.UserDto;
import com.foodreview.global.common.ApiResponse;
import com.foodreview.global.security.CurrentUser;
import com.foodreview.global.security.CustomUserDetails;
import com.foodreview.global.exception.CustomException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final KakaoOAuthService kakaoOAuthService;

    @Operation(summary = "회원가입")
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<UserDto.Response>> signUp(@Valid @RequestBody AuthDto.SignUpRequest request) {
        UserDto.Response response = authService.signUp(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response, "회원가입이 완료되었습니다"));
    }

    @Operation(summary = "로그인")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthDto.TokenResponse>> login(
            @Valid @RequestBody AuthDto.LoginRequest request,
            @RequestHeader(value = "X-Device-Id", required = false) String deviceId,
            HttpServletRequest httpRequest) {

        String finalDeviceId = getOrCreateDeviceId(deviceId);
        String userAgent = httpRequest.getHeader("User-Agent");
        String ipAddress = getClientIpAddress(httpRequest);

        AuthDto.TokenResponse response = authService.login(request, finalDeviceId, userAgent, ipAddress);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "토큰 갱신")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthDto.TokenResponse>> refresh(
            @Valid @RequestBody AuthDto.RefreshRequest request,
            @RequestHeader(value = "X-Device-Id", required = false) String deviceId,
            HttpServletRequest httpRequest) {

        String finalDeviceId = getOrCreateDeviceId(deviceId);
        String userAgent = httpRequest.getHeader("User-Agent");
        String ipAddress = getClientIpAddress(httpRequest);

        AuthDto.TokenResponse response = authService.refresh(request, finalDeviceId, userAgent, ipAddress);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "카카오 로그인")
    @PostMapping("/oauth/kakao")
    public ResponseEntity<ApiResponse<AuthDto.TokenResponse>> kakaoLogin(
            @RequestBody KakaoOAuthDto.TokenRequest request,
            @RequestHeader(value = "X-Device-Id", required = false) String deviceId,
            HttpServletRequest httpRequest) {

        String finalDeviceId = getOrCreateDeviceId(deviceId);
        String userAgent = httpRequest.getHeader("User-Agent");
        String ipAddress = getClientIpAddress(httpRequest);

        AuthDto.TokenResponse response = kakaoOAuthService.loginWithKakao(request.getCode(), finalDeviceId, userAgent, ipAddress);
        return ResponseEntity.ok(ApiResponse.success(response, "카카오 로그인 성공"));
    }

    @Operation(summary = "로그아웃")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CurrentUser CustomUserDetails userDetails,
            @RequestBody(required = false) AuthDto.LogoutRequest request) {

        if (userDetails == null) {
            throw new CustomException("인증되지 않은 사용자입니다", HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
        }

        String refreshToken = request != null ? request.getRefreshToken() : null;
        boolean allDevices = request != null && Boolean.TRUE.equals(request.getAllDevices());

        authService.logout(userDetails.getUsername(), refreshToken, allDevices);
        return ResponseEntity.ok(ApiResponse.success(null, "로그아웃 되었습니다"));
    }

    /**
     * Device ID 획득 또는 생성
     */
    private String getOrCreateDeviceId(String deviceId) {
        if (deviceId != null && !deviceId.isBlank()) {
            return deviceId;
        }
        // Device ID가 없으면 새로 생성 (클라이언트에서 저장해야 함)
        return UUID.randomUUID().toString();
    }

    /**
     * 클라이언트 IP 주소 획득 (프록시 고려)
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {
                "X-Forwarded-For",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_X_FORWARDED_FOR",
                "HTTP_X_FORWARDED",
                "HTTP_X_CLUSTER_CLIENT_IP",
                "HTTP_CLIENT_IP",
                "HTTP_FORWARDED_FOR",
                "HTTP_FORWARDED",
                "HTTP_VIA",
                "REMOTE_ADDR"
        };

        for (String headerName : headerNames) {
            String ip = request.getHeader(headerName);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For의 경우 첫 번째 IP가 실제 클라이언트
                return ip.split(",")[0].trim();
            }
        }

        return request.getRemoteAddr();
    }
}
