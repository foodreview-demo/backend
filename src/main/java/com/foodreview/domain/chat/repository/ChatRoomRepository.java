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

    // 사용자의 채팅방 목록
    @Query("SELECT c FROM ChatRoom c WHERE c.user1 = :user OR c.user2 = :user ORDER BY c.lastMessageAt DESC")
    Page<ChatRoom> findByUser(@Param("user") User user, Pageable pageable);

    // 두 사용자 간의 채팅방 조회
    @Query("SELECT c FROM ChatRoom c WHERE " +
           "(c.user1 = :user1 AND c.user2 = :user2) OR " +
           "(c.user1 = :user2 AND c.user2 = :user1)")
    Optional<ChatRoom> findByUsers(@Param("user1") User user1, @Param("user2") User user2);

    // UUID로 채팅방 조회
    Optional<ChatRoom> findByUuid(String uuid);
}
