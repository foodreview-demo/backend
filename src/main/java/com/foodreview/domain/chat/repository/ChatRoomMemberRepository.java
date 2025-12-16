package com.foodreview.domain.chat.repository;

import com.foodreview.domain.chat.entity.ChatRoom;
import com.foodreview.domain.chat.entity.ChatRoomMember;
import com.foodreview.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {

    // 채팅방의 모든 멤버 조회
    List<ChatRoomMember> findByChatRoom(ChatRoom chatRoom);

    // 특정 유저의 모든 채팅방 멤버십 조회
    List<ChatRoomMember> findByUser(User user);

    // 특정 채팅방에서 특정 유저의 멤버십 조회
    Optional<ChatRoomMember> findByChatRoomAndUser(ChatRoom chatRoom, User user);

    // 특정 유저가 채팅방 멤버인지 확인
    boolean existsByChatRoomAndUser(ChatRoom chatRoom, User user);

    // 채팅방의 멤버 수 조회
    long countByChatRoom(ChatRoom chatRoom);

    // 채팅방에서 특정 유저 삭제
    void deleteByChatRoomAndUser(ChatRoom chatRoom, User user);

    // 채팅방의 모든 멤버 삭제
    void deleteByChatRoom(ChatRoom chatRoom);

    // 특정 유저가 참여한 단체톡방 목록 조회
    @Query("SELECT m.chatRoom FROM ChatRoomMember m WHERE m.user = :user")
    List<ChatRoom> findGroupChatRoomsByUser(@Param("user") User user);

    // 채팅방의 방장 조회
    @Query("SELECT m FROM ChatRoomMember m WHERE m.chatRoom = :chatRoom AND m.role = 'OWNER'")
    Optional<ChatRoomMember> findOwnerByChatRoom(@Param("chatRoom") ChatRoom chatRoom);
}
