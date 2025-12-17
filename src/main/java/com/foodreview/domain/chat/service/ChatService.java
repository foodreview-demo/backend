package com.foodreview.domain.chat.service;

import com.foodreview.domain.chat.dto.ChatDto;
import com.foodreview.domain.chat.entity.ChatMessage;
import com.foodreview.domain.chat.entity.ChatRoom;
import com.foodreview.domain.chat.entity.ChatRoomMember;
import com.foodreview.domain.chat.entity.MessageReadStatus;
import com.foodreview.domain.chat.repository.ChatMessageRepository;
import com.foodreview.domain.chat.repository.ChatRoomMemberRepository;
import com.foodreview.domain.chat.repository.ChatRoomRepository;
import com.foodreview.domain.chat.repository.MessageReadStatusRepository;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final MessageReadStatusRepository messageReadStatusRepository;
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

                    long unreadCount;
                    if (room.getRoomType() == ChatRoom.RoomType.GROUP) {
                        // 단체톡: MessageReadStatus 테이블 기준
                        unreadCount = chatMessageRepository.countUnreadMessagesByUser(room, user);
                    } else {
                        // 1:1 채팅: 기존 isRead 필드 기준
                        unreadCount = chatMessageRepository.countUnreadMessages(room, user);
                    }

                    if (room.getRoomType() == ChatRoom.RoomType.GROUP) {
                        List<ChatRoomMember> members = chatRoomMemberRepository.findByChatRoom(room);
                        return ChatDto.RoomResponse.fromGroup(room, members, unreadCount);
                    }
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

        Page<ChatMessage> messages = chatMessageRepository.findByChatRoomOrderByCreatedAtAsc(room, pageable);

        if (room.getRoomType() == ChatRoom.RoomType.GROUP) {
            // 단체톡: MessageReadStatus 사용
            // 읽지 않은 메시지들을 읽음 처리
            List<ChatMessage> unreadMessages = chatMessageRepository.findUnreadMessagesByUser(room, user);
            for (ChatMessage msg : unreadMessages) {
                if (!messageReadStatusRepository.existsByMessageAndUser(msg, user)) {
                    MessageReadStatus readStatus = MessageReadStatus.builder()
                            .message(msg)
                            .user(user)
                            .build();
                    messageReadStatusRepository.save(readStatus);
                }
            }

            // 각 메시지의 읽음 수 조회
            List<ChatMessage> messageList = messages.getContent();
            Map<Long, Long> readCountMap = getReadCountMap(messageList);
            int memberCount = room.getMembers().size() - 1;  // 본인 제외

            List<ChatDto.MessageResponse> content = messageList.stream()
                    .map(msg -> {
                        int readCount = readCountMap.getOrDefault(msg.getId(), 0L).intValue();
                        return ChatDto.MessageResponse.fromWithReadCount(msg, userId, readCount, memberCount);
                    })
                    .toList();

            return PageResponse.from(messages, content);
        } else {
            // 1:1 채팅: 기존 isRead 필드 사용
            chatMessageRepository.markAllAsRead(room, user);

            List<ChatDto.MessageResponse> content = messages.getContent().stream()
                    .map(msg -> ChatDto.MessageResponse.from(msg, userId))
                    .toList();

            return PageResponse.from(messages, content);
        }
    }

    // 여러 메시지의 읽음 수를 Map으로 반환
    private Map<Long, Long> getReadCountMap(List<ChatMessage> messages) {
        if (messages.isEmpty()) {
            return new HashMap<>();
        }
        List<Object[]> results = messageReadStatusRepository.countByMessages(messages);
        Map<Long, Long> map = new HashMap<>();
        for (Object[] row : results) {
            Long messageId = (Long) row[0];
            Long count = (Long) row[1];
            map.put(messageId, count);
        }
        return map;
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

    // 메시지 전송 (UUID 기반) - 수신자 ID 목록도 함께 반환
    @Transactional
    public SendMessageResult sendMessageByUuidWithRecipient(Long userId, String roomUuid, String content) {
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

        // 수신자 ID 목록 조회 (나 제외)
        List<Long> recipientIds = room.getAllMemberIds().stream()
                .filter(id -> !id.equals(userId))
                .toList();

        return new SendMessageResult(ChatDto.MessageResponse.from(saved, userId), recipientIds);
    }

    @lombok.Getter
    @lombok.AllArgsConstructor
    public static class SendMessageResult {
        private ChatDto.MessageResponse messageResponse;
        private List<Long> recipientIds;  // 단체톡 지원을 위해 List로 변경
    }

    // 메시지 읽음 처리 및 다른 멤버 ID 목록 반환 (WebSocket용)
    @Transactional
    public MarkAsReadResult markMessagesAsReadAndGetOtherUsers(Long userId, String roomUuid) {
        User user = findUserById(userId);
        ChatRoom room = findChatRoomByUuid(roomUuid);

        // 채팅방 참여자인지 확인
        validateChatRoomParticipant(room, userId);

        Long lastReadMessageId = null;
        Integer lastReadCount = null;

        if (room.getRoomType() == ChatRoom.RoomType.GROUP) {
            // 단체톡: MessageReadStatus 사용
            List<ChatMessage> unreadMessages = chatMessageRepository.findUnreadMessagesByUser(room, user);
            ChatMessage lastMessage = null;
            for (ChatMessage msg : unreadMessages) {
                if (!messageReadStatusRepository.existsByMessageAndUser(msg, user)) {
                    MessageReadStatus readStatus = MessageReadStatus.builder()
                            .message(msg)
                            .user(user)
                            .build();
                    messageReadStatusRepository.save(readStatus);
                    lastMessage = msg;
                }
            }
            // 마지막으로 읽은 메시지 정보
            if (lastMessage != null) {
                lastReadMessageId = lastMessage.getId();
                lastReadCount = (int) messageReadStatusRepository.countByMessage(lastMessage);
            }
        } else {
            // 1:1 채팅: 기존 isRead 필드 사용
            chatMessageRepository.markAllAsRead(room, user);
        }

        // 다른 멤버 ID 목록 반환 (나 제외)
        List<Long> otherUserIds = room.getAllMemberIds().stream()
                .filter(id -> !id.equals(userId))
                .toList();

        return new MarkAsReadResult(otherUserIds, lastReadMessageId, lastReadCount);
    }

    @lombok.Getter
    @lombok.AllArgsConstructor
    public static class MarkAsReadResult {
        private List<Long> otherUserIds;
        private Long lastReadMessageId;    // 단체톡용: 마지막으로 읽은 메시지 ID
        private Integer lastReadCount;     // 단체톡용: 해당 메시지를 읽은 총 인원
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

        if (room.getRoomType() == ChatRoom.RoomType.DIRECT) {
            // 1:1 채팅방은 바로 삭제
            chatMessageRepository.deleteByChatRoom(room);
            chatRoomRepository.delete(room);
        } else {
            // 단체톡은 멤버만 제거
            User user = findUserById(userId);
            ChatRoomMember member = chatRoomMemberRepository.findByChatRoomAndUser(room, user)
                    .orElseThrow(() -> new CustomException("채팅방 멤버가 아닙니다", HttpStatus.BAD_REQUEST));

            // 퇴장 시스템 메시지 생성
            createSystemMessage(room, user.getName() + "님이 퇴장했습니다.");

            room.removeMember(member);
            chatRoomMemberRepository.delete(member);

            // 마지막 멤버가 나가면 채팅방 삭제
            if (room.getMembers().isEmpty()) {
                chatMessageRepository.deleteByChatRoom(room);
                chatRoomRepository.delete(room);
            } else if (member.getRole() == ChatRoomMember.MemberRole.OWNER) {
                // 방장이 나가면 가장 먼저 들어온 멤버에게 방장 위임
                ChatRoomMember newOwner = room.getMembers().stream()
                        .min((m1, m2) -> m1.getJoinedAt().compareTo(m2.getJoinedAt()))
                        .orElseThrow();
                newOwner.promoteToOwner();
            }
        }
    }

    // ====== 단체톡 관련 메서드 ======

    // 단체톡방 생성
    @Transactional
    public ChatDto.RoomResponse createGroupChatRoom(Long userId, String name, List<Long> memberIds) {
        User owner = findUserById(userId);

        // 자기 자신이 포함되어 있으면 제외
        memberIds = memberIds.stream().filter(id -> !id.equals(userId)).toList();

        if (memberIds.size() < 2) {
            throw new CustomException("단체톡은 최소 2명 이상이어야 합니다", HttpStatus.BAD_REQUEST);
        }

        // 첫 번째 멤버 조회 (user2용)
        User firstMember = findUserById(memberIds.get(0));

        // 단체톡방 생성 (DB NOT NULL 제약조건 때문에 user1, user2 설정)
        ChatRoom room = ChatRoom.builder()
                .roomType(ChatRoom.RoomType.GROUP)
                .name(name != null ? name : generateGroupName(owner, memberIds))
                .user1(owner)       // 방장
                .user2(firstMember) // 첫 번째 멤버 (DB 제약조건용)
                .build();
        chatRoomRepository.save(room);

        // 방장 추가
        ChatRoomMember ownerMember = ChatRoomMember.builder()
                .chatRoom(room)
                .user(owner)
                .role(ChatRoomMember.MemberRole.OWNER)
                .build();
        room.addMember(ownerMember);
        chatRoomMemberRepository.save(ownerMember);

        // 멤버 추가
        StringBuilder memberNames = new StringBuilder();
        for (int i = 0; i < memberIds.size(); i++) {
            Long memberId = memberIds.get(i);
            User memberUser = findUserById(memberId);
            ChatRoomMember member = ChatRoomMember.builder()
                    .chatRoom(room)
                    .user(memberUser)
                    .role(ChatRoomMember.MemberRole.MEMBER)
                    .build();
            room.addMember(member);
            chatRoomMemberRepository.save(member);

            if (i > 0) memberNames.append(", ");
            memberNames.append(memberUser.getName());
        }

        // 단체톡방 생성 시스템 메시지
        createSystemMessage(room, owner.getName() + "님이 " + memberNames + "님을 초대하여 대화를 시작했습니다.");

        List<ChatRoomMember> members = chatRoomMemberRepository.findByChatRoom(room);
        return ChatDto.RoomResponse.fromGroup(room, members, 0L);
    }

    // 채팅방에 사용자 초대
    @Transactional
    public ChatDto.RoomResponse inviteToRoom(Long userId, String roomUuid, List<Long> userIds) {
        ChatRoom room = findChatRoomByUuid(roomUuid);
        User inviter = findUserById(userId);

        // 채팅방 참여자인지 확인
        validateChatRoomParticipant(room, userId);

        if (room.getRoomType() == ChatRoom.RoomType.DIRECT) {
            throw new CustomException("1:1 채팅방에는 초대할 수 없습니다", HttpStatus.BAD_REQUEST);
        }

        StringBuilder invitedNames = new StringBuilder();
        int inviteCount = 0;

        for (Long inviteUserId : userIds) {
            // 이미 멤버인지 확인
            User inviteUser = findUserById(inviteUserId);
            if (chatRoomMemberRepository.existsByChatRoomAndUser(room, inviteUser)) {
                continue;  // 이미 멤버면 스킵
            }

            ChatRoomMember member = ChatRoomMember.builder()
                    .chatRoom(room)
                    .user(inviteUser)
                    .role(ChatRoomMember.MemberRole.MEMBER)
                    .build();
            room.addMember(member);
            chatRoomMemberRepository.save(member);

            if (inviteCount > 0) invitedNames.append(", ");
            invitedNames.append(inviteUser.getName());
            inviteCount++;
        }

        // 초대 시스템 메시지 생성
        if (inviteCount > 0) {
            createSystemMessage(room, inviter.getName() + "님이 " + invitedNames + "님을 초대했습니다.");
        }

        List<ChatRoomMember> members = chatRoomMemberRepository.findByChatRoom(room);
        long unreadCount = chatMessageRepository.countUnreadMessages(room, inviter);
        return ChatDto.RoomResponse.fromGroup(room, members, unreadCount);
    }

    // 채팅방 이름 변경
    @Transactional
    public ChatDto.RoomResponse updateRoomName(Long userId, String roomUuid, String name) {
        ChatRoom room = findChatRoomByUuid(roomUuid);

        // 채팅방 참여자인지 확인
        validateChatRoomParticipant(room, userId);

        if (room.getRoomType() == ChatRoom.RoomType.DIRECT) {
            throw new CustomException("1:1 채팅방은 이름을 변경할 수 없습니다", HttpStatus.BAD_REQUEST);
        }

        room.updateName(name);

        List<ChatRoomMember> members = chatRoomMemberRepository.findByChatRoom(room);
        long unreadCount = chatMessageRepository.countUnreadMessages(room, findUserById(userId));
        return ChatDto.RoomResponse.fromGroup(room, members, unreadCount);
    }

    // 채팅방 멤버 목록 조회
    public List<ChatDto.MemberResponse> getRoomMembers(Long userId, String roomUuid) {
        ChatRoom room = findChatRoomByUuid(roomUuid);

        // 채팅방 참여자인지 확인
        validateChatRoomParticipant(room, userId);

        if (room.getRoomType() == ChatRoom.RoomType.DIRECT) {
            throw new CustomException("1:1 채팅방은 멤버 목록을 조회할 수 없습니다", HttpStatus.BAD_REQUEST);
        }

        return chatRoomMemberRepository.findByChatRoom(room).stream()
                .map(ChatDto.MemberResponse::from)
                .toList();
    }

    // 그룹 이름 자동 생성 (멤버 이름 나열)
    private String generateGroupName(User owner, List<Long> memberIds) {
        StringBuilder sb = new StringBuilder();
        sb.append(owner.getName());
        int count = 0;
        for (Long memberId : memberIds) {
            if (count >= 2) {  // 최대 3명까지만 표시
                sb.append(" 외 ").append(memberIds.size() - 2).append("명");
                break;
            }
            User member = findUserById(memberId);
            sb.append(", ").append(member.getName());
            count++;
        }
        return sb.toString();
    }

    private void validateChatRoomParticipant(ChatRoom room, Long userId) {
        if (!room.isMember(userId)) {
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

    // 시스템 메시지 생성 (입장/퇴장/초대 등)
    private void createSystemMessage(ChatRoom room, String content) {
        ChatMessage message = ChatMessage.builder()
                .chatRoom(room)
                .sender(null)  // 시스템 메시지는 sender 없음
                .content(content)
                .messageType(ChatMessage.MessageType.SYSTEM)
                .isRead(true)  // 시스템 메시지는 항상 읽음 처리
                .build();
        chatMessageRepository.save(message);
        room.updateLastMessage(content);
    }
}
