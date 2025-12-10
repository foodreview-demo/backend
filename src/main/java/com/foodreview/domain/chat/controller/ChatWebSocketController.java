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
            // 메시지 저장
            ChatDto.MessageResponse response = chatService.sendMessageByUuid(userId, roomUuid, message.getContent());

            // 해당 채팅방을 구독 중인 모든 클라이언트에게 브로드캐스트
            messagingTemplate.convertAndSend("/topic/chat/" + roomUuid, response);

            log.info("메시지 브로드캐스트 완료: roomUuid={}", roomUuid);
        } catch (Exception e) {
            log.error("메시지 전송 실패: roomUuid={}, error={}", roomUuid, e.getMessage());
        }
    }
}
