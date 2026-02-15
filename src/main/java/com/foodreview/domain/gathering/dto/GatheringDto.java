package com.foodreview.domain.gathering.dto;

import com.foodreview.domain.gathering.entity.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class GatheringDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateRequest {
        private Long restaurantId;
        private String title;
        private String description;
        private LocalDateTime targetTime;
        private Integer maxParticipants;
        private BigDecimal depositAmount;
        private RefundType refundType;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long id;
        private String uuid;
        private RestaurantInfo restaurant;
        private CreatorInfo creator;
        private String title;
        private String description;
        private LocalDateTime targetTime;
        private Integer maxParticipants;
        private Integer currentParticipants;
        private BigDecimal depositAmount;
        private String refundType;
        private String refundTypeDisplay;
        private String status;
        private String statusDisplay;
        private String region;
        private String district;
        private String neighborhood;
        private String chatRoomUuid;
        private Boolean isHost;
        private Boolean isParticipant;
        private String myDepositStatus;
        private LocalDateTime createdAt;

        public static Response from(Gathering gathering, Long currentUserId) {
            boolean isHost = gathering.getCreator().getId().equals(currentUserId);

            return Response.builder()
                    .id(gathering.getId())
                    .uuid(gathering.getUuid())
                    .restaurant(RestaurantInfo.from(gathering.getRestaurant()))
                    .creator(CreatorInfo.from(gathering.getCreator()))
                    .title(gathering.getTitle())
                    .description(gathering.getDescription())
                    .targetTime(gathering.getTargetTime())
                    .maxParticipants(gathering.getMaxParticipants())
                    .currentParticipants(gathering.getCurrentParticipants())
                    .depositAmount(gathering.getDepositAmount())
                    .refundType(gathering.getRefundType().name())
                    .refundTypeDisplay(gathering.getRefundType().getDisplayName())
                    .status(gathering.getStatus().name())
                    .statusDisplay(gathering.getStatus().getDisplayName())
                    .region(gathering.getRegion())
                    .district(gathering.getDistrict())
                    .neighborhood(gathering.getNeighborhood())
                    .chatRoomUuid(gathering.getChatRoomUuid())
                    .isHost(isHost)
                    .isParticipant(false)
                    .myDepositStatus(null)
                    .createdAt(gathering.getCreatedAt())
                    .build();
        }

        public static Response from(Gathering gathering, Long currentUserId, GatheringParticipant myParticipation) {
            Response response = from(gathering, currentUserId);
            if (myParticipation != null) {
                return Response.builder()
                        .id(response.getId())
                        .uuid(response.getUuid())
                        .restaurant(response.getRestaurant())
                        .creator(response.getCreator())
                        .title(response.getTitle())
                        .description(response.getDescription())
                        .targetTime(response.getTargetTime())
                        .maxParticipants(response.getMaxParticipants())
                        .currentParticipants(response.getCurrentParticipants())
                        .depositAmount(response.getDepositAmount())
                        .refundType(response.getRefundType())
                        .refundTypeDisplay(response.getRefundTypeDisplay())
                        .status(response.getStatus())
                        .statusDisplay(response.getStatusDisplay())
                        .region(response.getRegion())
                        .district(response.getDistrict())
                        .neighborhood(response.getNeighborhood())
                        .chatRoomUuid(response.getChatRoomUuid())
                        .isHost(response.getIsHost())
                        .isParticipant(true)
                        .myDepositStatus(myParticipation.getDepositStatus().name())
                        .createdAt(response.getCreatedAt())
                        .build();
            }
            return response;
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RestaurantInfo {
        private Long id;
        private String uuid;
        private String name;
        private String category;
        private String categoryDisplay;
        private String address;
        private String thumbnail;

        public static RestaurantInfo from(com.foodreview.domain.restaurant.entity.Restaurant restaurant) {
            return RestaurantInfo.builder()
                    .id(restaurant.getId())
                    .uuid(restaurant.getUuid())
                    .name(restaurant.getName())
                    .category(restaurant.getCategory().name())
                    .categoryDisplay(restaurant.getCategory().getDisplayName())
                    .address(restaurant.getAddress())
                    .thumbnail(restaurant.getThumbnail())
                    .build();
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreatorInfo {
        private Long id;
        private String name;
        private String avatar;
        private Integer tasteScore;

        public static CreatorInfo from(com.foodreview.domain.user.entity.User user) {
            return CreatorInfo.builder()
                    .id(user.getId())
                    .name(user.getName())
                    .avatar(user.getAvatar())
                    .tasteScore(user.getTasteScore())
                    .build();
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DetailResponse {
        private Response gathering;
        private List<ParticipantInfo> participants;

        public static DetailResponse from(Gathering gathering, Long currentUserId,
                                          GatheringParticipant myParticipation,
                                          List<GatheringParticipant> allParticipants) {
            List<ParticipantInfo> participantInfos = allParticipants.stream()
                    .filter(p -> p.getDepositStatus() == DepositStatus.DEPOSITED)
                    .map(ParticipantInfo::from)
                    .toList();

            return DetailResponse.builder()
                    .gathering(Response.from(gathering, currentUserId, myParticipation))
                    .participants(participantInfos)
                    .build();
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ParticipantInfo {
        private Long id;
        private Long userId;
        private String userName;
        private String userAvatar;
        private Integer userTasteScore;
        private String depositStatus;
        private String depositStatusDisplay;
        private LocalDateTime joinedAt;
        private Boolean isHost;

        public static ParticipantInfo from(GatheringParticipant participant) {
            return ParticipantInfo.builder()
                    .id(participant.getId())
                    .userId(participant.getUser().getId())
                    .userName(participant.getUser().getName())
                    .userAvatar(participant.getUser().getAvatar())
                    .userTasteScore(participant.getUser().getTasteScore())
                    .depositStatus(participant.getDepositStatus().name())
                    .depositStatusDisplay(participant.getDepositStatus().getDisplayName())
                    .joinedAt(participant.getJoinedAt())
                    .isHost(participant.isHost())
                    .build();
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class JoinRequest {
        private String impUid;        // 아임포트 결제 고유 ID
        private String merchantUid;   // 가맹점 주문번호
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RefundRequest {
        private Long participantId;   // 특정 참여자만 환금할 때 (호스트용)
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StatusUpdateRequest {
        private GatheringStatus status;
    }
}
