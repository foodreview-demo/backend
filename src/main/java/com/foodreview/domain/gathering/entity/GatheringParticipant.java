package com.foodreview.domain.gathering.entity;

import com.foodreview.domain.common.BaseTimeEntity;
import com.foodreview.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "gathering_participant",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"gathering_id", "user_id"})
        },
        indexes = {
                @Index(name = "idx_participant_gathering", columnList = "gathering_id"),
                @Index(name = "idx_participant_user", columnList = "user_id"),
                @Index(name = "idx_participant_deposit_status", columnList = "deposit_status")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class GatheringParticipant extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gathering_id", nullable = false)
    private Gathering gathering;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "deposit_status", nullable = false)
    @Builder.Default
    private DepositStatus depositStatus = DepositStatus.PENDING;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    // 아임포트 결제 고유 ID (imp_uid)
    @Column(name = "imp_uid", length = 100)
    private String impUid;

    // 가맹점 주문번호 (merchant_uid)
    @Column(name = "merchant_uid", length = 100)
    private String merchantUid;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @Column(name = "refund_failed_reason", length = 500)
    private String refundFailedReason;

    public void confirmDeposit(String impUid, String merchantUid) {
        this.depositStatus = DepositStatus.DEPOSITED;
        this.joinedAt = LocalDateTime.now();
        this.impUid = impUid;
        this.merchantUid = merchantUid;
    }

    public void markRefunded() {
        this.depositStatus = DepositStatus.REFUNDED;
        this.refundedAt = LocalDateTime.now();
    }

    public void markRefundFailed(String reason) {
        this.depositStatus = DepositStatus.REFUND_FAILED;
        this.refundFailedReason = reason;
    }

    public boolean isDeposited() {
        return this.depositStatus == DepositStatus.DEPOSITED;
    }

    public boolean needsRefund() {
        return this.depositStatus == DepositStatus.DEPOSITED ||
               this.depositStatus == DepositStatus.REFUND_FAILED;
    }

    public boolean isHost() {
        return this.gathering.getCreator().getId().equals(this.user.getId());
    }
}
