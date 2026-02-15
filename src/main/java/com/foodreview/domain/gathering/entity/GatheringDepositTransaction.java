package com.foodreview.domain.gathering.entity;

import com.foodreview.domain.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "gathering_deposit_transaction",
        indexes = {
                @Index(name = "idx_transaction_participant", columnList = "participant_id"),
                @Index(name = "idx_transaction_type", columnList = "transaction_type"),
                @Index(name = "idx_transaction_status", columnList = "status"),
                @Index(name = "idx_transaction_imp_uid", columnList = "imp_uid")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class GatheringDepositTransaction extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id", nullable = false)
    private GatheringParticipant participant;

    @Column(nullable = false, precision = 10, scale = 0)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "refund_type")
    private RefundType refundType;

    // 아임포트 결제/환금 고유 ID
    @Column(name = "imp_uid", length = 100)
    private String impUid;

    // 가맹점 주문번호
    @Column(name = "merchant_uid", length = 100)
    private String merchantUid;

    // 실패 사유
    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    // Admin 메모 (수동 처리 시)
    @Column(name = "admin_note", length = 500)
    private String adminNote;

    public enum TransactionType {
        CHARGE("결제"),
        REFUND("환금");

        private final String displayName;

        TransactionType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum TransactionStatus {
        PENDING("대기중"),
        COMPLETED("완료"),
        FAILED("실패");

        private final String displayName;

        TransactionStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public void complete(String impUid) {
        this.status = TransactionStatus.COMPLETED;
        this.impUid = impUid;
    }

    public void fail(String reason) {
        this.status = TransactionStatus.FAILED;
        this.failureReason = reason;
    }

    public void setAdminNote(String note) {
        this.adminNote = note;
    }

    public boolean isCompleted() {
        return this.status == TransactionStatus.COMPLETED;
    }

    public boolean isFailed() {
        return this.status == TransactionStatus.FAILED;
    }

    public boolean isRefund() {
        return this.transactionType == TransactionType.REFUND;
    }
}
