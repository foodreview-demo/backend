package com.foodreview.domain.chat.controller;

import com.foodreview.domain.chat.dto.ChatDto;
import com.foodreview.domain.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 클라이언트가 /app/chat/{roomUuid} 로 메시지를 보내면
     * 해당 채팅방을 구독 중인 모든 클라이언트에게 브로드캐스트
     */
    @MessageMapping("/chat/{roomUuid}")
    public void sendMessage(
            @DestinationVariable String roomUuid,
            @Payload ChatDto.WebSocketMessage message,
            @Header("userId") Long userId) {

        log.info("WebSocket 메시지 수신: roomUuid={}, userId={}, content={}", roomUuid, userId, message.getContent());

        try {
            // 메시지 저장 및 수신자 ID 목록 조회
            ChatService.SendMessageResult result = chatService.sendMessageByUuidWithRecipient(userId, roomUuid, message.getContent());

            // 해당 채팅방을 구독 중인 모든 클라이언트에게 브로드캐스트
            messagingTemplate.convertAndSend("/topic/chat/" + roomUuid, result.getMessageResponse());

            // 모든 수신자에게 새 메시지 알림 전송 (채팅방 목록 갱신용)
            for (Long recipientId : result.getRecipientIds()) {
                messagingTemplate.convertAndSend("/topic/user/" + recipientId + "/notification",
                        new ChatDto.NewMessageNotification(roomUuid, result.getMessageResponse()));
            }

            log.info("메시지 브로드캐스트 완료: roomUuid={}, recipientIds={}", roomUuid, result.getRecipientIds());
        } catch (Exception e) {
            log.error("메시지 전송 실패: roomUuid={}, error={}", roomUuid, e.getMessage());
        }
    }

    /**
     * 클라이언트가 /app/chat/{roomUuid}/read 로 읽음 알림을 보내면
     * 다른 멤버들에게 읽음 상태 알림 전송
     */
    @MessageMapping("/chat/{roomUuid}/read")
    public void markAsRead(
            @DestinationVariable String roomUuid,
            @Header("userId") Long userId) {

        log.info("읽음 알림 수신: roomUuid={}, userId={}", roomUuid, userId);

        try {
            // 메시지 읽음 처리 및 다른 멤버 ID 목록 조회
            ChatService.MarkAsReadResult result = chatService.markMessagesAsReadAndGetOtherUsers(userId, roomUuid);

            // 채팅방 구독자 전체에게 읽음 상태 알림 전송 (단체톡 정보 포함)
            ChatDto.ReadNotification notification = new ChatDto.ReadNotification(
                    roomUuid,
                    userId,
                    result.getLastReadMessageId(),
                    result.getLastReadCount()
            );
            messagingTemplate.convertAndSend("/topic/chat/" + roomUuid + "/read", notification);

            log.info("읽음 알림 전송 완료: roomUuid={}, readByUserId={}, lastReadMessageId={}, readCount={}",
                    roomUuid, userId, result.getLastReadMessageId(), result.getLastReadCount());
        } catch (Exception e) {
            log.error("읽음 알림 전송 실패: roomUuid={}, error={}", roomUuid, e.getMessage());
        }
    }
}
