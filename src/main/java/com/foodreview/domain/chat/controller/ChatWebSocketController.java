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
            // 메시지 저장 및 상대방 ID 조회
            ChatService.SendMessageResult result = chatService.sendMessageByUuidWithRecipient(userId, roomUuid, message.getContent());

            // 해당 채팅방을 구독 중인 모든 클라이언트에게 브로드캐스트
            messagingTemplate.convertAndSend("/topic/chat/" + roomUuid, result.getMessageResponse());

            // 상대방에게 새 메시지 알림 전송 (채팅방 목록 갱신용)
            messagingTemplate.convertAndSend("/topic/user/" + result.getRecipientId() + "/notification",
                    new ChatDto.NewMessageNotification(roomUuid, result.getMessageResponse()));

            log.info("메시지 브로드캐스트 완료: roomUuid={}, recipientId={}", roomUuid, result.getRecipientId());
        } catch (Exception e) {
            log.error("메시지 전송 실패: roomUuid={}, error={}", roomUuid, e.getMessage());
        }
    }

    /**
     * 클라이언트가 /app/chat/{roomUuid}/read 로 읽음 알림을 보내면
     * 상대방에게 읽음 상태 알림 전송
     */
    @MessageMapping("/chat/{roomUuid}/read")
    public void markAsRead(
            @DestinationVariable String roomUuid,
            @Header("userId") Long userId) {

        log.info("읽음 알림 수신: roomUuid={}, userId={}", roomUuid, userId);

        try {
            // 메시지 읽음 처리 및 상대방 ID 조회
            Long otherUserId = chatService.markMessagesAsReadAndGetOtherUser(userId, roomUuid);

            // 상대방에게 읽음 상태 알림 전송
            messagingTemplate.convertAndSend("/topic/chat/" + roomUuid + "/read",
                    new ChatDto.ReadNotification(roomUuid, userId));

            log.info("읽음 알림 전송 완료: roomUuid={}, readByUserId={}", roomUuid, userId);
        } catch (Exception e) {
            log.error("읽음 알림 전송 실패: roomUuid={}, error={}", roomUuid, e.getMessage());
        }
    }
}
