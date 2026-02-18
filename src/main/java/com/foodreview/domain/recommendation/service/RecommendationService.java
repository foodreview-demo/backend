package com.foodreview.domain.recommendation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodreview.domain.recommendation.dto.RecommendationDto;
import com.foodreview.domain.recommendation.dto.RecommendationDto.*;
import com.foodreview.domain.recommendation.entity.AiRecommendationCache;
import com.foodreview.domain.recommendation.entity.UserTasteProfile;
import com.foodreview.domain.recommendation.repository.AiRecommendationCacheRepository;
import com.foodreview.domain.recommendation.repository.UserTasteProfileRepository;
import com.foodreview.domain.restaurant.entity.Restaurant;
import com.foodreview.domain.restaurant.repository.RestaurantRepository;
import com.foodreview.domain.user.entity.User;
import com.foodreview.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final UserTasteProfileRepository tasteProfileRepository;
    private final AiRecommendationCacheRepository cacheRepository;
    private final RestaurantRepository restaurantRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final WebClient.Builder webClientBuilder;

    @Value("${anthropic.api-key:}")
    private String anthropicApiKey;

    private static final String CLAUDE_API_URL = "https://api.anthropic.com/v1/messages";
    private static final String CLAUDE_MODEL = "claude-3-haiku-20240307";
    private static final int MAX_TOKENS = 500;

    /**
     * 오늘의 추천 조회 (캐시된 결과 우선)
     */
    @Transactional
    public Response getTodayRecommendation(Long userId, TodayRequest request) {
        LocalDate today = LocalDate.now();

        // 캐시 확인
        Optional<AiRecommendationCache> cached = cacheRepository
                .findByUserIdAndRecommendationDateAndRecommendationType(userId, today, "today");

        if (cached.isPresent()) {
            return parseCachedResponse(cached.get());
        }

        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        // 새로 생성
        return generateRecommendation(user, null, request.getLatitude(), request.getLongitude(), "today");
    }

    /**
     * AI에게 질문하기
     */
    @Transactional
    public Response askRecommendation(Long userId, AskRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
        return generateRecommendation(user, request.getQuery(), request.getLatitude(), request.getLongitude(), "ask");
    }

    /**
     * 추천 히스토리 조회
     */
    @Transactional(readOnly = true)
    public List<HistoryItem> getRecommendationHistory(Long userId) {
        List<AiRecommendationCache> caches = cacheRepository.findByUserIdOrderByCreatedAtDesc(userId);

        return caches.stream()
                .limit(20)
                .map(this::toHistoryItem)
                .collect(Collectors.toList());
    }

    /**
     * 피드백 저장
     */
    @Transactional
    public void saveFeedback(Long userId, FeedbackRequest request) {
        cacheRepository.findById(request.getCacheId())
                .filter(cache -> cache.getUser().getId().equals(userId))
                .ifPresent(cache -> cache.updateFeedback(request.getFeedback()));
    }

    /**
     * AI 추천 생성
     */
    private Response generateRecommendation(User user, String query, Double latitude, Double longitude, String type) {
        // 1. 사용자 취향 프로필 조회
        UserTasteProfile profile = tasteProfileRepository.findByUserId(user.getId()).orElse(null);

        // 2. 컨텍스트 정보 생성
        ContextInfo context = buildContextInfo();

        // 3. 후보 음식점 조회 (규칙 기반 필터링)
        List<Restaurant> candidates = getCandidateRestaurants(user, profile, context, latitude, longitude);

        if (candidates.isEmpty()) {
            return buildEmptyResponse(context);
        }

        // 4. Claude API 호출
        String aiResponse;
        int tokensUsed = 0;

        if (anthropicApiKey != null && !anthropicApiKey.isEmpty()) {
            try {
                ClaudeResponse claudeResponse = callClaudeApi(profile, context, candidates, query);
                aiResponse = claudeResponse.content;
                tokensUsed = claudeResponse.tokensUsed;
            } catch (Exception e) {
                log.error("Claude API 호출 실패, 규칙 기반으로 대체", e);
                aiResponse = generateRuleBasedResponse(profile, context, candidates, query);
            }
        } else {
            // API 키 없으면 규칙 기반 추천
            aiResponse = generateRuleBasedResponse(profile, context, candidates, query);
        }

        // 5. 응답 파싱 및 음식점 매칭
        Response response = parseAiResponse(aiResponse, candidates, context);

        // 6. 캐시 저장
        AiRecommendationCache cache = saveToCache(user, type, query, response, context, tokensUsed);
        response.setCacheId(cache.getId());

        return response;
    }

    /**
     * 컨텍스트 정보 생성
     */
    private ContextInfo buildContextInfo() {
        LocalDateTime now = LocalDateTime.now();
        int hour = now.getHour();
        DayOfWeek dayOfWeek = now.getDayOfWeek();

        String timeSlot;
        if (hour >= 6 && hour <= 10) timeSlot = "morning";
        else if (hour >= 11 && hour <= 14) timeSlot = "lunch";
        else if (hour >= 17 && hour <= 21) timeSlot = "dinner";
        else if (hour >= 22 || hour < 6) timeSlot = "lateNight";
        else timeSlot = "afternoon";

        return ContextInfo.builder()
                .timeSlot(timeSlot)
                .dayOfWeek(dayOfWeek.name())
                .generatedAt(now)
                .build();
    }

    /**
     * 후보 음식점 조회 (규칙 기반 필터링)
     */
    private List<Restaurant> getCandidateRestaurants(User user, UserTasteProfile profile,
                                                      ContextInfo context, Double latitude, Double longitude) {
        // 사용자 지역 기반 음식점 조회
        String region = user.getRegion();
        String district = user.getDistrict();

        List<Restaurant> restaurants;
        if (district != null && !district.isEmpty()) {
            restaurants = restaurantRepository.findByRegionAndDistrictWithReviews(region, district);
        } else {
            restaurants = restaurantRepository.findByRegionWithReviews(region);
        }

        // 선호 카테고리 기반 정렬
        if (profile != null && profile.getPreferredCategories() != null) {
            try {
                List<String> preferred = objectMapper.readValue(profile.getPreferredCategories(), List.class);
                restaurants.sort((r1, r2) -> {
                    int score1 = preferred.indexOf(r1.getCategory().name());
                    int score2 = preferred.indexOf(r2.getCategory().name());
                    if (score1 == -1) score1 = 100;
                    if (score2 == -1) score2 = 100;
                    return score1 - score2;
                });
            } catch (JsonProcessingException e) {
                log.warn("선호 카테고리 파싱 실패", e);
            }
        }

        // 상위 20개만 반환 (토큰 절약)
        return restaurants.stream().limit(20).collect(Collectors.toList());
    }

    /**
     * Claude API 호출
     */
    private ClaudeResponse callClaudeApi(UserTasteProfile profile, ContextInfo context,
                                          List<Restaurant> candidates, String query) {
        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPrompt(profile, context, candidates, query);

        String requestBody = String.format("""
            {
                "model": "%s",
                "max_tokens": %d,
                "system": "%s",
                "messages": [
                    {"role": "user", "content": "%s"}
                ]
            }
            """,
                CLAUDE_MODEL,
                MAX_TOKENS,
                escapeJson(systemPrompt),
                escapeJson(userPrompt)
        );

        WebClient client = webClientBuilder.build();

        String response = client.post()
                .uri(CLAUDE_API_URL)
                .header("x-api-key", anthropicApiKey)
                .header("anthropic-version", "2023-06-01")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        return parseClaudeResponse(response);
    }

    /**
     * 시스템 프롬프트 생성
     */
    private String buildSystemPrompt() {
        return """
            당신은 한국 음식 추천 전문가입니다. 사용자의 취향과 상황에 맞는 맛집을 추천해주세요.

            응답 형식 (반드시 JSON으로):
            {
                "summary": "추천 요약 (20자 이내)",
                "recommendations": [
                    {
                        "restaurantIndex": 0,
                        "reason": "추천 이유 (30자 이내)",
                        "recommendedMenu": "추천 메뉴"
                    }
                ]
            }

            규칙:
            - 최대 3개 음식점 추천
            - restaurantIndex는 제공된 후보 목록의 인덱스 (0부터 시작)
            - 추천 이유는 사용자 취향과 연결해서 설명
            - 시간대에 맞는 음식 추천 (아침: 가벼운 것, 저녁: 다양, 야식: 간단한 것)
            """;
    }

    /**
     * 사용자 프롬프트 생성
     */
    private String buildUserPrompt(UserTasteProfile profile, ContextInfo context,
                                    List<Restaurant> candidates, String query) {
        StringBuilder sb = new StringBuilder();

        // 사용자 취향 요약
        if (profile != null) {
            sb.append("사용자 취향:\n");
            sb.append("- 선호: ").append(profile.getPreferredCategories()).append("\n");
            sb.append("- 평균 평점: ").append(String.format("%.1f", profile.getAvgRating())).append("\n");
            if (profile.getTimeSlotPreferences() != null) {
                sb.append("- 시간대 선호: ").append(profile.getTimeSlotPreferences()).append("\n");
            }
            if (profile.getSequencePatterns() != null) {
                sb.append("- 식사 패턴: ").append(profile.getSequencePatterns()).append("\n");
            }
        } else {
            sb.append("신규 사용자 (취향 정보 없음)\n");
        }

        // 현재 상황
        sb.append("\n현재 상황:\n");
        sb.append("- 시간대: ").append(translateTimeSlot(context.getTimeSlot())).append("\n");
        sb.append("- 요일: ").append(translateDayOfWeek(context.getDayOfWeek())).append("\n");

        // 사용자 질문
        if (query != null && !query.isEmpty()) {
            sb.append("\n사용자 요청: ").append(query).append("\n");
        }

        // 후보 음식점 목록
        sb.append("\n후보 음식점:\n");
        for (int i = 0; i < candidates.size(); i++) {
            Restaurant r = candidates.get(i);
            sb.append(String.format("[%d] %s (%s) - 평점 %.1f, 리뷰 %d개\n",
                    i, r.getName(), r.getCategory().getDisplayName(),
                    r.getAverageRating() != null ? r.getAverageRating() : 0.0,
                    r.getReviewCount()));
        }

        return sb.toString();
    }

    /**
     * 규칙 기반 추천 생성 (AI 없이)
     */
    private String generateRuleBasedResponse(UserTasteProfile profile, ContextInfo context,
                                              List<Restaurant> candidates, String query) {
        List<Integer> selectedIndexes = new ArrayList<>();

        // 상위 3개 선택
        for (int i = 0; i < Math.min(3, candidates.size()); i++) {
            selectedIndexes.add(i);
        }

        StringBuilder json = new StringBuilder();
        json.append("{\"summary\": \"오늘의 추천 맛집\", \"recommendations\": [");

        for (int i = 0; i < selectedIndexes.size(); i++) {
            if (i > 0) json.append(",");
            int idx = selectedIndexes.get(i);
            Restaurant r = candidates.get(idx);
            json.append(String.format(
                    "{\"restaurantIndex\": %d, \"reason\": \"%s 맛집 추천\", \"recommendedMenu\": \"대표 메뉴\"}",
                    idx, r.getCategory().getDisplayName()
            ));
        }

        json.append("]}");
        return json.toString();
    }

    /**
     * Claude 응답 파싱
     */
    private ClaudeResponse parseClaudeResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            String content = root.path("content").get(0).path("text").asText();
            int inputTokens = root.path("usage").path("input_tokens").asInt();
            int outputTokens = root.path("usage").path("output_tokens").asInt();

            return new ClaudeResponse(content, inputTokens + outputTokens);
        } catch (Exception e) {
            log.error("Claude 응답 파싱 실패", e);
            return new ClaudeResponse("{\"summary\": \"추천 생성 실패\", \"recommendations\": []}", 0);
        }
    }

    /**
     * AI 응답을 Response 객체로 변환
     */
    private Response parseAiResponse(String aiResponse, List<Restaurant> candidates, ContextInfo context) {
        try {
            // JSON 부분만 추출 (마크다운 코드블록 제거)
            String jsonContent = aiResponse;
            if (aiResponse.contains("```json")) {
                jsonContent = aiResponse.substring(aiResponse.indexOf("```json") + 7);
                jsonContent = jsonContent.substring(0, jsonContent.indexOf("```"));
            } else if (aiResponse.contains("```")) {
                jsonContent = aiResponse.substring(aiResponse.indexOf("```") + 3);
                jsonContent = jsonContent.substring(0, jsonContent.indexOf("```"));
            }
            jsonContent = jsonContent.trim();

            JsonNode root = objectMapper.readTree(jsonContent);
            String summary = root.path("summary").asText("오늘의 추천");

            List<RestaurantRecommendation> recommendations = new ArrayList<>();
            JsonNode recsNode = root.path("recommendations");

            for (JsonNode recNode : recsNode) {
                int idx = recNode.path("restaurantIndex").asInt(-1);
                if (idx >= 0 && idx < candidates.size()) {
                    Restaurant r = candidates.get(idx);
                    recommendations.add(RestaurantRecommendation.builder()
                            .id(r.getId())
                            .kakaoPlaceId(r.getKakaoPlaceId())
                            .name(r.getName())
                            .category(r.getCategory().name())
                            .categoryDisplay(r.getCategory().getDisplayName())
                            .address(r.getAddress())
                            .reason(recNode.path("reason").asText("추천 맛집"))
                            .recommendedMenu(recNode.path("recommendedMenu").asText())
                            .rating(r.getAverageRating() != null ? r.getAverageRating().doubleValue() : null)
                            .reviewCount(r.getReviewCount())
                            .latitude(r.getLatitude())
                            .longitude(r.getLongitude())
                            .build());
                }
            }

            return Response.builder()
                    .summary(summary)
                    .restaurants(recommendations)
                    .context(context)
                    .build();

        } catch (Exception e) {
            log.error("AI 응답 파싱 실패: {}", aiResponse, e);
            return buildEmptyResponse(context);
        }
    }

    /**
     * 빈 응답 생성
     */
    private Response buildEmptyResponse(ContextInfo context) {
        return Response.builder()
                .summary("주변에 추천할 음식점이 없어요")
                .restaurants(new ArrayList<>())
                .context(context)
                .build();
    }

    /**
     * 캐시 저장
     */
    private AiRecommendationCache saveToCache(User user, String type, String query,
                                            Response response, ContextInfo context, int tokensUsed) {
        try {
            String resultJson = objectMapper.writeValueAsString(response);
            String contextJson = objectMapper.writeValueAsString(context);

            AiRecommendationCache cache = AiRecommendationCache.builder()
                    .user(user)
                    .recommendationDate(LocalDate.now())
                    .recommendationType(type)
                    .userQuery(query)
                    .recommendationResult(resultJson)
                    .contextInfo(contextJson)
                    .tokensUsed(tokensUsed)
                    .build();

            return cacheRepository.save(cache);
        } catch (JsonProcessingException e) {
            log.error("캐시 저장 실패", e);
            throw new RuntimeException("캐시 저장 실패", e);
        }
    }

    /**
     * 캐시된 응답 파싱
     */
    private Response parseCachedResponse(AiRecommendationCache cache) {
        try {
            Response response = objectMapper.readValue(cache.getRecommendationResult(), Response.class);
            response.setCacheId(cache.getId());
            return response;
        } catch (JsonProcessingException e) {
            log.error("캐시 파싱 실패", e);
            return buildEmptyResponse(null);
        }
    }

    /**
     * 히스토리 아이템 변환
     */
    private HistoryItem toHistoryItem(AiRecommendationCache cache) {
        try {
            Response response = objectMapper.readValue(cache.getRecommendationResult(), Response.class);
            return HistoryItem.builder()
                    .id(cache.getId())
                    .type(cache.getRecommendationType())
                    .query(cache.getUserQuery())
                    .summary(response.getSummary())
                    .restaurants(response.getRestaurants())
                    .date(cache.getRecommendationDate())
                    .feedback(cache.getUserFeedback())
                    .build();
        } catch (JsonProcessingException e) {
            log.error("히스토리 아이템 변환 실패", e);
            return null;
        }
    }

    /**
     * 시간대 번역
     */
    private String translateTimeSlot(String timeSlot) {
        return switch (timeSlot) {
            case "morning" -> "아침";
            case "lunch" -> "점심";
            case "afternoon" -> "오후";
            case "dinner" -> "저녁";
            case "lateNight" -> "야식";
            default -> timeSlot;
        };
    }

    /**
     * 요일 번역
     */
    private String translateDayOfWeek(String dayOfWeek) {
        return switch (dayOfWeek) {
            case "MONDAY" -> "월요일";
            case "TUESDAY" -> "화요일";
            case "WEDNESDAY" -> "수요일";
            case "THURSDAY" -> "목요일";
            case "FRIDAY" -> "금요일";
            case "SATURDAY" -> "토요일";
            case "SUNDAY" -> "일요일";
            default -> dayOfWeek;
        };
    }

    /**
     * JSON 이스케이프
     */
    private String escapeJson(String str) {
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Claude API 응답 래퍼
     */
    private record ClaudeResponse(String content, int tokensUsed) {}
}
