package com.foodreview.domain.playlist.dto;

import com.foodreview.domain.playlist.entity.Playlist;
import com.foodreview.domain.playlist.entity.PlaylistItem;
import com.foodreview.domain.restaurant.dto.RestaurantDto;
import com.foodreview.domain.user.dto.UserDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

public class PlaylistDto {

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String name;
        private String description;
        private Boolean isPublic;
        private String thumbnail;
        private Integer itemCount;
        private UserDto.SimpleResponse user;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static Response from(Playlist playlist) {
            return Response.builder()
                    .id(playlist.getId())
                    .name(playlist.getName())
                    .description(playlist.getDescription())
                    .isPublic(playlist.getIsPublic())
                    .thumbnail(playlist.getThumbnail())
                    .itemCount(playlist.getItemCount())
                    .user(UserDto.SimpleResponse.from(playlist.getUser()))
                    .createdAt(playlist.getCreatedAt())
                    .updatedAt(playlist.getUpdatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class SimpleResponse {
        private Long id;
        private String name;
        private String thumbnail;
        private Integer itemCount;
        private Boolean isPublic;

        public static SimpleResponse from(Playlist playlist) {
            return SimpleResponse.builder()
                    .id(playlist.getId())
                    .name(playlist.getName())
                    .thumbnail(playlist.getThumbnail())
                    .itemCount(playlist.getItemCount())
                    .isPublic(playlist.getIsPublic())
                    .build();
        }
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class DetailResponse {
        private Long id;
        private String name;
        private String description;
        private Boolean isPublic;
        private String thumbnail;
        private Integer itemCount;
        private UserDto.SimpleResponse user;
        private List<ItemResponse> items;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static DetailResponse from(Playlist playlist, List<PlaylistItem> items) {
            return DetailResponse.builder()
                    .id(playlist.getId())
                    .name(playlist.getName())
                    .description(playlist.getDescription())
                    .isPublic(playlist.getIsPublic())
                    .thumbnail(playlist.getThumbnail())
                    .itemCount(playlist.getItemCount())
                    .user(UserDto.SimpleResponse.from(playlist.getUser()))
                    .items(items.stream().map(ItemResponse::from).toList())
                    .createdAt(playlist.getCreatedAt())
                    .updatedAt(playlist.getUpdatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class ItemResponse {
        private Long id;
        private RestaurantDto.SimpleResponse restaurant;
        private Integer position;
        private String memo;
        private LocalDateTime addedAt;

        public static ItemResponse from(PlaylistItem item) {
            return ItemResponse.builder()
                    .id(item.getId())
                    .restaurant(RestaurantDto.SimpleResponse.from(item.getRestaurant()))
                    .position(item.getPosition())
                    .memo(item.getMemo())
                    .addedAt(item.getCreatedAt())
                    .build();
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        @NotBlank(message = "플레이리스트 이름은 필수입니다")
        @Size(max = 100, message = "플레이리스트 이름은 100자 이내로 작성해주세요")
        private String name;

        @Size(max = 500, message = "설명은 500자 이내로 작성해주세요")
        private String description;

        private Boolean isPublic = false;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        @NotBlank(message = "플레이리스트 이름은 필수입니다")
        @Size(max = 100, message = "플레이리스트 이름은 100자 이내로 작성해주세요")
        private String name;

        @Size(max = 500, message = "설명은 500자 이내로 작성해주세요")
        private String description;

        private Boolean isPublic;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddItemRequest {
        @NotNull(message = "음식점 ID는 필수입니다")
        private Long restaurantId;

        @Size(max = 500, message = "메모는 500자 이내로 작성해주세요")
        private String memo;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateItemRequest {
        @Size(max = 500, message = "메모는 500자 이내로 작성해주세요")
        private String memo;
    }

    // 음식점이 어떤 플레이리스트에 저장되어 있는지 확인용
    @Getter
    @Builder
    @AllArgsConstructor
    public static class SaveStatusResponse {
        private Long restaurantId;
        private List<Long> savedPlaylistIds;
        private Boolean isSaved;
    }
}
