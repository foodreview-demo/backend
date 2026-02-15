package com.foodreview.domain.gathering.repository;

import com.foodreview.domain.gathering.entity.GatheringDepositTransaction;
import com.foodreview.domain.gathering.entity.GatheringDepositTransaction.TransactionStatus;
import com.foodreview.domain.gathering.entity.GatheringDepositTransaction.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GatheringDepositTransactionRepository extends JpaRepository<GatheringDepositTransaction, Long> {

    // 아임포트 결제 ID로 조회
    Optional<GatheringDepositTransaction> findByImpUid(String impUid);

    // 가맹점 주문번호로 조회
    Optional<GatheringDepositTransaction> findByMerchantUid(String merchantUid);

    // 참여자의 모든 거래 내역 조회
    List<GatheringDepositTransaction> findByParticipantIdOrderByCreatedAtDesc(Long participantId);

    // 실패한 환금 거래 조회 (Admin용)
    @Query("SELECT t FROM GatheringDepositTransaction t " +
           "WHERE t.transactionType = :type " +
           "AND t.status = :status " +
           "ORDER BY t.createdAt DESC")
    Page<GatheringDepositTransaction> findFailedRefunds(
            @Param("type") TransactionType type,
            @Param("status") TransactionStatus status,
            Pageable pageable);

    // 통계: 실패한 환금 건수
    @Query("SELECT COUNT(t) FROM GatheringDepositTransaction t " +
           "WHERE t.transactionType = :type " +
           "AND t.status = :status")
    long countByTypeAndStatus(
            @Param("type") TransactionType type,
            @Param("status") TransactionStatus status);
}
