package com.foodreview.domain.playlist.repository;

import com.foodreview.domain.playlist.entity.Playlist;
import com.foodreview.domain.playlist.entity.PlaylistItem;
import com.foodreview.domain.restaurant.entity.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PlaylistItemRepository extends JpaRepository<PlaylistItem, Long> {

    // 플레이리스트 내 아이템 목록 조회 (position 순)
    List<PlaylistItem> findByPlaylistOrderByPositionAsc(Playlist playlist);

    // 플레이리스트 내 특정 음식점 아이템 조회
    Optional<PlaylistItem> findByPlaylistAndRestaurant(Playlist playlist, Restaurant restaurant);

    // 플레이리스트 내 특정 음식점 존재 여부
    boolean existsByPlaylistAndRestaurant(Playlist playlist, Restaurant restaurant);

    // 플레이리스트 내 최대 position 조회
    @Query("SELECT COALESCE(MAX(pi.position), -1) FROM PlaylistItem pi WHERE pi.playlist = :playlist")
    int findMaxPositionByPlaylist(@Param("playlist") Playlist playlist);

    // 플레이리스트의 모든 아이템 삭제
    @Modifying
    void deleteByPlaylist(Playlist playlist);

    // 특정 position 이상의 아이템들 position 감소
    @Modifying
    @Query("UPDATE PlaylistItem pi SET pi.position = pi.position - 1 WHERE pi.playlist = :playlist AND pi.position > :position")
    void decreasePositionAfter(@Param("playlist") Playlist playlist, @Param("position") int position);
}
