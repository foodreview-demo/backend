package com.foodreview.domain.badge.service;

import com.foodreview.domain.badge.dto.BadgeDto;
import com.foodreview.domain.badge.entity.Badge;
import com.foodreview.domain.badge.entity.BadgeCategory;
import com.foodreview.domain.badge.entity.BadgeConditionType;
import com.foodreview.domain.badge.entity.UserBadge;
import com.foodreview.domain.badge.repository.BadgeRepository;
import com.foodreview.domain.badge.repository.UserBadgeRepository;
import com.foodreview.domain.user.entity.User;
import com.foodreview.domain.user.repository.UserRepository;
import com.foodreview.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BadgeService {

    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final UserRepository userRepository;

    // 전체 배지 목록 조회 (사용자의 획득 여부 포함)
    public List<BadgeDto.Response> getAllBadges(Long userId) {
        List<Badge> allBadges = badgeRepository.findByActiveOrderBySortOrderAsc(true);
        Set<Long> acquiredBadgeIds = userBadgeRepository.findBadgeIdsByUserId(userId);

        Map<Long, UserBadge> userBadgeMap = new HashMap<>();
        if (!acquiredBadgeIds.isEmpty()) {
            userBadgeRepository.findByUserIdWithBadge(userId)
                    .forEach(ub -> userBadgeMap.put(ub.getBadge().getId(), ub));
        }

        return allBadges.stream()
                .map(badge -> {
                    boolean acquired = acquiredBadgeIds.contains(badge.getId());
                    UserBadge ub = userBadgeMap.get(badge.getId());
                    return BadgeDto.Response.from(
                            badge,
                            acquired,
                            acquired ? ub.getAcquiredAt() : null,
                            acquired ? ub.getIsDisplayed() : null
                    );
                })
                .collect(Collectors.toList());
    }

    // 카테고리별 배지 목록 조회
    public List<BadgeDto.Response> getBadgesByCategory(Long userId, BadgeCategory category) {
        List<Badge> badges = badgeRepository.findByCategoryAndActiveOrderBySortOrderAsc(category, true);
        Set<Long> acquiredBadgeIds = userBadgeRepository.findBadgeIdsByUserId(userId);

        Map<Long, UserBadge> userBadgeMap = new HashMap<>();
        userBadgeRepository.findByUserIdWithBadge(userId)
                .forEach(ub -> userBadgeMap.put(ub.getBadge().getId(), ub));

        return badges.stream()
                .map(badge -> {
                    boolean acquired = acquiredBadgeIds.contains(badge.getId());
                    UserBadge ub = userBadgeMap.get(badge.getId());
                    return BadgeDto.Response.from(
                            badge,
                            acquired,
                            acquired ? ub.getAcquiredAt() : null,
                            acquired ? ub.getIsDisplayed() : null
                    );
                })
                .collect(Collectors.toList());
    }

    // 사용자가 획득한 배지 목록
    public List<BadgeDto.Response> getAcquiredBadges(Long userId) {
        return userBadgeRepository.findByUserIdWithBadge(userId).stream()
                .map(BadgeDto.Response::from)
                .collect(Collectors.toList());
    }

    // 사용자의 표시 중인 배지 목록
    public List<BadgeDto.SimpleResponse> getDisplayedBadges(Long userId) {
        return userBadgeRepository.findDisplayedBadgesByUserId(userId).stream()
                .map(ub -> BadgeDto.SimpleResponse.from(ub.getBadge()))
                .collect(Collectors.toList());
    }

    // 배지 표시 여부 토글
    @Transactional
    public void toggleBadgeDisplay(Long userId, Long badgeId, boolean display) {
        UserBadge userBadge = userBadgeRepository.findByUserIdAndBadgeId(userId, badgeId)
                .orElseThrow(() -> new CustomException("획득하지 않은 배지입니다", HttpStatus.BAD_REQUEST));

        userBadge.setDisplayed(display);
    }

    // 배지 획득 체크 및 지급 (점수 기반)
    @Transactional
    public List<BadgeDto.AcquiredNotification> checkAndAwardScoreBadges(Long userId, int currentScore) {
        return checkAndAwardBadges(userId, BadgeConditionType.SCORE, currentScore);
    }

    // 배지 획득 체크 및 지급 (리뷰 수 기반)
    @Transactional
    public List<BadgeDto.AcquiredNotification> checkAndAwardReviewBadges(Long userId, int reviewCount) {
        List<BadgeDto.AcquiredNotification> acquired = new ArrayList<>();

        // 첫 리뷰 체크
        if (reviewCount >= 1) {
            acquired.addAll(checkAndAwardBadges(userId, BadgeConditionType.FIRST_REVIEW, 1));
        }

        // 리뷰 수 기반 배지
        acquired.addAll(checkAndAwardBadges(userId, BadgeConditionType.REVIEW_COUNT, reviewCount));

        return acquired;
    }

    // 배지 획득 체크 및 지급 (팔로워 수 기반)
    @Transactional
    public List<BadgeDto.AcquiredNotification> checkAndAwardFollowerBadges(Long userId, int followerCount) {
        List<BadgeDto.AcquiredNotification> acquired = new ArrayList<>();

        // 첫 팔로워 체크
        if (followerCount >= 1) {
            acquired.addAll(checkAndAwardBadges(userId, BadgeConditionType.FIRST_FOLLOWER, 1));
        }

        // 팔로워 수 기반 배지
        acquired.addAll(checkAndAwardBadges(userId, BadgeConditionType.FOLLOWER_COUNT, followerCount));

        return acquired;
    }

    // 배지 획득 체크 및 지급 (받은 공감 수 기반)
    @Transactional
    public List<BadgeDto.AcquiredNotification> checkAndAwardSympathyBadges(Long userId, int sympathyCount) {
        return checkAndAwardBadges(userId, BadgeConditionType.RECEIVED_SYMPATHY, sympathyCount);
    }

    // 공통 배지 획득 로직
    @Transactional
    public List<BadgeDto.AcquiredNotification> checkAndAwardBadges(Long userId, BadgeConditionType conditionType, int value) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException("사용자를 찾을 수 없습니다", HttpStatus.NOT_FOUND));

        // 조건을 만족하는 배지 목록
        List<Badge> eligibleBadges = badgeRepository.findEligibleBadges(conditionType, value);

        // 이미 획득한 배지 ID 목록
        Set<Long> acquiredBadgeIds = userBadgeRepository.findBadgeIdsByUserId(userId);

        List<BadgeDto.AcquiredNotification> newlyAcquired = new ArrayList<>();

        for (Badge badge : eligibleBadges) {
            if (!acquiredBadgeIds.contains(badge.getId())) {
                // 새 배지 지급
                UserBadge userBadge = UserBadge.create(user, badge);
                userBadgeRepository.save(userBadge);

                newlyAcquired.add(BadgeDto.AcquiredNotification.from(badge));
                log.info("Badge awarded: user={}, badge={}", userId, badge.getCode());
            }
        }

        return newlyAcquired;
    }

    // 기존 사용자들에게 배지 일괄 지급 (마이그레이션용)
    @Transactional
    public void migrateExistingUserBadges() {
        List<User> allUsers = userRepository.findAll();

        for (User user : allUsers) {
            if (user.isDeleted()) continue;

            // 점수 기반 배지 체크
            checkAndAwardBadges(user.getId(), BadgeConditionType.SCORE, user.getTasteScore());

            // 리뷰 수 기반 배지 체크
            checkAndAwardReviewBadges(user.getId(), user.getReviewCount());
        }

        log.info("Badge migration completed for {} users", allUsers.size());
    }
}
