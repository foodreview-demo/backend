package com.foodreview.domain.chat.controller;

import com.foodreview.domain.chat.dto.ChatDto;
import com.foodreview.domain.chat.service.ChatService;
import com.foodreview.global.common.ApiResponse;
import com.foodreview.global.common.PageResponse;
import com.foodreview.global.security.CurrentUser;
import com.foodreview.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Chat", description = "채팅 API")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @Operation(summary = "채팅방 목록 조회")
    @GetMapping("/rooms")
    public ResponseEntity<ApiResponse<PageResponse<ChatDto.RoomResponse>>> getChatRooms(
            @CurrentUser CustomUserDetails userDetails,
            @PageableDefault(size = 20, sort = "lastMessageAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<ChatDto.RoomResponse> response = chatService.getChatRooms(userDetails.getUserId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "채팅방 생성 또는 조회")
    @PostMapping("/rooms")
    public ResponseEntity<ApiResponse<ChatDto.RoomResponse>> getOrCreateChatRoom(
            @CurrentUser CustomUserDetails userDetails,
            @Valid @RequestBody ChatDto.CreateRoomRequest request) {
        ChatDto.RoomResponse response = chatService.getOrCreateChatRoom(userDetails.getUserId(), request.getOtherUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @Operation(summary = "채팅 메시지 조회")
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<ApiResponse<PageResponse<ChatDto.MessageResponse>>> getMessages(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable Long roomId,
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable) {
        PageResponse<ChatDto.MessageResponse> response = chatService.getMessages(userDetails.getUserId(), roomId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "메시지 전송")
    @PostMapping("/rooms/{roomId}/messages")
    public ResponseEntity<ApiResponse<ChatDto.MessageResponse>> sendMessage(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable Long roomId,
            @Valid @RequestBody ChatDto.SendMessageRequest request) {
        ChatDto.MessageResponse response = chatService.sendMessage(userDetails.getUserId(), roomId, request.getContent());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    // ===== UUID 기반 API =====

    @Operation(summary = "채팅방 정보 조회 (UUID)")
    @GetMapping("/room/{roomUuid}")
    public ResponseEntity<ApiResponse<ChatDto.RoomResponse>> getChatRoomByUuid(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable String roomUuid) {
        ChatDto.RoomResponse response = chatService.getChatRoomByUuid(userDetails.getUserId(), roomUuid);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "채팅 메시지 조회 (UUID)")
    @GetMapping("/room/{roomUuid}/messages")
    public ResponseEntity<ApiResponse<PageResponse<ChatDto.MessageResponse>>> getMessagesByUuid(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable String roomUuid,
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable) {
        PageResponse<ChatDto.MessageResponse> response = chatService.getMessagesByUuid(userDetails.getUserId(), roomUuid, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "메시지 전송 (UUID)")
    @PostMapping("/room/{roomUuid}/messages")
    public ResponseEntity<ApiResponse<ChatDto.MessageResponse>> sendMessageByUuid(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable String roomUuid,
            @Valid @RequestBody ChatDto.SendMessageRequest request) {
        ChatDto.MessageResponse response = chatService.sendMessageByUuid(userDetails.getUserId(), roomUuid, request.getContent());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }
}
