package com.foodreview.domain.playlist.service;

import com.foodreview.domain.playlist.dto.PlaylistDto;
import com.foodreview.domain.playlist.entity.Playlist;
import com.foodreview.domain.playlist.entity.PlaylistItem;
import com.foodreview.domain.playlist.repository.PlaylistItemRepository;
import com.foodreview.domain.playlist.repository.PlaylistRepository;
import com.foodreview.domain.restaurant.entity.Restaurant;
import com.foodreview.domain.restaurant.repository.RestaurantRepository;
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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaylistService {

    private final PlaylistRepository playlistRepository;
    private final PlaylistItemRepository playlistItemRepository;
    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;

    // 내 플레이리스트 목록 조회
    public PageResponse<PlaylistDto.SimpleResponse> getMyPlaylists(Long userId, Pageable pageable) {
        User user = findUserById(userId);
        Page<Playlist> playlists = playlistRepository.findByUserOrderByCreatedAtDesc(user, pageable);

        List<PlaylistDto.SimpleResponse> content = playlists.getContent().stream()
                .map(PlaylistDto.SimpleResponse::from)
                .toList();

        return PageResponse.from(playlists, content);
    }

    // 내 플레이리스트 목록 조회 (페이징 없이, 음식점 저장 다이얼로그용)
    public List<PlaylistDto.SimpleResponse> getMyPlaylistsAll(Long userId) {
        User user = findUserById(userId);
        return playlistRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(PlaylistDto.SimpleResponse::from)
                .toList();
    }

    // 다른 사용자의 공개 플레이리스트 목록 조회
    public PageResponse<PlaylistDto.SimpleResponse> getUserPublicPlaylists(Long userId, Pageable pageable) {
        User user = findUserById(userId);
        Page<Playlist> playlists = playlistRepository.findByUserAndIsPublicTrueOrderByCreatedAtDesc(user, pageable);

        List<PlaylistDto.SimpleResponse> content = playlists.getContent().stream()
                .map(PlaylistDto.SimpleResponse::from)
                .toList();

        return PageResponse.from(playlists, content);
    }

    // 플레이리스트 상세 조회
    public PlaylistDto.DetailResponse getPlaylistDetail(Long userId, Long playlistId) {
        Playlist playlist = findPlaylistById(playlistId);

        // 비공개 플레이리스트는 소유자만 접근 가능
        if (!playlist.getIsPublic() && !playlist.getUser().getId().equals(userId)) {
            throw new CustomException("접근 권한이 없습니다", HttpStatus.FORBIDDEN, "FORBIDDEN");
        }

        List<PlaylistItem> items = playlistItemRepository.findByPlaylistOrderByPositionAsc(playlist);
        return PlaylistDto.DetailResponse.from(playlist, items);
    }

    // 플레이리스트 생성
    @Transactional
    public PlaylistDto.Response createPlaylist(Long userId, PlaylistDto.CreateRequest request) {
        User user = findUserById(userId);

        Playlist playlist = Playlist.builder()
                .user(user)
                .name(request.getName())
                .description(request.getDescription())
                .isPublic(request.getIsPublic() != null ? request.getIsPublic() : false)
                .build();

        Playlist saved = playlistRepository.save(playlist);
        return PlaylistDto.Response.from(saved);
    }

    // 플레이리스트 수정
    @Transactional
    public PlaylistDto.Response updatePlaylist(Long userId, Long playlistId, PlaylistDto.UpdateRequest request) {
        User user = findUserById(userId);
        Playlist playlist = findPlaylistByIdAndUser(playlistId, user);

        playlist.update(
                request.getName(),
                request.getDescription(),
                request.getIsPublic() != null ? request.getIsPublic() : playlist.getIsPublic()
        );

        return PlaylistDto.Response.from(playlist);
    }

    // 플레이리스트 삭제
    @Transactional
    public void deletePlaylist(Long userId, Long playlistId) {
        User user = findUserById(userId);
        Playlist playlist = findPlaylistByIdAndUser(playlistId, user);

        playlistRepository.delete(playlist);
    }

    // 플레이리스트에 음식점 추가
    @Transactional
    public PlaylistDto.ItemResponse addItem(Long userId, Long playlistId, PlaylistDto.AddItemRequest request) {
        User user = findUserById(userId);
        Playlist playlist = findPlaylistByIdAndUser(playlistId, user);
        Restaurant restaurant = findRestaurantById(request.getRestaurantId());

        // 이미 추가된 음식점인지 확인
        if (playlistItemRepository.existsByPlaylistAndRestaurant(playlist, restaurant)) {
            throw new CustomException("이미 추가된 음식점입니다", HttpStatus.BAD_REQUEST, "ALREADY_EXISTS");
        }

        int maxPosition = playlistItemRepository.findMaxPositionByPlaylist(playlist);

        PlaylistItem item = PlaylistItem.builder()
                .playlist(playlist)
                .restaurant(restaurant)
                .position(maxPosition + 1)
                .memo(request.getMemo())
                .build();

        PlaylistItem saved = playlistItemRepository.save(item);

        // 첫 번째 아이템이면 썸네일 업데이트
        if (playlist.getThumbnail() == null && restaurant.getThumbnail() != null) {
            playlist.updateThumbnail(restaurant.getThumbnail());
        }

        return PlaylistDto.ItemResponse.from(saved);
    }

    // 플레이리스트에서 음식점 제거
    @Transactional
    public void removeItem(Long userId, Long playlistId, Long restaurantId) {
        User user = findUserById(userId);
        Playlist playlist = findPlaylistByIdAndUser(playlistId, user);
        Restaurant restaurant = findRestaurantById(restaurantId);

        PlaylistItem item = playlistItemRepository.findByPlaylistAndRestaurant(playlist, restaurant)
                .orElseThrow(() -> new CustomException("해당 음식점이 플레이리스트에 없습니다", HttpStatus.NOT_FOUND, "NOT_FOUND"));

        int removedPosition = item.getPosition();
        playlistItemRepository.delete(item);

        // 삭제된 position 이후의 아이템들 position 감소
        playlistItemRepository.decreasePositionAfter(playlist, removedPosition);
    }

    // 아이템 메모 수정
    @Transactional
    public PlaylistDto.ItemResponse updateItemMemo(Long userId, Long playlistId, Long restaurantId, PlaylistDto.UpdateItemRequest request) {
        User user = findUserById(userId);
        Playlist playlist = findPlaylistByIdAndUser(playlistId, user);
        Restaurant restaurant = findRestaurantById(restaurantId);

        PlaylistItem item = playlistItemRepository.findByPlaylistAndRestaurant(playlist, restaurant)
                .orElseThrow(() -> new CustomException("해당 음식점이 플레이리스트에 없습니다", HttpStatus.NOT_FOUND, "NOT_FOUND"));

        item.updateMemo(request.getMemo());
        return PlaylistDto.ItemResponse.from(item);
    }

    // 음식점의 저장 상태 확인 (어떤 플레이리스트에 저장되어 있는지)
    public PlaylistDto.SaveStatusResponse getRestaurantSaveStatus(Long userId, Long restaurantId) {
        User user = findUserById(userId);
        List<Long> savedPlaylistIds = playlistRepository.findPlaylistIdsByUserAndRestaurantId(user, restaurantId);

        return PlaylistDto.SaveStatusResponse.builder()
                .restaurantId(restaurantId)
                .savedPlaylistIds(savedPlaylistIds)
                .isSaved(!savedPlaylistIds.isEmpty())
                .build();
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException("사용자를 찾을 수 없습니다", HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));
    }

    private Playlist findPlaylistById(Long playlistId) {
        return playlistRepository.findById(playlistId)
                .orElseThrow(() -> new CustomException("플레이리스트를 찾을 수 없습니다", HttpStatus.NOT_FOUND, "PLAYLIST_NOT_FOUND"));
    }

    private Playlist findPlaylistByIdAndUser(Long playlistId, User user) {
        return playlistRepository.findByIdAndUser(playlistId, user)
                .orElseThrow(() -> new CustomException("플레이리스트를 찾을 수 없거나 접근 권한이 없습니다", HttpStatus.NOT_FOUND, "PLAYLIST_NOT_FOUND"));
    }

    private Restaurant findRestaurantById(Long restaurantId) {
        return restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new CustomException("음식점을 찾을 수 없습니다", HttpStatus.NOT_FOUND, "RESTAURANT_NOT_FOUND"));
    }
}
