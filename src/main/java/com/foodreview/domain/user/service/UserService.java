package com.foodreview.domain.user.service;

import com.foodreview.domain.auth.repository.RefreshTokenRepository;
import com.foodreview.domain.notification.service.FcmService;
import com.foodreview.domain.review.repository.ReviewRepository;
import com.foodreview.domain.review.repository.SympathyRepository;
import com.foodreview.domain.user.dto.ScoreEventDto;
import com.foodreview.domain.user.dto.UserDto;
import com.foodreview.domain.user.entity.Follow;
import com.foodreview.domain.user.entity.User;
import com.foodreview.domain.user.entity.UserBlock;
import com.foodreview.domain.user.repository.FollowRepository;
import com.foodreview.domain.user.repository.ScoreEventRepository;
import com.foodreview.domain.user.repository.UserBlockRepository;
import com.foodreview.domain.user.repository.UserRepository;
import com.foodreview.global.common.PageResponse;
import com.foodreview.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final ScoreEventRepository scoreEventRepository;
    private final UserBlockRepository userBlockRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final FcmService fcmService;
    private final SympathyRepository sympathyRepository;
    private final ReviewRepository reviewRepository;

    // 추천 점수 가중치 상수
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

    public UserDto.Response getUser(Long userId) {
        User user = findUserById(userId);
        Integer rank = userRepository.findRankByRegionAndScore(user.getRegion(), user.getTasteScore());
        return UserDto.Response.from(user, rank);
    }

    public UserDto.Response getMyProfile(Long userId) {
        return getUser(userId);
    }

    @Transactional
    public UserDto.Response updateProfile(Long userId, UserDto.UpdateRequest request) {
        User user = findUserById(userId);
        user.updateProfile(
                request.getName(),
                request.getAvatar(),
                request.getRegion(),
                request.getDistrict(),
                request.getNeighborhood(),
                request.getFavoriteCategories()
        );
        Integer rank = userRepository.findRankByRegionAndScore(user.getRegion(), user.getTasteScore());
        return UserDto.Response.from(user, rank);
    }

    // 랭킹 조회
    public PageResponse<UserDto.RankingResponse> getRanking(String region, Pageable pageable) {
        Page<User> users;
        if (region != null && !region.isEmpty()) {
            users = userRepository.findByRegionOrderByTasteScoreDesc(region, pageable);
        } else {
            users = userRepository.findAllByOrderByTasteScoreDesc(pageable);
        }

        int startRank = pageable.getPageNumber() * pageable.getPageSize() + 1;
        List<UserDto.RankingResponse> rankings = IntStream.range(0, users.getContent().size())
                .mapToObj(i -> UserDto.RankingResponse.from(users.getContent().get(i), startRank + i))
                .toList();

        return PageResponse.from(users, rankings);
    }

    // 친구 추천 (점수 기반 종합 추천)
    public List<UserDto.RecommendResponse> getRecommendedFriends(Long userId, int limit) {
        User currentUser = findUserById(userId);
        Set<Long> followingIds = new HashSet<>(followRepository.findFollowingIdsByFollowerId(userId));
        Set<Long> blockedIds = new HashSet<>(userBlockRepository.findBlockedUserIdsByBlockerId(userId));

        // 후보자별 점수 저장
        Map<Long, RecommendationScore> scoreMap = new HashMap<>();

        // 1. 2촌 관계 점수 계산
        List<Object[]> secondDegreeData = followRepository.findSecondDegreeConnections(userId);
        for (Object[] row : secondDegreeData) {
            Long candidateId = (Long) row[0];
            Long mutualCount = (Long) row[1];
            if (followingIds.contains(candidateId) || blockedIds.contains(candidateId)) continue;

            int score = Math.min((int) (mutualCount * SECOND_DEGREE_SCORE_PER_CONNECTION), SECOND_DEGREE_MAX_SCORE);
            scoreMap.computeIfAbsent(candidateId, k -> new RecommendationScore())
                    .addSecondDegreeScore(score, mutualCount.intValue());
        }

        // 2. 공감 기반 점수 계산
        // 2-1. 내가 공감한 리뷰 작성자
        List<Object[]> sympathizedAuthors = sympathyRepository.findSympathizedAuthors(userId);
        for (Object[] row : sympathizedAuthors) {
            Long candidateId = (Long) row[0];
            Long count = (Long) row[1];
            if (followingIds.contains(candidateId) || blockedIds.contains(candidateId)) continue;

            int score = (int) (count * SYMPATHY_OUTGOING_SCORE);
            scoreMap.computeIfAbsent(candidateId, k -> new RecommendationScore())
                    .addOutgoingSympathyScore(score, count.intValue());
        }

        // 2-2. 내 리뷰에 공감한 사용자
        List<Object[]> sympathizers = sympathyRepository.findSympathizers(userId);
        for (Object[] row : sympathizers) {
            Long candidateId = (Long) row[0];
            Long count = (Long) row[1];
            if (followingIds.contains(candidateId) || blockedIds.contains(candidateId)) continue;

            int score = (int) (count * SYMPATHY_INCOMING_SCORE);
            scoreMap.computeIfAbsent(candidateId, k -> new RecommendationScore())
                    .addIncomingSympathyScore(score, count.intValue());
        }

        // 3. 취향 유사도 점수 계산
        List<Object[]> commonRestaurantUsers = reviewRepository.findUsersWithCommonRestaurants(userId, MIN_COMMON_RESTAURANTS);
        for (Object[] row : commonRestaurantUsers) {
            Long candidateId = (Long) row[0];
            Long commonCount = (Long) row[1];
            if (followingIds.contains(candidateId) || blockedIds.contains(candidateId)) continue;

            // 별점 차이 기반 유사도 계산
            double similarity = calculateTasteSimilarity(userId, candidateId);
            if (similarity > 0) {
                int score = (int) (similarity * commonCount * TASTE_SCORE_MULTIPLIER);
                scoreMap.computeIfAbsent(candidateId, k -> new RecommendationScore())
                        .addTasteScore(score, similarity, commonCount.intValue());
            }
        }

        // 4. 기본 점수 추가 (같은 지역, 공통 카테고리)
        for (Long candidateId : scoreMap.keySet()) {
            User candidate = userRepository.findById(candidateId).orElse(null);
            if (candidate == null) continue;

            RecommendationScore recScore = scoreMap.get(candidateId);

            // 같은 지역 점수
            if (currentUser.getRegion() != null && currentUser.getRegion().equals(candidate.getRegion())) {
                recScore.addBaseScore(BASE_SCORE_SAME_REGION, "같은 지역");
            }

            // 공통 카테고리 점수
            List<String> commonCategories = findCommonCategories(currentUser, candidate);
            if (!commonCategories.isEmpty()) {
                recScore.addBaseScore(commonCategories.size() * BASE_SCORE_PER_CATEGORY, "공통 카테고리");
                recScore.setCommonCategories(commonCategories);
            }
        }

        // 5. 총점 계산 및 정렬
        List<Map.Entry<Long, RecommendationScore>> sortedCandidates = scoreMap.entrySet().stream()
                .sorted((e1, e2) -> Double.compare(e2.getValue().getTotalScore(), e1.getValue().getTotalScore()))
                .limit(limit)
                .toList();

        // 6. 결과 생성
        List<UserDto.RecommendResponse> result = new ArrayList<>();
        for (Map.Entry<Long, RecommendationScore> entry : sortedCandidates) {
            User candidate = userRepository.findById(entry.getKey()).orElse(null);
            if (candidate == null) continue;

            RecommendationScore recScore = entry.getValue();
            String reason = generateRecommendReason(userId, entry.getKey(), recScore);

            result.add(UserDto.RecommendResponse.from(candidate, recScore.getCommonCategories(), reason));
        }

        log.debug("Friend recommendations for user {}: {} candidates scored", userId, scoreMap.size());
        return result;
    }

    // 취향 유사도 계산 (별점 차이 기반)
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
        return 1 - (avgDiff / 5.0); // 0~1 사이 값
    }

    private List<String> findCommonCategories(User user1, User user2) {
        if (user1.getFavoriteCategories() == null || user2.getFavoriteCategories() == null) {
            return new ArrayList<>();
        }
        List<String> common = new ArrayList<>(user1.getFavoriteCategories());
        common.retainAll(user2.getFavoriteCategories());
        return common;
    }

    private String generateRecommendReason(Long userId, Long candidateId, RecommendationScore score) {
        // 가장 높은 점수를 받은 요소 기반으로 추천 이유 생성
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

        // 기본 추천 이유
        if (!score.getCommonCategories().isEmpty()) {
            return String.format("공통 관심사: %s", String.join(", ", score.getCommonCategories()));
        }

        return "추천 맛잘알";
    }

    // 추천 점수 내부 클래스
    private static class RecommendationScore {
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

        void addBaseScore(int score, String reason) {
            this.baseScore += score;
        }

        void setCommonCategories(List<String> categories) {
            this.commonCategories = categories;
        }

        List<String> getCommonCategories() {
            return commonCategories;
        }

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

        int getIncomingSympathyCount() {
            return incomingSympathyCount;
        }

        double getTasteSimilarity() {
            return tasteSimilarity;
        }
    }

    // 팔로우
    @Transactional
    public void follow(Long followerId, Long followingId) {
        if (followerId.equals(followingId)) {
            throw new CustomException("자기 자신을 팔로우할 수 없습니다", HttpStatus.BAD_REQUEST);
        }

        User follower = findUserById(followerId);
        User following = findUserById(followingId);

        if (followRepository.existsByFollowerAndFollowing(follower, following)) {
            throw new CustomException("이미 팔로우한 사용자입니다", HttpStatus.CONFLICT);
        }

        Follow follow = Follow.builder()
                .follower(follower)
                .following(following)
                .build();

        followRepository.save(follow);
    }

    // 언팔로우
    @Transactional
    public void unfollow(Long followerId, Long followingId) {
        User follower = findUserById(followerId);
        User following = findUserById(followingId);

        Follow follow = followRepository.findByFollowerAndFollowing(follower, following)
                .orElseThrow(() -> new CustomException("팔로우 관계가 없습니다", HttpStatus.NOT_FOUND));

        followRepository.delete(follow);
    }

    // 팔로잉 목록
    public PageResponse<UserDto.SimpleResponse> getFollowings(Long userId, Pageable pageable) {
        User user = findUserById(userId);
        Page<User> followings = followRepository.findFollowingsByFollower(user, pageable);
        List<UserDto.SimpleResponse> content = followings.getContent().stream()
                .map(UserDto.SimpleResponse::from)
                .toList();
        return PageResponse.from(followings, content);
    }

    // 팔로워 목록
    public PageResponse<UserDto.SimpleResponse> getFollowers(Long userId, Pageable pageable) {
        User user = findUserById(userId);
        Page<User> followers = followRepository.findFollowersByFollowing(user, pageable);
        List<UserDto.SimpleResponse> content = followers.getContent().stream()
                .map(UserDto.SimpleResponse::from)
                .toList();
        return PageResponse.from(followers, content);
    }

    // 점수 획득 내역
    public PageResponse<ScoreEventDto.Response> getScoreHistory(Long userId, Pageable pageable) {
        User user = findUserById(userId);
        var events = scoreEventRepository.findByUserOrderByCreatedAtDesc(user, pageable);
        List<ScoreEventDto.Response> content = events.getContent().stream()
                .map(ScoreEventDto.Response::from)
                .toList();
        return PageResponse.from(events, content);
    }

    // 팔로우 여부 확인 (단일 쿼리로 최적화)
    public boolean isFollowing(Long followerId, Long followingId) {
        return followRepository.existsByFollowerIdAndFollowingId(followerId, followingId);
    }

    // 사용자 차단
    @Transactional
    public void blockUser(Long blockerId, Long blockedUserId) {
        if (blockerId.equals(blockedUserId)) {
            throw new CustomException("자기 자신을 차단할 수 없습니다", HttpStatus.BAD_REQUEST);
        }

        User blocker = findUserById(blockerId);
        User blockedUser = findUserById(blockedUserId);

        if (userBlockRepository.existsByBlockerAndBlockedUser(blocker, blockedUser)) {
            throw new CustomException("이미 차단한 사용자입니다", HttpStatus.CONFLICT);
        }

        UserBlock userBlock = UserBlock.builder()
                .blocker(blocker)
                .blockedUser(blockedUser)
                .build();

        userBlockRepository.save(userBlock);

        // 팔로우 관계가 있으면 해제
        followRepository.findByFollowerAndFollowing(blocker, blockedUser)
                .ifPresent(followRepository::delete);
        followRepository.findByFollowerAndFollowing(blockedUser, blocker)
                .ifPresent(followRepository::delete);
    }

    // 사용자 차단 해제
    @Transactional
    public void unblockUser(Long blockerId, Long blockedUserId) {
        User blocker = findUserById(blockerId);
        User blockedUser = findUserById(blockedUserId);

        UserBlock userBlock = userBlockRepository.findByBlockerAndBlockedUser(blocker, blockedUser)
                .orElseThrow(() -> new CustomException("차단 관계가 없습니다", HttpStatus.NOT_FOUND));

        userBlockRepository.delete(userBlock);
    }

    // 차단 목록 조회
    public PageResponse<UserDto.BlockedUserResponse> getBlockedUsers(Long userId, Pageable pageable) {
        User user = findUserById(userId);
        Page<User> blockedUsers = userBlockRepository.findBlockedUsersByBlocker(user, pageable);

        // 차단 시점 정보를 위해 UserBlock 엔티티도 조회
        List<Long> blockedUserIds = blockedUsers.getContent().stream().map(User::getId).toList();
        List<UserBlock> blocks = userBlockRepository.findAll().stream()
                .filter(ub -> ub.getBlocker().getId().equals(userId) && blockedUserIds.contains(ub.getBlockedUser().getId()))
                .toList();

        List<UserDto.BlockedUserResponse> content = blockedUsers.getContent().stream()
                .map(blockedUser -> {
                    var block = blocks.stream()
                            .filter(ub -> ub.getBlockedUser().getId().equals(blockedUser.getId()))
                            .findFirst()
                            .orElse(null);
                    return UserDto.BlockedUserResponse.from(blockedUser, block != null ? block.getCreatedAt() : null);
                })
                .toList();

        return PageResponse.from(blockedUsers, content);
    }

    // 차단 여부 확인
    public boolean isBlocked(Long blockerId, Long blockedUserId) {
        return userBlockRepository.existsByBlockerIdAndBlockedUserId(blockerId, blockedUserId);
    }

    // 차단된 사용자 ID 목록 조회 (필터링용)
    public List<Long> getBlockedUserIds(Long userId) {
        return userBlockRepository.findBlockedUserIdsByBlockerId(userId);
    }

    // 사용자 검색
    public PageResponse<UserDto.SearchResponse> searchUsers(String query, Long currentUserId, Pageable pageable) {
        Page<User> users = userRepository.findByNameContainingIgnoreCase(query, pageable);

        // 현재 사용자의 팔로잉 목록 조회
        Set<Long> followingIds = currentUserId != null
                ? new HashSet<>(followRepository.findFollowingIdsByFollowerId(currentUserId))
                : new HashSet<>();

        List<UserDto.SearchResponse> content = users.getContent().stream()
                .filter(user -> !user.getId().equals(currentUserId)) // 자기 자신 제외
                .map(user -> UserDto.SearchResponse.from(user, followingIds.contains(user.getId())))
                .toList();

        return PageResponse.from(users, content);
    }

    // 알림 설정 조회
    public UserDto.NotificationSettingsResponse getNotificationSettings(Long userId) {
        User user = findUserById(userId);
        return UserDto.NotificationSettingsResponse.from(user);
    }

    // 알림 설정 업데이트
    @Transactional
    public UserDto.NotificationSettingsResponse updateNotificationSettings(Long userId, UserDto.NotificationSettingsRequest request) {
        User user = findUserById(userId);
        user.updateNotificationSettings(
                request.getReviews(),
                request.getFollows(),
                request.getMessages(),
                request.getMarketing()
        );
        return UserDto.NotificationSettingsResponse.from(user);
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException("사용자를 찾을 수 없습니다", HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));
    }

    // 회원 탈퇴
    @Transactional
    public void withdraw(Long userId) {
        User user = findUserById(userId);

        if (user.isDeleted()) {
            throw new CustomException("이미 탈퇴한 계정입니다", HttpStatus.BAD_REQUEST, "ALREADY_WITHDRAWN");
        }

        // 1. 모든 RefreshToken 무효화
        refreshTokenRepository.revokeAllByUser(user);

        // 2. 팔로우 관계 삭제
        followRepository.deleteByFollowerOrFollowing(user, user);

        // 3. 차단 관계 삭제
        userBlockRepository.deleteByBlockerOrBlockedUser(user, user);

        // 4. FCM 토큰 삭제
        fcmService.deleteUserTokens(user);

        // 5. 사용자 정보 익명화 및 소프트 삭제
        user.withdraw();
    }
}
