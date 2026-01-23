package com.foodreview.domain.user.service;

import com.foodreview.domain.auth.repository.RefreshTokenRepository;
import com.foodreview.domain.badge.service.BadgeService;
import com.foodreview.domain.notification.service.FcmService;
import com.foodreview.domain.review.repository.ReviewRepository;
import com.foodreview.domain.review.repository.SympathyRepository;
import com.foodreview.domain.user.dto.ScoreEventDto;
import com.foodreview.domain.user.dto.UserDto;
import com.foodreview.domain.user.entity.Follow;
import com.foodreview.domain.user.entity.RecommendationCache;
import com.foodreview.domain.user.entity.User;
import com.foodreview.domain.user.entity.UserBlock;
import com.foodreview.domain.user.repository.FollowRepository;
import com.foodreview.domain.user.repository.RecommendationCacheRepository;
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

    // 추천 점수 기준치 (이 점수 이상이어야 추천 목록에 표시)
    private static final int MIN_RECOMMEND_SCORE = 20;
    // 점수가 낮아도 최소 보장되는 추천 인원
    private static final int MIN_GUARANTEED_COUNT = 3;

    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final ScoreEventRepository scoreEventRepository;
    private final UserBlockRepository userBlockRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final FcmService fcmService;
    private final SympathyRepository sympathyRepository;
    private final ReviewRepository reviewRepository;
    private final RecommendationCacheRepository recommendationCacheRepository;
    private final BadgeService badgeService;


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

    // 친구 추천 (캐시 테이블에서 조회)
    public List<UserDto.RecommendResponse> getRecommendedFriends(Long userId, int limit) {
        findUserById(userId); // 사용자 존재 확인

        // 차단된 사용자와 이미 팔로우 중인 사용자 제외
        Set<Long> followingIds = new HashSet<>(followRepository.findFollowingIdsByFollowerId(userId));
        List<Long> blockedIds = userBlockRepository.findBlockedUserIdsByBlockerId(userId);

        // 캐시 테이블에서 추천 목록 조회
        List<RecommendationCache> cachedRecommendations;
        if (blockedIds.isEmpty()) {
            cachedRecommendations = recommendationCacheRepository
                    .findByUserIdOrderByTotalScoreDesc(userId, org.springframework.data.domain.PageRequest.of(0, limit * 2));
        } else {
            cachedRecommendations = recommendationCacheRepository
                    .findByUserIdExcludingBlocked(userId, blockedIds, org.springframework.data.domain.PageRequest.of(0, limit * 2));
        }

        // 이미 팔로우 중인 사용자 제외하고 결과 생성
        // 상위 MIN_GUARANTEED_COUNT명은 점수 무관하게 항상 포함, 이후는 기준치 이상만 포함
        List<UserDto.RecommendResponse> result = new ArrayList<>();
        int validCandidateCount = 0; // 유효한 후보 순위 (팔로우/삭제 제외 후)

        for (RecommendationCache cache : cachedRecommendations) {
            if (result.size() >= limit) break;
            if (followingIds.contains(cache.getRecommendedUserId())) continue;

            User candidate = userRepository.findById(cache.getRecommendedUserId()).orElse(null);
            if (candidate == null || candidate.isDeleted()) continue;

            validCandidateCount++;

            // 상위 3명은 점수 무관하게 포함, 이후는 기준치 이상만
            if (validCandidateCount > MIN_GUARANTEED_COUNT && cache.getTotalScore() < MIN_RECOMMEND_SCORE) {
                continue;
            }

            List<String> commonCategories = cache.getCommonCategories() != null && !cache.getCommonCategories().isEmpty()
                    ? Arrays.asList(cache.getCommonCategories().split(","))
                    : new ArrayList<>();

            result.add(UserDto.RecommendResponse.from(candidate, commonCategories, cache.getReason()));
        }

        log.debug("Friend recommendations for user {} from cache: {} results (guaranteed: {}, threshold: {})",
                userId, result.size(), MIN_GUARANTEED_COUNT, MIN_RECOMMEND_SCORE);
        return result;
    }

    // 추천 점수 상세 조회 (디버깅/Admin용)
    public UserDto.RecommendationScoreDetail getRecommendationScoreDetail(Long userId, Long recommendedUserId) {
        RecommendationCache cache = recommendationCacheRepository
                .findByUserIdAndRecommendedUserId(userId, recommendedUserId)
                .orElseThrow(() -> new CustomException("추천 캐시를 찾을 수 없습니다", HttpStatus.NOT_FOUND));

        return UserDto.RecommendationScoreDetail.from(cache);
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

        // 팔로워 배지 체크 (팔로우 받은 사용자)
        int followerCount = (int) followRepository.countByFollowing(following);
        badgeService.checkAndAwardFollowerBadges(followingId, followerCount);
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
