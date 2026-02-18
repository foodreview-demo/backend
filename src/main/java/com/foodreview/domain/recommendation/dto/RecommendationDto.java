package com.foodreview.domain.recommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class RecommendationDto {

    /**
     * AI 추천 요청
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AskRequest {
        private String query;  // 사용자 질문 (예: "오늘 점심 뭐 먹지?", "매운 거 먹고 싶어")
        private Double latitude;
        private Double longitude;
    }

    /**
     * 오늘의 추천 요청
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TodayRequest {
        private Double latitude;
        private Double longitude;
        private String timeSlot;  // morning, lunch, dinner, lateNight (선택적)
    }

    /**
     * AI 추천 응답
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private String summary;  // 추천 요약 메시지
        private List<RestaurantRecommendation> restaurants;
        private ContextInfo context;
        private Long cacheId;  // 피드백용 캐시 ID
    }

    /**
     * 음식점 추천 정보
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RestaurantRecommendation {
        private Long id;
        private String kakaoPlaceId;
        private String name;
        private String category;
        private String categoryDisplay;
        private String address;
        private String reason;  // 추천 이유
        private String recommendedMenu;  // 추천 메뉴
        private Double rating;  // 앱 내 평균 평점
        private Integer reviewCount;  // 앱 내 리뷰 수
        private Double distance;  // 현재 위치에서 거리 (km)
        private Double latitude;
        private Double longitude;
    }

    /**
     * 컨텍스트 정보
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContextInfo {
        private String timeSlot;  // 현재 시간대
        private String dayOfWeek;  // 요일
        private String weather;  // 날씨 (향후 연동)
        private Integer temperature;  // 온도 (향후 연동)
        private LocalDateTime generatedAt;  // 생성 시간
    }

    /**
     * 추천 히스토리
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HistoryItem {
        private Long id;
        private String type;  // today, ask
        private String query;  // ask일 경우 사용자 질문
        private String summary;
        private List<RestaurantRecommendation> restaurants;
        private LocalDate date;
        private Integer feedback;  // 1: 좋아요, -1: 별로예요, null: 미응답
    }

    /**
     * 피드백 요청
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeedbackRequest {
        private Long cacheId;
        private Integer feedback;  // 1: 좋아요, -1: 별로예요
    }
}
