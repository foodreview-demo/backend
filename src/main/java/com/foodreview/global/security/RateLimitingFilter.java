package com.foodreview.global.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate Limiting 필터 - 브루트 포스 공격 방지
 *
 * 로그인/회원가입 등 민감한 엔드포인트에 대해 IP 기반 요청 제한
 */
@Slf4j
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    // IP별 버킷 저장소
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    // 로그인 엔드포인트: 분당 10회 제한
    private static final int LOGIN_REQUESTS_PER_MINUTE = 10;

    // 회원가입 엔드포인트: 분당 5회 제한
    private static final int SIGNUP_REQUESTS_PER_MINUTE = 5;

    // 일반 API: 분당 100회 제한
    private static final int API_REQUESTS_PER_MINUTE = 100;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        // Rate Limiting 적용 대상 확인
        if (!shouldApplyRateLimit(path, method)) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(request);
        String bucketKey = clientIp + ":" + getRateLimitCategory(path);

        Bucket bucket = buckets.computeIfAbsent(bucketKey,
                k -> createBucket(path));

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for IP: {}, path: {}", clientIp, path);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    "{\"success\":false,\"message\":\"요청이 너무 많습니다. 잠시 후 다시 시도해주세요.\",\"errorCode\":\"TOO_MANY_REQUESTS\"}"
            );
        }
    }

    private boolean shouldApplyRateLimit(String path, String method) {
        // POST 요청만 제한 (로그인, 회원가입 등)
        if (!"POST".equals(method)) {
            return false;
        }

        return path.startsWith("/api/auth/") ||
               path.startsWith("/api/reviews") ||
               path.startsWith("/api/comments");
    }

    private String getRateLimitCategory(String path) {
        if (path.contains("/auth/login") || path.contains("/auth/kakao")) {
            return "login";
        } else if (path.contains("/auth/signup")) {
            return "signup";
        }
        return "api";
    }

    private Bucket createBucket(String path) {
        int requestsPerMinute;

        if (path.contains("/auth/login") || path.contains("/auth/kakao")) {
            requestsPerMinute = LOGIN_REQUESTS_PER_MINUTE;
        } else if (path.contains("/auth/signup")) {
            requestsPerMinute = SIGNUP_REQUESTS_PER_MINUTE;
        } else {
            requestsPerMinute = API_REQUESTS_PER_MINUTE;
        }

        Bandwidth limit = Bandwidth.classic(requestsPerMinute,
                Refill.greedy(requestsPerMinute, Duration.ofMinutes(1)));

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    private String getClientIp(HttpServletRequest request) {
        // 프록시/로드밸런서 뒤에 있는 경우 X-Forwarded-For 헤더 확인
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // 첫 번째 IP가 실제 클라이언트 IP
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}
