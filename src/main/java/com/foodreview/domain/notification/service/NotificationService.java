package com.foodreview.domain.notification.service;

import com.foodreview.domain.notification.dto.NotificationDto;
import com.foodreview.domain.notification.entity.Notification;
import com.foodreview.domain.notification.repository.NotificationRepository;
import com.foodreview.domain.user.entity.User;
import com.foodreview.domain.user.repository.UserRepository;
import com.foodreview.global.common.PageResponse;
import com.foodreview.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final FcmService fcmService;

    /**
     * 알림 생성
     */
    @Transactional
    public void createNotification(User recipient, User actor, Notification.NotificationType type,
                                   String message, Long referenceId) {
        // 자기 자신에게는 알림을 보내지 않음
        if (recipient.getId().equals(actor.getId())) {
            return;
        }

        // 알림 타입에 따른 설정 확인
        if (!shouldSendNotification(recipient, type)) {
            log.debug("Notification disabled for user: {}, type: {}", recipient.getId(), type);
            return;
        }

        Notification notification = Notification.builder()
                .user(recipient)
                .actor(actor)
                .type(type)
                .message(message)
                .referenceId(referenceId)
                .build();

        notificationRepository.save(notification);
        log.debug("Notification created: type={}, recipient={}, actor={}", type, recipient.getId(), actor.getId());

        // 푸시 알림 전송 (비동기)
        sendPushNotification(recipient.getId(), type, message, referenceId);
    }

    /**
     * 사용자 설정에 따라 알림을 보낼지 확인
     */
    private boolean shouldSendNotification(User recipient, Notification.NotificationType type) {
        return switch (type) {
            case SYMPATHY, COMMENT -> recipient.getNotifyReviews();
            case FOLLOW -> recipient.getNotifyFollows();
            case CHAT -> recipient.getNotifyMessages();
            default -> true;
        };
    }

    /**
     * 푸시 알림 전송 (비동기)
     */
    @Async
    public void sendPushNotification(Long userId, Notification.NotificationType type, String message, Long referenceId) {
        String title = getPushTitle(type);
        String clickAction = getClickAction(type, referenceId);
        fcmService.sendToUser(userId, title, message, clickAction);
    }

    private String getPushTitle(Notification.NotificationType type) {
        return switch (type) {
            case SYMPATHY -> "새로운 공감";
            case COMMENT -> "새 댓글";
            case FOLLOW -> "새 팔로워";
            case CHAT -> "새 메시지";
            case REFERENCE -> "리뷰 참고";
            default -> "맛잘알";
        };
    }

    private String getClickAction(Notification.NotificationType type, Long referenceId) {
        return switch (type) {
            case SYMPATHY, COMMENT, REFERENCE -> "/reviews/" + referenceId;
            case FOLLOW -> "/profile/" + referenceId;
            case CHAT -> "/chat";
            default -> "/";
        };
    }

    /**
     * 알림 목록 조회
     */
    public PageResponse<NotificationDto.Response> getNotifications(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException("사용자를 찾을 수 없습니다", HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));

        Page<Notification> notifications = notificationRepository.findByUserOrderByCreatedAtDesc(user, pageable);

        List<NotificationDto.Response> content = notifications.getContent().stream()
                .map(NotificationDto.Response::from)
                .toList();

        return PageResponse.from(notifications, content);
    }

    /**
     * 읽지 않은 알림 수 조회
     */
    public long getUnreadCount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException("사용자를 찾을 수 없습니다", HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));

        return notificationRepository.countByUserAndIsReadFalse(user);
    }

    /**
     * 알림 읽음 처리
     */
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new CustomException("알림을 찾을 수 없습니다", HttpStatus.NOT_FOUND, "NOTIFICATION_NOT_FOUND"));

        if (!notification.getUser().getId().equals(userId)) {
            throw new CustomException("알림에 접근할 권한이 없습니다", HttpStatus.FORBIDDEN, "FORBIDDEN");
        }

        notification.markAsRead();
    }

    /**
     * 모든 알림 읽음 처리
     */
    @Transactional
    public void markAllAsRead(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException("사용자를 찾을 수 없습니다", HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));

        notificationRepository.markAllAsRead(user);
    }
}
