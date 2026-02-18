package com.foodreview.domain.recommendation.batch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodreview.domain.recommendation.entity.UserTasteProfile;
import com.foodreview.domain.recommendation.repository.UserTasteProfileRepository;
import com.foodreview.domain.restaurant.entity.Restaurant;
import com.foodreview.domain.review.entity.Review;
import com.foodreview.domain.review.repository.ReviewRepository;
import com.foodreview.domain.user.entity.User;
import com.foodreview.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 사용자 취향 프로필 분석 배치 Job
 * - 사용자별 리뷰 데이터를 분석하여 UserTasteProfile 테이블에 저장
 * - 매일 새벽 3시에 실행 (TasteProfileScheduler에서 트리거)
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class TasteProfileBatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final UserTasteProfileRepository tasteProfileRepository;
    private final ObjectMapper objectMapper;

    // 분석에 필요한 최소 리뷰 수
    private static final int MIN_REVIEWS_FOR_ANALYSIS = 3;
    // 최근 N개 음식점만 저장
    private static final int RECENT_RESTAURANTS_LIMIT = 10;
    // 키워드 추출 최대 개수
    private static final int MAX_KEYWORDS = 10;

    @Bean
    public Job tasteProfileJob() {
        return new JobBuilder("tasteProfileJob", jobRepository)
                .start(tasteProfileStep())
                .build();
    }

    @Bean
    public Step tasteProfileStep() {
        return new StepBuilder("tasteProfileStep", jobRepository)
                .<User, UserTasteProfile>chunk(10, transactionManager)
                .reader(tasteProfileUserReader())
                .processor(tasteProfileProcessor())
                .writer(tasteProfileWriter())
                .build();
    }

    @Bean
    public ItemReader<User> tasteProfileUserReader() {
        // 활성 사용자 중 리뷰가 있는 사용자만 조회
        List<User> users = userRepository.findAllActiveUsersWithCategories();
        log.info("TasteProfile batch: {} users to process", users.size());
        return new ListItemReader<>(users);
    }

    @Bean
    public ItemProcessor<User, UserTasteProfile> tasteProfileProcessor() {
        return user -> {
            Long userId = user.getId();

            // 사용자의 모든 리뷰 조회
            List<Review> reviews = reviewRepository.findByUserIdOrderByCreatedAtDesc(userId);

            if (reviews.size() < MIN_REVIEWS_FOR_ANALYSIS) {
                log.debug("User {}: {} reviews (< {}), skipping", userId, reviews.size(), MIN_REVIEWS_FOR_ANALYSIS);
                return null;
            }

            try {
                // 1. 선호 카테고리 분석 (카테고리별 리뷰 수 + 평균 평점 가중치)
                Map<String, CategoryStats> categoryStats = analyzeCategoryPreferences(reviews);
                List<String> preferredCategories = getTopCategories(categoryStats, 3);
                List<String> dislikedCategories = getDislikedCategories(categoryStats);

                // 2. 평균 평점 계산
                double avgRating = reviews.stream()
                        .mapToDouble(r -> r.getRating().doubleValue())
                        .average()
                        .orElse(0.0);

                // 3. 시간대별 선호 분석
                Map<String, List<String>> timeSlotPreferences = analyzeTimeSlotPreferences(reviews);

                // 4. 요일+시간대별 선호 분석
                Map<String, List<String>> dayTimePreferences = analyzeDayTimePreferences(reviews);

                // 5. 연속 패턴 분석 (이전 식사 → 다음 식사)
                Map<String, List<String>> sequencePatterns = analyzeSequencePatterns(reviews);

                // 6. 키워드 추출 (리뷰 내용에서 자주 등장하는 단어)
                List<String> keywords = extractKeywords(reviews);

                // 7. 최근 방문 음식점
                List<Long> recentRestaurants = reviews.stream()
                        .limit(RECENT_RESTAURANTS_LIMIT)
                        .map(r -> r.getRestaurant().getId())
                        .collect(Collectors.toList());

                // 8. 가격대 선호 분석 (향후 확장용 - 현재는 mid 고정)
                String pricePreference = "mid";

                // 기존 프로필 조회 또는 새로 생성
                UserTasteProfile profile = tasteProfileRepository.findByUserId(userId)
                        .orElse(UserTasteProfile.builder()
                                .user(user)
                                .build());

                // 프로필 업데이트
                profile.updateProfile(
                        toJson(preferredCategories),
                        toJson(dislikedCategories),
                        avgRating,
                        reviews.size(),
                        toJson(timeSlotPreferences),
                        toJson(dayTimePreferences),
                        toJson(sequencePatterns),
                        toJson(keywords),
                        toJson(recentRestaurants),
                        pricePreference
                );

                log.debug("User {}: profile updated (reviews: {}, preferred: {})",
                        userId, reviews.size(), preferredCategories);
                return profile;

            } catch (Exception e) {
                log.error("User {}: profile analysis failed", userId, e);
                return null;
            }
        };
    }

    @Bean
    public ItemWriter<UserTasteProfile> tasteProfileWriter() {
        return items -> {
            for (UserTasteProfile profile : items) {
                if (profile != null) {
                    tasteProfileRepository.save(profile);
                }
            }
            log.info("TasteProfile batch: {} profiles saved", items.size());
        };
    }

    /**
     * 카테고리별 통계 분석
     */
    private Map<String, CategoryStats> analyzeCategoryPreferences(List<Review> reviews) {
        Map<String, CategoryStats> stats = new HashMap<>();

        for (Review review : reviews) {
            String category = review.getRestaurant().getCategory().name();
            stats.computeIfAbsent(category, k -> new CategoryStats())
                    .addReview(review.getRating().doubleValue());
        }

        return stats;
    }

    /**
     * 상위 선호 카테고리 추출
     */
    private List<String> getTopCategories(Map<String, CategoryStats> stats, int limit) {
        return stats.entrySet().stream()
                .sorted((e1, e2) -> {
                    // 점수 = (리뷰 수 * 0.6) + (평균 평점 * 0.4)
                    double score1 = e1.getValue().getScore();
                    double score2 = e2.getValue().getScore();
                    return Double.compare(score2, score1);
                })
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * 비선호 카테고리 추출 (평균 평점 3.0 미만인 카테고리)
     */
    private List<String> getDislikedCategories(Map<String, CategoryStats> stats) {
        return stats.entrySet().stream()
                .filter(e -> e.getValue().getAvgRating() < 3.0 && e.getValue().getCount() >= 2)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * 시간대별 선호 카테고리 분석
     */
    private Map<String, List<String>> analyzeTimeSlotPreferences(List<Review> reviews) {
        Map<String, Map<String, Integer>> timeSlotCategoryCount = new HashMap<>();

        for (Review review : reviews) {
            LocalDateTime createdAt = review.getCreatedAt();
            String timeSlot = getTimeSlot(createdAt.getHour());
            String category = review.getRestaurant().getCategory().name();

            timeSlotCategoryCount
                    .computeIfAbsent(timeSlot, k -> new HashMap<>())
                    .merge(category, 1, Integer::sum);
        }

        Map<String, List<String>> result = new HashMap<>();
        for (Map.Entry<String, Map<String, Integer>> entry : timeSlotCategoryCount.entrySet()) {
            List<String> topCategories = entry.getValue().entrySet().stream()
                    .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                    .limit(2)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
            result.put(entry.getKey(), topCategories);
        }

        return result;
    }

    /**
     * 요일+시간대별 선호 분석
     */
    private Map<String, List<String>> analyzeDayTimePreferences(List<Review> reviews) {
        Map<String, Map<String, Integer>> dayTimeCategoryCount = new HashMap<>();

        for (Review review : reviews) {
            LocalDateTime createdAt = review.getCreatedAt();
            String dayOfWeek = createdAt.getDayOfWeek().name().toLowerCase().substring(0, 3);
            String timeSlot = getTimeSlot(createdAt.getHour());
            String key = dayOfWeek + "_" + timeSlot;
            String category = review.getRestaurant().getCategory().name();

            dayTimeCategoryCount
                    .computeIfAbsent(key, k -> new HashMap<>())
                    .merge(category, 1, Integer::sum);
        }

        Map<String, List<String>> result = new HashMap<>();
        for (Map.Entry<String, Map<String, Integer>> entry : dayTimeCategoryCount.entrySet()) {
            if (entry.getValue().values().stream().mapToInt(Integer::intValue).sum() >= 2) {
                List<String> topCategories = entry.getValue().entrySet().stream()
                        .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                        .limit(2)
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toList());
                result.put(entry.getKey(), topCategories);
            }
        }

        return result;
    }

    /**
     * 연속 패턴 분석 (이전 카테고리 → 다음 카테고리)
     */
    private Map<String, List<String>> analyzeSequencePatterns(List<Review> reviews) {
        Map<String, Map<String, Integer>> sequenceCounts = new HashMap<>();

        // 최근 순으로 정렬된 리뷰에서 연속 패턴 추출
        List<Review> sortedReviews = reviews.stream()
                .sorted(Comparator.comparing(Review::getCreatedAt))
                .collect(Collectors.toList());

        for (int i = 0; i < sortedReviews.size() - 1; i++) {
            Review current = sortedReviews.get(i);
            Review next = sortedReviews.get(i + 1);

            // 24시간 이내의 연속 식사만 패턴으로 인식
            if (java.time.Duration.between(current.getCreatedAt(), next.getCreatedAt()).toHours() <= 24) {
                String currentCategory = current.getRestaurant().getCategory().getDisplayName();
                String nextCategory = next.getRestaurant().getCategory().getDisplayName();

                sequenceCounts
                        .computeIfAbsent(currentCategory, k -> new HashMap<>())
                        .merge(nextCategory, 1, Integer::sum);
            }
        }

        // 빈도가 2회 이상인 패턴만 저장
        Map<String, List<String>> result = new HashMap<>();
        for (Map.Entry<String, Map<String, Integer>> entry : sequenceCounts.entrySet()) {
            List<String> frequentNext = entry.getValue().entrySet().stream()
                    .filter(e -> e.getValue() >= 2)
                    .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                    .limit(2)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            if (!frequentNext.isEmpty()) {
                result.put(entry.getKey(), frequentNext);
            }
        }

        return result;
    }

    /**
     * 리뷰 내용에서 키워드 추출
     */
    private List<String> extractKeywords(List<Review> reviews) {
        // 긍정적 키워드 목록 (자주 사용되는 맛집 관련 표현)
        Set<String> positiveKeywords = Set.of(
                "맛있어요", "맛있다", "존맛", "대박", "추천", "최고", "굿", "좋아요",
                "가성비", "친절", "분위기", "깔끔", "신선", "푸짐", "양많음",
                "재방문", "또올게", "단골", "혼밥", "데이트", "모임"
        );

        Map<String, Integer> keywordCount = new HashMap<>();

        for (Review review : reviews) {
            String content = review.getContent();
            if (content == null) continue;

            for (String keyword : positiveKeywords) {
                if (content.contains(keyword)) {
                    keywordCount.merge(keyword, 1, Integer::sum);
                }
            }
        }

        return keywordCount.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(MAX_KEYWORDS)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * 시간대 분류
     */
    private String getTimeSlot(int hour) {
        if (hour >= 6 && hour <= 10) return "morning";
        if (hour >= 11 && hour <= 14) return "lunch";
        if (hour >= 15 && hour <= 16) return "afternoon";
        if (hour >= 17 && hour <= 21) return "dinner";
        return "lateNight";
    }

    /**
     * JSON 변환
     */
    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("JSON 변환 실패", e);
            return "[]";
        }
    }

    /**
     * 카테고리 통계 내부 클래스
     */
    private static class CategoryStats {
        private int count = 0;
        private double totalRating = 0;

        void addReview(double rating) {
            count++;
            totalRating += rating;
        }

        int getCount() {
            return count;
        }

        double getAvgRating() {
            return count > 0 ? (double) totalRating / count : 0;
        }

        double getScore() {
            // 점수 = (리뷰 수 * 0.6) + (평균 평점 * 0.4 * 2)
            return (count * 0.6) + (getAvgRating() * 0.4 * 2);
        }
    }
}
