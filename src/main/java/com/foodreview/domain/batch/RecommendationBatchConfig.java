package com.foodreview.domain.batch;

import com.foodreview.domain.review.repository.ReviewRepository;
import com.foodreview.domain.review.repository.SympathyRepository;
import com.foodreview.domain.user.entity.RecommendationCache;
import com.foodreview.domain.user.entity.User;
import com.foodreview.domain.user.repository.FollowRepository;
import com.foodreview.domain.user.repository.RecommendationCacheRepository;
import com.foodreview.domain.user.repository.UserBlockRepository;
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

@Slf4j
@Configuration
@RequiredArgsConstructor
public class RecommendationBatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final SympathyRepository sympathyRepository;
    private final ReviewRepository reviewRepository;
    private final UserBlockRepository userBlockRepository;
    private final RecommendationCacheRepository recommendationCacheRepository;

    // 추천 점수 가중치 상수 (UserService와 동일하게 유지)
    private static final double WEIGHT_SECOND_DEGREE = 1.0;
    private static final double WEIGHT_SYMPATHY = 1.0;
    private static final double WEIGHT_TASTE = 1.0;
    private static final int SECOND_DEGREE_SCORE_PER_CONNECTION = 10;
    private static final int SECOND_DEGREE_MAX_SCORE = 50;
    private static final int SYMPATHY_OUTGOING_SCORE = 8;
    private static final int SYMPATHY_INCOMING_SCORE = 5;
    private static final int TASTE_SCORE_MULTIPLIER = 10;
    private static final int MIN_COMMON_RESTAURANTS = 2;
    private static final int BASE_SCORE_SAME_REGION = 10;
    private static final int BASE_SCORE_PER_CATEGORY = 3;
    private static final int MAX_RECOMMENDATIONS_PER_USER = 50;

    @Bean
    public Job recommendationJob() {
        return new JobBuilder("recommendationJob", jobRepository)
                .start(recommendationStep())
                .build();
    }

    @Bean
    public Step recommendationStep() {
        return new StepBuilder("recommendationStep", jobRepository)
                .<User, List<RecommendationCache>>chunk(10, transactionManager)
                .reader(userItemReader())
                .processor(recommendationProcessor())
                .writer(recommendationWriter())
                .build();
    }

    @Bean
    public ItemReader<User> userItemReader() {
        // 삭제되지 않은 활성 사용자만 조회 (favoriteCategories 함께 로드)
        List<User> users = userRepository.findAllActiveUsersWithCategories();
        log.info("Recommendation batch: {} active users to process", users.size());
        return new ListItemReader<>(users);
    }

    @Bean
    public ItemProcessor<User, List<RecommendationCache>> recommendationProcessor() {
        return user -> {
            Long userId = user.getId();
            Set<Long> followingIds = new HashSet<>(followRepository.findFollowingIdsByFollowerId(userId));
            Set<Long> blockedIds = new HashSet<>(userBlockRepository.findBlockedUserIdsByBlockerId(userId));

            // 자기 자신도 제외
            followingIds.add(userId);
            blockedIds.add(userId);

            Map<Long, RecommendationScoreData> scoreMap = new HashMap<>();

            // 1. 2촌 관계 점수 계산
            List<Object[]> secondDegreeData = followRepository.findSecondDegreeConnections(userId);
            for (Object[] row : secondDegreeData) {
                Long candidateId = (Long) row[0];
                Long mutualCount = (Long) row[1];
                if (followingIds.contains(candidateId) || blockedIds.contains(candidateId)) continue;

                int score = Math.min((int) (mutualCount * SECOND_DEGREE_SCORE_PER_CONNECTION), SECOND_DEGREE_MAX_SCORE);
                scoreMap.computeIfAbsent(candidateId, k -> new RecommendationScoreData())
                        .addSecondDegreeScore(score, mutualCount.intValue());
            }

            // 2. 공감 기반 점수 계산
            List<Object[]> sympathizedAuthors = sympathyRepository.findSympathizedAuthors(userId);
            for (Object[] row : sympathizedAuthors) {
                Long candidateId = (Long) row[0];
                Long count = (Long) row[1];
                if (followingIds.contains(candidateId) || blockedIds.contains(candidateId)) continue;

                int score = (int) (count * SYMPATHY_OUTGOING_SCORE);
                scoreMap.computeIfAbsent(candidateId, k -> new RecommendationScoreData())
                        .addOutgoingSympathyScore(score, count.intValue());
            }

            List<Object[]> sympathizers = sympathyRepository.findSympathizers(userId);
            for (Object[] row : sympathizers) {
                Long candidateId = (Long) row[0];
                Long count = (Long) row[1];
                if (followingIds.contains(candidateId) || blockedIds.contains(candidateId)) continue;

                int score = (int) (count * SYMPATHY_INCOMING_SCORE);
                scoreMap.computeIfAbsent(candidateId, k -> new RecommendationScoreData())
                        .addIncomingSympathyScore(score, count.intValue());
            }

            // 3. 취향 유사도 점수 계산
            List<Object[]> commonRestaurantUsers = reviewRepository.findUsersWithCommonRestaurants(userId, MIN_COMMON_RESTAURANTS);
            for (Object[] row : commonRestaurantUsers) {
                Long candidateId = (Long) row[0];
                Long commonCount = (Long) row[1];
                if (followingIds.contains(candidateId) || blockedIds.contains(candidateId)) continue;

                double similarity = calculateTasteSimilarity(userId, candidateId);
                if (similarity > 0) {
                    int score = (int) (similarity * commonCount * TASTE_SCORE_MULTIPLIER);
                    scoreMap.computeIfAbsent(candidateId, k -> new RecommendationScoreData())
                            .addTasteScore(score, similarity, commonCount.intValue());
                }
            }

            // 4. 기본 점수 추가 (같은 지역, 공통 카테고리)
            for (Long candidateId : scoreMap.keySet()) {
                User candidate = userRepository.findByIdWithCategories(candidateId).orElse(null);
                if (candidate == null || candidate.isDeleted()) continue;

                RecommendationScoreData recScore = scoreMap.get(candidateId);

                if (user.getRegion() != null && user.getRegion().equals(candidate.getRegion())) {
                    recScore.addBaseScore(BASE_SCORE_SAME_REGION);
                }

                List<String> commonCategories = findCommonCategories(user, candidate);
                if (!commonCategories.isEmpty()) {
                    recScore.addBaseScore(commonCategories.size() * BASE_SCORE_PER_CATEGORY);
                    recScore.setCommonCategories(commonCategories);
                }
            }

            // 5. 총점 계산 및 상위 N개만 선택
            LocalDateTime now = LocalDateTime.now();
            List<RecommendationCache> results = new ArrayList<>();

            scoreMap.entrySet().stream()
                    .sorted((e1, e2) -> Double.compare(e2.getValue().getTotalScore(), e1.getValue().getTotalScore()))
                    .limit(MAX_RECOMMENDATIONS_PER_USER)
                    .forEach(entry -> {
                        Long candidateId = entry.getKey();
                        RecommendationScoreData data = entry.getValue();
                        String reason = generateRecommendReason(userId, candidateId, data);

                        RecommendationCache cache = RecommendationCache.builder()
                                .userId(userId)
                                .recommendedUserId(candidateId)
                                .totalScore((int) data.getTotalScore())
                                .secondDegreeScore(data.getSecondDegreeScore())
                                .secondDegreeCount(data.getSecondDegreeCount())
                                .outgoingSympathyScore(data.getOutgoingSympathyScore())
                                .outgoingSympathyCount(data.getOutgoingSympathyCount())
                                .incomingSympathyScore(data.getIncomingSympathyScore())
                                .incomingSympathyCount(data.getIncomingSympathyCount())
                                .tasteScore(data.getTasteScore())
                                .tasteSimilarity(data.getTasteSimilarity())
                                .commonRestaurantCount(data.getCommonRestaurantCount())
                                .baseScore(data.getBaseScore())
                                .reason(reason)
                                .commonCategories(String.join(",", data.getCommonCategories()))
                                .calculatedAt(now)
                                .build();

                        results.add(cache);
                    });

            log.debug("User {}: {} recommendations calculated", userId, results.size());
            return results.isEmpty() ? null : results;
        };
    }

    @Bean
    public ItemWriter<List<RecommendationCache>> recommendationWriter() {
        return items -> {
            for (List<RecommendationCache> caches : items) {
                if (caches == null || caches.isEmpty()) continue;

                Long userId = caches.get(0).getUserId();

                // 기존 캐시 삭제 후 새로 저장
                recommendationCacheRepository.deleteByUserId(userId);
                recommendationCacheRepository.saveAll(caches);
            }
        };
    }

    private double calculateTasteSimilarity(Long userId1, Long userId2) {
        List<Object[]> ratings = reviewRepository.findCommonRestaurantRatings(userId1, userId2);
        if (ratings.isEmpty()) return 0;

        double totalDiff = 0;
        for (Object[] row : ratings) {
            int rating1 = (Integer) row[1];
            int rating2 = (Integer) row[2];
            totalDiff += Math.abs(rating1 - rating2);
        }
        double avgDiff = totalDiff / ratings.size();
        return 1 - (avgDiff / 5.0);
    }

    private List<String> findCommonCategories(User user1, User user2) {
        if (user1.getFavoriteCategories() == null || user2.getFavoriteCategories() == null) {
            return new ArrayList<>();
        }
        List<String> common = new ArrayList<>(user1.getFavoriteCategories());
        common.retainAll(user2.getFavoriteCategories());
        return common;
    }

    private String generateRecommendReason(Long userId, Long candidateId, RecommendationScoreData score) {
        String primaryReason = score.getPrimaryReason();

        if ("secondDegree".equals(primaryReason)) {
            List<String> mutualNames = followRepository.findMutualFollowerNames(userId, candidateId);
            if (!mutualNames.isEmpty()) {
                String firstName = mutualNames.get(0);
                if (mutualNames.size() > 1) {
                    return String.format("%s님 외 %d명이 팔로우 중", firstName, mutualNames.size() - 1);
                }
                return String.format("%s님이 팔로우 중", firstName);
            }
        }

        if ("outgoingSympathy".equals(primaryReason)) {
            return "회원님이 공감한 리뷰어";
        }

        if ("incomingSympathy".equals(primaryReason)) {
            return String.format("회원님의 리뷰에 %d번 공감", score.getIncomingSympathyCount());
        }

        if ("taste".equals(primaryReason)) {
            int similarityPercent = (int) (score.getTasteSimilarity() * 100);
            return String.format("취향이 %d%% 일치해요", similarityPercent);
        }

        if (!score.getCommonCategories().isEmpty()) {
            return String.format("공통 관심사: %s", String.join(", ", score.getCommonCategories()));
        }

        return "추천 맛잘알";
    }

    // 내부 점수 데이터 클래스
    private static class RecommendationScoreData {
        private int secondDegreeScore = 0;
        private int secondDegreeCount = 0;
        private int outgoingSympathyScore = 0;
        private int outgoingSympathyCount = 0;
        private int incomingSympathyScore = 0;
        private int incomingSympathyCount = 0;
        private int tasteScore = 0;
        private double tasteSimilarity = 0;
        private int commonRestaurantCount = 0;
        private int baseScore = 0;
        private List<String> commonCategories = new ArrayList<>();

        void addSecondDegreeScore(int score, int count) {
            this.secondDegreeScore = score;
            this.secondDegreeCount = count;
        }

        void addOutgoingSympathyScore(int score, int count) {
            this.outgoingSympathyScore = score;
            this.outgoingSympathyCount = count;
        }

        void addIncomingSympathyScore(int score, int count) {
            this.incomingSympathyScore = score;
            this.incomingSympathyCount = count;
        }

        void addTasteScore(int score, double similarity, int commonCount) {
            this.tasteScore = score;
            this.tasteSimilarity = similarity;
            this.commonRestaurantCount = commonCount;
        }

        void addBaseScore(int score) {
            this.baseScore += score;
        }

        void setCommonCategories(List<String> categories) {
            this.commonCategories = categories;
        }

        List<String> getCommonCategories() {
            return commonCategories;
        }

        int getSecondDegreeScore() { return secondDegreeScore; }
        int getSecondDegreeCount() { return secondDegreeCount; }
        int getOutgoingSympathyScore() { return outgoingSympathyScore; }
        int getOutgoingSympathyCount() { return outgoingSympathyCount; }
        int getIncomingSympathyScore() { return incomingSympathyScore; }
        int getIncomingSympathyCount() { return incomingSympathyCount; }
        int getTasteScore() { return tasteScore; }
        double getTasteSimilarity() { return tasteSimilarity; }
        int getCommonRestaurantCount() { return commonRestaurantCount; }
        int getBaseScore() { return baseScore; }

        double getTotalScore() {
            return (secondDegreeScore * WEIGHT_SECOND_DEGREE)
                    + ((outgoingSympathyScore + incomingSympathyScore) * WEIGHT_SYMPATHY)
                    + (tasteScore * WEIGHT_TASTE)
                    + baseScore;
        }

        String getPrimaryReason() {
            double maxScore = 0;
            String reason = "base";

            if (secondDegreeScore * WEIGHT_SECOND_DEGREE > maxScore) {
                maxScore = secondDegreeScore * WEIGHT_SECOND_DEGREE;
                reason = "secondDegree";
            }
            if (outgoingSympathyScore * WEIGHT_SYMPATHY > maxScore) {
                maxScore = outgoingSympathyScore * WEIGHT_SYMPATHY;
                reason = "outgoingSympathy";
            }
            if (incomingSympathyScore * WEIGHT_SYMPATHY > maxScore) {
                maxScore = incomingSympathyScore * WEIGHT_SYMPATHY;
                reason = "incomingSympathy";
            }
            if (tasteScore * WEIGHT_TASTE > maxScore) {
                reason = "taste";
            }
            return reason;
        }
    }
}
