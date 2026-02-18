package com.foodreview.domain.gathering.entity;

import com.foodreview.domain.common.BaseTimeEntity;
import com.foodreview.domain.restaurant.entity.Restaurant;
import com.foodreview.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "gathering", indexes = {
        @Index(name = "idx_gathering_uuid", columnList = "uuid", unique = true),
        @Index(name = "idx_gathering_restaurant", columnList = "restaurant_id"),
        @Index(name = "idx_gathering_creator", columnList = "creator_id"),
        @Index(name = "idx_gathering_status", columnList = "status"),
        @Index(name = "idx_gathering_target_time", columnList = "target_time"),
        @Index(name = "idx_gathering_region", columnList = "region, district")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Gathering extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 36)
    private String uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 500)
    private String description;

    @Column(name = "target_time", nullable = false)
    private LocalDateTime targetTime;

    @Column(name = "max_participants", nullable = false)
    private Integer maxParticipants;

    @Column(name = "current_participants", nullable = false)
    @Builder.Default
    private Integer currentParticipants = 1; // 호스트 포함

    @Column(name = "deposit_amount", nullable = false, precision = 10, scale = 0)
    private BigDecimal depositAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "refund_type", nullable = false)
    private RefundType refundType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private GatheringStatus status = GatheringStatus.RECRUITING;

    // 지역 정보 (음식점과 별도로 저장 - 검색 최적화)
    @Column(length = 50)
    private String region;

    @Column(length = 50)
    private String district;

    @Column(length = 50)
    private String neighborhood;

    // 채팅방 UUID (모임 생성 시 자동으로 그룹 채팅방 생성)
    @Column(name = "chat_room_uuid", length = 36)
    private String chatRoomUuid;

    // 리마인더 알림 발송 여부
    @Column(name = "reminder_sent", nullable = false)
    @Builder.Default
    private Boolean reminderSent = false;

    @OneToMany(mappedBy = "gathering", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<GatheringParticipant> participants = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (this.uuid == null) {
            this.uuid = UUID.randomUUID().toString();
        }
        // 음식점의 지역 정보 복사
        if (this.restaurant != null) {
            this.region = this.restaurant.getRegion();
            this.district = this.restaurant.getDistrict();
            this.neighborhood = this.restaurant.getNeighborhood();
        }
    }

    public void setChatRoomUuid(String chatRoomUuid) {
        this.chatRoomUuid = chatRoomUuid;
    }

    public void addParticipant() {
        this.currentParticipants++;
        if (this.currentParticipants >= this.maxParticipants) {
            this.status = GatheringStatus.CONFIRMED;
        }
    }

    public void removeParticipant() {
        if (this.currentParticipants > 1) {
            this.currentParticipants--;
            if (this.status == GatheringStatus.CONFIRMED && this.currentParticipants < this.maxParticipants) {
                this.status = GatheringStatus.RECRUITING;
            }
        }
    }

    public boolean isFull() {
        return this.currentParticipants >= this.maxParticipants;
    }

    public boolean isHost(User user) {
        return this.creator.getId().equals(user.getId());
    }

    public void startGathering() {
        this.status = GatheringStatus.IN_PROGRESS;
    }

    public void completeGathering() {
        this.status = GatheringStatus.COMPLETED;
    }

    public void cancelGathering() {
        this.status = GatheringStatus.CANCELLED;
    }

    public boolean isRecruiting() {
        return this.status == GatheringStatus.RECRUITING;
    }

    public boolean canJoin() {
        return isRecruiting() && !isFull() && LocalDateTime.now().isBefore(this.targetTime);
    }

    public boolean isAutoRefund() {
        return this.refundType == RefundType.AUTO;
    }

    public void markReminderSent() {
        this.reminderSent = true;
    }
}
