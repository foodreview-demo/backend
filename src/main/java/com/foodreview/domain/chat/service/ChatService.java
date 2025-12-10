package com.foodreview.domain.chat.service;

import com.foodreview.domain.chat.dto.ChatDto;
import com.foodreview.domain.chat.entity.ChatMessage;
import com.foodreview.domain.chat.entity.ChatRoom;
import com.foodreview.domain.chat.repository.ChatMessageRepository;
import com.foodreview.domain.chat.repository.ChatRoomRepository;
import com.foodreview.domain.user.entity.User;
import com.foodreview.domain.user.repository.UserRepository;
import com.foodreview.global.common.PageResponse;
import com.foodreview.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    // 채팅방 목록 조회
    @Transactional
    public PageResponse<ChatDto.RoomResponse> getChatRooms(Long userId, Pageable pageable) {
        User user = findUserById(userId);
        Page<ChatRoom> rooms = chatRoomRepository.findByUser(user, pageable);

        List<ChatDto.RoomResponse> content = rooms.getContent().stream()
                .map(room -> {
                    // 기존 채팅방에 UUID가 없으면 생성
                    room.ensureUuid();
                    long unreadCount = chatMessageRepository.countUnreadMessages(room, user);
                    return ChatDto.RoomResponse.from(room, user, unreadCount);
                })
                .toList();

        return PageResponse.from(rooms, content);
    }

    // 채팅방 생성 또는 조회
    @Transactional
    public ChatDto.RoomResponse getOrCreateChatRoom(Long userId, Long otherUserId) {
        if (userId.equals(otherUserId)) {
            throw new CustomException("자기 자신과 채팅할 수 없습니다", HttpStatus.BAD_REQUEST);
        }

        User user = findUserById(userId);
        User otherUser = findUserById(otherUserId);

        Optional<ChatRoom> existingRoom = chatRoomRepository.findByUsers(user, otherUser);

        ChatRoom room = existingRoom.orElseGet(() -> {
            ChatRoom newRoom = ChatRoom.builder()
                    .user1(user)
                    .user2(otherUser)
                    .build();
            return chatRoomRepository.save(newRoom);
        });

        // 기존 채팅방에 UUID가 없으면 생성
        room.ensureUuid();

        long unreadCount = chatMessageRepository.countUnreadMessages(room, user);
        return ChatDto.RoomResponse.from(room, user, unreadCount);
    }

    // 채팅 메시지 조회 (roomId 기반 - deprecated)
    @Transactional
    public PageResponse<ChatDto.MessageResponse> getMessages(Long userId, Long roomId, Pageable pageable) {
        User user = findUserById(userId);
        ChatRoom room = findChatRoomById(roomId);

        // 채팅방 참여자인지 확인
        validateChatRoomParticipant(room, userId);

        // 메시지 읽음 처리
        chatMessageRepository.markAllAsRead(room, user);

        Page<ChatMessage> messages = chatMessageRepository.findByChatRoomOrderByCreatedAtAsc(room, pageable);

        List<ChatDto.MessageResponse> content = messages.getContent().stream()
                .map(msg -> ChatDto.MessageResponse.from(msg, userId))
                .toList();

        return PageResponse.from(messages, content);
    }

    // 채팅 메시지 조회 (UUID 기반)
    @Transactional
    public PageResponse<ChatDto.MessageResponse> getMessagesByUuid(Long userId, String roomUuid, Pageable pageable) {
        User user = findUserById(userId);
        ChatRoom room = findChatRoomByUuid(roomUuid);

        // 채팅방 참여자인지 확인
        validateChatRoomParticipant(room, userId);

        // 메시지 읽음 처리
        chatMessageRepository.markAllAsRead(room, user);

        Page<ChatMessage> messages = chatMessageRepository.findByChatRoomOrderByCreatedAtAsc(room, pageable);

        List<ChatDto.MessageResponse> content = messages.getContent().stream()
                .map(msg -> ChatDto.MessageResponse.from(msg, userId))
                .toList();

        return PageResponse.from(messages, content);
    }

    // 메시지 전송 (roomId 기반 - deprecated)
    @Transactional
    public ChatDto.MessageResponse sendMessage(Long userId, Long roomId, String content) {
        User sender = findUserById(userId);
        ChatRoom room = findChatRoomById(roomId);

        // 채팅방 참여자인지 확인
        validateChatRoomParticipant(room, userId);

        ChatMessage message = ChatMessage.builder()
                .chatRoom(room)
                .sender(sender)
                .content(content)
                .build();

        ChatMessage saved = chatMessageRepository.save(message);

        // 채팅방 마지막 메시지 업데이트
        room.updateLastMessage(content);

        return ChatDto.MessageResponse.from(saved, userId);
    }

    // 메시지 전송 (UUID 기반)
    @Transactional
    public ChatDto.MessageResponse sendMessageByUuid(Long userId, String roomUuid, String content) {
        User sender = findUserById(userId);
        ChatRoom room = findChatRoomByUuid(roomUuid);

        // 채팅방 참여자인지 확인
        validateChatRoomParticipant(room, userId);

        ChatMessage message = ChatMessage.builder()
                .chatRoom(room)
                .sender(sender)
                .content(content)
                .build();

        ChatMessage saved = chatMessageRepository.save(message);

        // 채팅방 마지막 메시지 업데이트
        room.updateLastMessage(content);

        return ChatDto.MessageResponse.from(saved, userId);
    }

    // UUID로 채팅방 정보 조회
    public ChatDto.RoomResponse getChatRoomByUuid(Long userId, String roomUuid) {
        User user = findUserById(userId);
        ChatRoom room = findChatRoomByUuid(roomUuid);

        // 채팅방 참여자인지 확인
        validateChatRoomParticipant(room, userId);

        long unreadCount = chatMessageRepository.countUnreadMessages(room, user);
        return ChatDto.RoomResponse.from(room, user, unreadCount);
    }

    // 채팅방 나가기 (UUID 기반)
    @Transactional
    public void leaveChatRoom(Long userId, String roomUuid) {
        ChatRoom room = findChatRoomByUuid(roomUuid);

        // 채팅방 참여자인지 확인
        validateChatRoomParticipant(room, userId);

        // 채팅방의 모든 메시지 삭제
        chatMessageRepository.deleteByChatRoom(room);

        // 채팅방 삭제
        chatRoomRepository.delete(room);
    }

    private void validateChatRoomParticipant(ChatRoom room, Long userId) {
        if (!room.getUser1().getId().equals(userId) && !room.getUser2().getId().equals(userId)) {
            throw new CustomException("채팅방에 접근 권한이 없습니다", HttpStatus.FORBIDDEN, "FORBIDDEN");
        }
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException("사용자를 찾을 수 없습니다", HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));
    }

    private ChatRoom findChatRoomById(Long roomId) {
        return chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException("채팅방을 찾을 수 없습니다", HttpStatus.NOT_FOUND, "CHATROOM_NOT_FOUND"));
    }

    private ChatRoom findChatRoomByUuid(String uuid) {
        return chatRoomRepository.findByUuid(uuid)
                .orElseThrow(() -> new CustomException("채팅방을 찾을 수 없습니다", HttpStatus.NOT_FOUND, "CHATROOM_NOT_FOUND"));
    }
}
