package com.foodreview.domain.chat.repository;

import com.foodreview.domain.chat.entity.ChatMessage;
import com.foodreview.domain.chat.entity.ChatRoom;
import com.foodreview.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // 채팅방의 메시지 조회
    Page<ChatMessage> findByChatRoomOrderByCreatedAtAsc(ChatRoom chatRoom, Pageable pageable);

    // 채팅방의 읽지 않은 메시지 수 (내가 보낸 메시지 제외) - 1:1 채팅용
    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.chatRoom = :chatRoom AND m.sender != :user AND m.isRead = false")
    long countUnreadMessages(@Param("chatRoom") ChatRoom chatRoom, @Param("user") User user);

    // 채팅방의 메시지 모두 읽음 처리 - 1:1 채팅용
    @Modifying
    @Query("UPDATE ChatMessage m SET m.isRead = true WHERE m.chatRoom = :chatRoom AND m.sender != :user")
    void markAllAsRead(@Param("chatRoom") ChatRoom chatRoom, @Param("user") User user);

    // 채팅방의 모든 메시지 삭제
    @Modifying
    @Query("DELETE FROM ChatMessage m WHERE m.chatRoom = :chatRoom")
    void deleteByChatRoom(@Param("chatRoom") ChatRoom chatRoom);

    // 채팅방의 읽지 않은 메시지 조회 (단체톡용 - MessageReadStatus 테이블 기준)
    @Query("SELECT m FROM ChatMessage m WHERE m.chatRoom = :chatRoom AND m.sender != :user " +
           "AND NOT EXISTS (SELECT 1 FROM MessageReadStatus mrs WHERE mrs.message = m AND mrs.user = :user)")
    List<ChatMessage> findUnreadMessagesByUser(@Param("chatRoom") ChatRoom chatRoom, @Param("user") User user);

    // 채팅방의 읽지 않은 메시지 수 (단체톡용 - MessageReadStatus 테이블 기준)
    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.chatRoom = :chatRoom AND m.sender != :user " +
           "AND NOT EXISTS (SELECT 1 FROM MessageReadStatus mrs WHERE mrs.message = m AND mrs.user = :user)")
    long countUnreadMessagesByUser(@Param("chatRoom") ChatRoom chatRoom, @Param("user") User user);
}
