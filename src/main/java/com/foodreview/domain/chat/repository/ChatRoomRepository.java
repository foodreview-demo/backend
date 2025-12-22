package com.foodreview.domain.chat.repository;

import com.foodreview.domain.chat.entity.ChatRoom;
import com.foodreview.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    // 사용자의 채팅방 목록 (1:1 채팅 + 단체톡 모두 포함) - user1, user2 JOIN FETCH 추가
    @Query("SELECT DISTINCT c FROM ChatRoom c " +
           "LEFT JOIN FETCH c.user1 " +
           "LEFT JOIN FETCH c.user2 " +
           "LEFT JOIN c.members m " +
           "WHERE c.user1 = :user OR c.user2 = :user OR m.user = :user " +
           "ORDER BY c.lastMessageAt DESC")
    Page<ChatRoom> findByUser(@Param("user") User user, Pageable pageable);

    // 두 사용자 간의 채팅방 조회 (1:1 채팅용)
    @Query("SELECT c FROM ChatRoom c WHERE c.roomType = 'DIRECT' AND " +
           "((c.user1 = :user1 AND c.user2 = :user2) OR " +
           "(c.user1 = :user2 AND c.user2 = :user1))")
    Optional<ChatRoom> findByUsers(@Param("user1") User user1, @Param("user2") User user2);

    // UUID로 채팅방 조회 (user1, user2 JOIN FETCH)
    @Query("SELECT c FROM ChatRoom c " +
           "LEFT JOIN FETCH c.user1 " +
           "LEFT JOIN FETCH c.user2 " +
           "WHERE c.uuid = :uuid")
    Optional<ChatRoom> findByUuidWithUsers(@Param("uuid") String uuid);

    // UUID로 채팅방 조회 (기본)
    Optional<ChatRoom> findByUuid(String uuid);
}
