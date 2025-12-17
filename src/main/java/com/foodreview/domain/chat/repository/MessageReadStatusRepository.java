package com.foodreview.domain.chat.repository;

import com.foodreview.domain.chat.entity.ChatMessage;
import com.foodreview.domain.chat.entity.MessageReadStatus;
import com.foodreview.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MessageReadStatusRepository extends JpaRepository<MessageReadStatus, Long> {

    // 특정 메시지를 읽은 사용자 수
    long countByMessage(ChatMessage message);

    // 특정 메시지를 특정 사용자가 읽었는지 확인
    boolean existsByMessageAndUser(ChatMessage message, User user);

    // 특정 메시지를 읽은 사용자 목록
    List<MessageReadStatus> findByMessage(ChatMessage message);

    // 특정 사용자가 특정 메시지를 읽은 기록
    Optional<MessageReadStatus> findByMessageAndUser(ChatMessage message, User user);

    // 여러 메시지의 읽음 수를 한 번에 조회
    @Query("SELECT mrs.message.id, COUNT(mrs) FROM MessageReadStatus mrs WHERE mrs.message IN :messages GROUP BY mrs.message.id")
    List<Object[]> countByMessages(@Param("messages") List<ChatMessage> messages);

    // 특정 채팅방의 모든 읽음 상태 삭제 (채팅방 삭제 시)
    @Query("DELETE FROM MessageReadStatus mrs WHERE mrs.message.chatRoom.id = :roomId")
    void deleteByRoomId(@Param("roomId") Long roomId);
}
