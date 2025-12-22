package com.foodreview.domain.user.service;

import com.foodreview.domain.user.dto.ScoreEventDto;
import com.foodreview.domain.user.dto.UserDto;
import com.foodreview.domain.user.entity.Follow;
import com.foodreview.domain.user.entity.User;
import com.foodreview.domain.user.repository.FollowRepository;
import com.foodreview.domain.user.repository.ScoreEventRepository;
import com.foodreview.domain.user.repository.UserRepository;
import com.foodreview.global.common.PageResponse;
import com.foodreview.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final ScoreEventRepository scoreEventRepository;

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

    // 친구 추천 (HashSet 사용으로 O(1) 검색 최적화)
    public List<UserDto.RecommendResponse> getRecommendedFriends(Long userId, int limit) {
        User currentUser = findUserById(userId);
        Set<Long> followingIds = new HashSet<>(followRepository.findFollowingIdsByFollowerId(userId));

        List<User> recommended = userRepository.findRecommendedFriends(
                currentUser.getRegion(), userId, PageRequest.of(0, limit + followingIds.size())
        );

        // 이미 팔로우한 사용자 제외
        List<UserDto.RecommendResponse> result = new ArrayList<>();
        for (User user : recommended) {
            if (result.size() >= limit) break;
            if (followingIds.contains(user.getId())) continue;

            List<String> commonCategories = findCommonCategories(currentUser, user);
            String reason = generateRecommendReason(currentUser, user, commonCategories);
            result.add(UserDto.RecommendResponse.from(user, commonCategories, reason));
        }

        return result;
    }

    private List<String> findCommonCategories(User user1, User user2) {
        List<String> common = new ArrayList<>(user1.getFavoriteCategories());
        common.retainAll(user2.getFavoriteCategories());
        return common;
    }

    private String generateRecommendReason(User currentUser, User recommended, List<String> commonCategories) {
        if (!commonCategories.isEmpty()) {
            return String.format("공통 관심사: %s", String.join(", ", commonCategories));
        }
        if (currentUser.getRegion().equals(recommended.getRegion())) {
            return "같은 지역의 맛잘알";
        }
        return "비슷한 점수대의 맛잘알";
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

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException("사용자를 찾을 수 없습니다", HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));
    }
}
