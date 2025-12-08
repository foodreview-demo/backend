package com.foodreview.domain.auth.controller;

import com.foodreview.domain.auth.dto.AuthDto;
import com.foodreview.domain.auth.service.AuthService;
import com.foodreview.domain.user.dto.UserDto;
import com.foodreview.domain.user.service.UserService;
import com.foodreview.global.common.ApiResponse;
import com.foodreview.global.security.CurrentUser;
import com.foodreview.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @Operation(summary = "회원가입")
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<UserDto.Response>> signUp(@Valid @RequestBody AuthDto.SignUpRequest request) {
        UserDto.Response response = authService.signUp(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response, "회원가입이 완료되었습니다"));
    }

    @Operation(summary = "로그인")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthDto.TokenResponse>> login(@Valid @RequestBody AuthDto.LoginRequest request) {
        AuthDto.TokenResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "토큰 갱신")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthDto.TokenResponse>> refresh(@Valid @RequestBody AuthDto.RefreshRequest request) {
        AuthDto.TokenResponse response = authService.refresh(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "내 정보 조회")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto.Response>> getMe(@CurrentUser CustomUserDetails userDetails) {
        UserDto.Response response = userService.getMyProfile(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
