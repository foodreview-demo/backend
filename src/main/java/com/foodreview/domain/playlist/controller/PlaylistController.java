package com.foodreview.domain.playlist.controller;

import com.foodreview.domain.playlist.dto.PlaylistDto;
import com.foodreview.domain.playlist.service.PlaylistService;
import com.foodreview.global.common.ApiResponse;
import com.foodreview.global.common.PageResponse;
import com.foodreview.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Playlist", description = "플레이리스트 API")
@RestController
@RequestMapping("/api/playlists")
@RequiredArgsConstructor
public class PlaylistController {

    private final PlaylistService playlistService;

    @Operation(summary = "내 플레이리스트 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PlaylistDto.SimpleResponse>>> getMyPlaylists(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 20) Pageable pageable) {
        PageResponse<PlaylistDto.SimpleResponse> response = playlistService.getMyPlaylists(userDetails.getUserId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "내 플레이리스트 전체 조회 (저장 다이얼로그용)")
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<PlaylistDto.SimpleResponse>>> getMyPlaylistsAll(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<PlaylistDto.SimpleResponse> response = playlistService.getMyPlaylistsAll(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "다른 사용자의 공개 플레이리스트 조회")
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<PageResponse<PlaylistDto.SimpleResponse>>> getUserPublicPlaylists(
            @PathVariable Long userId,
            @PageableDefault(size = 20) Pageable pageable) {
        PageResponse<PlaylistDto.SimpleResponse> response = playlistService.getUserPublicPlaylists(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "플레이리스트 상세 조회")
    @GetMapping("/{playlistId}")
    public ResponseEntity<ApiResponse<PlaylistDto.DetailResponse>> getPlaylistDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long playlistId) {
        PlaylistDto.DetailResponse response = playlistService.getPlaylistDetail(userDetails.getUserId(), playlistId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "플레이리스트 생성")
    @PostMapping
    public ResponseEntity<ApiResponse<PlaylistDto.Response>> createPlaylist(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PlaylistDto.CreateRequest request) {
        PlaylistDto.Response response = playlistService.createPlaylist(userDetails.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response, "플레이리스트가 생성되었습니다"));
    }

    @Operation(summary = "플레이리스트 수정")
    @PutMapping("/{playlistId}")
    public ResponseEntity<ApiResponse<PlaylistDto.Response>> updatePlaylist(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long playlistId,
            @Valid @RequestBody PlaylistDto.UpdateRequest request) {
        PlaylistDto.Response response = playlistService.updatePlaylist(userDetails.getUserId(), playlistId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "플레이리스트가 수정되었습니다"));
    }

    @Operation(summary = "플레이리스트 삭제")
    @DeleteMapping("/{playlistId}")
    public ResponseEntity<ApiResponse<Void>> deletePlaylist(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long playlistId) {
        playlistService.deletePlaylist(userDetails.getUserId(), playlistId);
        return ResponseEntity.ok(ApiResponse.success(null, "플레이리스트가 삭제되었습니다"));
    }

    @Operation(summary = "플레이리스트에 음식점 추가")
    @PostMapping("/{playlistId}/items")
    public ResponseEntity<ApiResponse<PlaylistDto.ItemResponse>> addItem(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long playlistId,
            @Valid @RequestBody PlaylistDto.AddItemRequest request) {
        PlaylistDto.ItemResponse response = playlistService.addItem(userDetails.getUserId(), playlistId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response, "음식점이 추가되었습니다"));
    }

    @Operation(summary = "플레이리스트에서 음식점 제거")
    @DeleteMapping("/{playlistId}/items/{restaurantId}")
    public ResponseEntity<ApiResponse<Void>> removeItem(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long playlistId,
            @PathVariable Long restaurantId) {
        playlistService.removeItem(userDetails.getUserId(), playlistId, restaurantId);
        return ResponseEntity.ok(ApiResponse.success(null, "음식점이 제거되었습니다"));
    }

    @Operation(summary = "아이템 메모 수정")
    @PutMapping("/{playlistId}/items/{restaurantId}")
    public ResponseEntity<ApiResponse<PlaylistDto.ItemResponse>> updateItemMemo(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long playlistId,
            @PathVariable Long restaurantId,
            @Valid @RequestBody PlaylistDto.UpdateItemRequest request) {
        PlaylistDto.ItemResponse response = playlistService.updateItemMemo(userDetails.getUserId(), playlistId, restaurantId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "메모가 수정되었습니다"));
    }

    @Operation(summary = "음식점 저장 상태 확인")
    @GetMapping("/restaurant/{restaurantId}/status")
    public ResponseEntity<ApiResponse<PlaylistDto.SaveStatusResponse>> getRestaurantSaveStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long restaurantId) {
        PlaylistDto.SaveStatusResponse response = playlistService.getRestaurantSaveStatus(userDetails.getUserId(), restaurantId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
