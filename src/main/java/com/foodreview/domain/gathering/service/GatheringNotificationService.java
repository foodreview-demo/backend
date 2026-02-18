package com.foodreview.domain.gathering.service;

import com.foodreview.domain.gathering.entity.Gathering;
import com.foodreview.domain.notification.entity.Notification;
import com.foodreview.domain.notification.service.NotificationService;
import com.foodreview.domain.review.repository.ReviewRepository;
import com.foodreview.domain.user.entity.User;
import com.foodreview.domain.user.repository.UserBlockRepository;
import com.foodreview.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GatheringNotificationService {

    private final NotificationService notificationService;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final UserBlockRepository userBlockRepository;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("M월 d일 HH:mm");

    /**
     * 번개모임 생성 시 알림 전송
     * - GATHERING_REVIEWED: 해당 음식점에 리뷰 남긴 사용자
     * - GATHERING_NEARBY: 같은 지역 사용자
     */
    @Async
    @Transactional
    public void sendGatheringCreatedNotifications(Gathering gathering, User creator) {
        Long restaurantId = gathering.getRestaurant().getId();
        String restaurantName = gathering.getRestaurant().getName();
        String region = gathering.getRestaurant().getRegion();
        String district = gathering.getRestaurant().getDistrict();

        // 호스트가 차단한 사용자 목록
        List<Long> blockedByCreator = userBlockRepository.findBlockedUserIdsByBlockerId(creator.getId());
        Set<Long> excludeUserIds = new HashSet<>(blockedByCreator);
        excludeUserIds.add(creator.getId()); // 본인 제외

        // 1. GATHERING_REVIEWED: 해당 음식점에 리뷰 남긴 사용자
        List<Long> reviewerIds = reviewRepository.findReviewerIdsByRestaurantId(restaurantId);
        Set<Long> notifiedUserIds = new HashSet<>();

        for (Long reviewerId : reviewerIds) {
            if (excludeUserIds.contains(reviewerId)) continue;

            User reviewer = userRepository.findById(reviewerId).orElse(null);
            if (reviewer == null || reviewer.isDeleted() || !reviewer.getNotifyGatherings()) continue;

            // 리뷰어가 호스트를 차단했는지 확인
            if (userBlockRepository.existsByBlockerIdAndBlockedUserId(reviewerId, creator.getId())) continue;

            String message = String.format("%s에서 번개모임이 열려요!", restaurantName);
            notificationService.createNotification(
                reviewer,
                creator,
                Notification.NotificationType.GATHERING_REVIEWED,
                message,
                gathering.getId()
            );
            notifiedUserIds.add(reviewerId);
        }

        // 2. GATHERING_NEARBY: 같은 지역 사용자 (이미 알림 받은 사용자 제외)
        List<User> nearbyUsers = userRepository.findByRegionAndDistrict(region, district);

        for (User nearbyUser : nearbyUsers) {
            if (excludeUserIds.contains(nearbyUser.getId())) continue;
            if (notifiedUserIds.contains(nearbyUser.getId())) continue; // 이미 GATHERING_REVIEWED로 알림 받음

            // 근처 사용자가 호스트를 차단했는지 확인
            if (userBlockRepository.existsByBlockerIdAndBlockedUserId(nearbyUser.getId(), creator.getId())) continue;

            String message = String.format("근처 %s에서 번개모임 모집 중!", restaurantName);
            notificationService.createNotification(
                nearbyUser,
                creator,
                Notification.NotificationType.GATHERING_NEARBY,
                message,
                gathering.getId()
            );
        }

        log.info("Gathering notifications sent: gathering={}, reviewed={}, nearby={}",
            gathering.getUuid(), notifiedUserIds.size(), nearbyUsers.size() - notifiedUserIds.size());
    }

    /**
     * 번개모임 리마인더 알림 (시작 1시간 전)
     */
    @Async
    @Transactional
    public void sendGatheringReminderNotification(User participant, Gathering gathering) {
        if (!participant.getNotifyGatherings()) return;

        String timeStr = gathering.getTargetTime().format(TIME_FORMATTER);
        String message = String.format("%s %s에 시작! 잊지 마세요", gathering.getTitle(), timeStr);

        notificationService.createNotification(
            participant,
            gathering.getCreator(),
            Notification.NotificationType.GATHERING_REMINDER,
            message,
            gathering.getId()
        );
    }
}
