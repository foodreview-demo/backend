package com.foodreview.domain.playlist.repository;

import com.foodreview.domain.playlist.entity.Playlist;
import com.foodreview.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PlaylistRepository extends JpaRepository<Playlist, Long> {

    // 사용자의 플레이리스트 목록 조회
    Page<Playlist> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    // 사용자의 플레이리스트 목록 (페이징 없이)
    List<Playlist> findByUserOrderByCreatedAtDesc(User user);

    // 특정 플레이리스트 조회 (소유자 확인용)
    Optional<Playlist> findByIdAndUser(Long id, User user);

    // 공개된 플레이리스트 조회 (다른 사용자 프로필에서)
    Page<Playlist> findByUserAndIsPublicTrueOrderByCreatedAtDesc(User user, Pageable pageable);

    // 특정 음식점이 포함된 사용자의 플레이리스트 ID 목록
    @Query("SELECT p.id FROM Playlist p JOIN p.items i WHERE p.user = :user AND i.restaurant.id = :restaurantId")
    List<Long> findPlaylistIdsByUserAndRestaurantId(@Param("user") User user, @Param("restaurantId") Long restaurantId);
}
