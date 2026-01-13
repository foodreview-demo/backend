package com.foodreview.domain.notification.service;

import com.foodreview.domain.notification.dto.FcmTokenDto;
import com.foodreview.domain.notification.entity.FcmToken;
import com.foodreview.domain.notification.repository.FcmTokenRepository;
import com.foodreview.domain.user.entity.User;
import com.foodreview.domain.user.repository.UserRepository;
import com.foodreview.global.exception.CustomException;
import com.google.firebase.messaging.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class FcmService {

    private final FcmTokenRepository fcmTokenRepository;
    private final UserRepository userRepository;
    private final FirebaseMessaging firebaseMessaging;

    public FcmService(FcmTokenRepository fcmTokenRepository,
                      UserRepository userRepository,
                      @Autowired(required = false) FirebaseMessaging firebaseMessaging) {
        this.fcmTokenRepository = fcmTokenRepository;
        this.userRepository = userRepository;
        this.firebaseMessaging = firebaseMessaging;
    }

    /**
     * FCM 토큰 등록/업데이트
     */
    @Transactional
    public void registerToken(Long userId, FcmTokenDto.RegisterRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException("사용자를 찾을 수 없습니다", HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));

        Optional<FcmToken> existingToken = fcmTokenRepository.findByToken(request.getToken());

        if (existingToken.isPresent()) {
            FcmToken token = existingToken.get();
            // 다른 사용자의 토큰이면 해당 사용자에서 제거하고 현재 사용자에게 할당
            if (!token.getUser().getId().equals(userId)) {
                fcmTokenRepository.delete(token);
                createNewToken(user, request);
            } else {
                // 같은 사용자의 토큰이면 활성화
                token.activate();
            }
        } else {
            createNewToken(user, request);
        }

        log.info("FCM token registered for user: {}", userId);
    }

    private void createNewToken(User user, FcmTokenDto.RegisterRequest request) {
        FcmToken fcmToken = FcmToken.builder()
                .user(user)
                .token(request.getToken())
                .deviceType(request.getDeviceType())
                .deviceId(request.getDeviceId())
                .build();
        fcmTokenRepository.save(fcmToken);
    }

    /**
     * FCM 토큰 해제
     */
    @Transactional
    public void unregisterToken(String token) {
        fcmTokenRepository.deactivateByToken(token);
        log.info("FCM token unregistered: {}", token.substring(0, 20) + "...");
    }

    /**
     * 특정 사용자에게 푸시 알림 전송
     */
    public void sendToUser(Long userId, String title, String body, String clickAction) {
        log.info("sendToUser called: userId={}, title={}", userId, title);

        if (firebaseMessaging == null) {
            log.warn("FirebaseMessaging not initialized. Skipping push notification.");
            return;
        }

        List<String> tokens = fcmTokenRepository.findActiveTokensByUserId(userId);
        log.info("Found {} active FCM tokens for user: {}", tokens.size(), userId);

        if (tokens.isEmpty()) {
            return;
        }

        sendMulticast(tokens, title, body, clickAction);
    }

    /**
     * 여러 사용자에게 푸시 알림 전송
     */
    public void sendToUsers(List<Long> userIds, String title, String body, String clickAction) {
        if (firebaseMessaging == null) {
            log.warn("FirebaseMessaging not initialized. Skipping push notification.");
            return;
        }

        List<String> tokens = fcmTokenRepository.findActiveTokensByUserIds(userIds);

        if (tokens.isEmpty()) {
            log.debug("No active FCM tokens for users: {}", userIds);
            return;
        }

        sendMulticast(tokens, title, body, clickAction);
    }

    /**
     * 여러 토큰에 멀티캐스트 전송
     */
    private void sendMulticast(List<String> tokens, String title, String body, String clickAction) {
        if (tokens.size() > 500) {
            // FCM은 한 번에 500개까지만 전송 가능
            for (int i = 0; i < tokens.size(); i += 500) {
                List<String> batch = tokens.subList(i, Math.min(i + 500, tokens.size()));
                sendBatch(batch, title, body, clickAction);
            }
        } else {
            sendBatch(tokens, title, body, clickAction);
        }
    }

    private void sendBatch(List<String> tokens, String title, String body, String clickAction) {
        Notification notification = Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build();

        MulticastMessage message = MulticastMessage.builder()
                .setNotification(notification)
                .putData("click_action", clickAction != null ? clickAction : "/")
                .addAllTokens(tokens)
                .setAndroidConfig(AndroidConfig.builder()
                        .setNotification(AndroidNotification.builder()
                                .setSound("default")
                                .setClickAction("FLUTTER_NOTIFICATION_CLICK")
                                .build())
                        .build())
                .setApnsConfig(ApnsConfig.builder()
                        .setAps(Aps.builder()
                                .setSound("default")
                                .build())
                        .build())
                .build();

        try {
            BatchResponse response = firebaseMessaging.sendEachForMulticast(message);
            log.info("FCM multicast sent. Success: {}, Failure: {}",
                    response.getSuccessCount(), response.getFailureCount());

            // 실패한 토큰 처리 (토큰이 유효하지 않으면 비활성화)
            handleFailedTokens(tokens, response);
        } catch (FirebaseMessagingException e) {
            log.error("Failed to send FCM multicast: {}", e.getMessage());
        }
    }

    /**
     * 실패한 토큰 비활성화 처리
     */
    private void handleFailedTokens(List<String> tokens, BatchResponse response) {
        List<SendResponse> responses = response.getResponses();
        for (int i = 0; i < responses.size(); i++) {
            SendResponse sendResponse = responses.get(i);
            if (!sendResponse.isSuccessful()) {
                FirebaseMessagingException exception = sendResponse.getException();
                if (exception != null) {
                    MessagingErrorCode errorCode = exception.getMessagingErrorCode();
                    if (errorCode == MessagingErrorCode.UNREGISTERED ||
                        errorCode == MessagingErrorCode.INVALID_ARGUMENT) {
                        String failedToken = tokens.get(i);
                        fcmTokenRepository.deactivateByToken(failedToken);
                        log.info("Deactivated invalid FCM token: {}", failedToken.substring(0, 20) + "...");
                    }
                }
            }
        }
    }

    /**
     * 회원 탈퇴 시 FCM 토큰 삭제
     */
    @Transactional
    public void deleteUserTokens(User user) {
        fcmTokenRepository.deleteByUser(user);
        log.info("Deleted all FCM tokens for user: {}", user.getId());
    }
}
