package com.foodreview.domain.review.entity;

/**
 * 영수증 검증 상태
 */
public enum ReceiptVerificationStatus {
    /**
     * 영수증 미첨부
     */
    NONE,

    /**
     * 검증 대기 중 (OCR 처리 전)
     */
    PENDING,

    /**
     * 자동 검증 완료 - 영수증으로 인정
     */
    VERIFIED,

    /**
     * 자동 검증 실패 - 영수증이 아님
     */
    REJECTED,

    /**
     * 수동 검토 필요
     */
    PENDING_REVIEW,

    /**
     * Admin에 의해 수동 승인
     */
    MANUALLY_APPROVED,

    /**
     * Admin에 의해 수동 거부
     */
    MANUALLY_REJECTED
}
