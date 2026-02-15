package com.foodreview.domain.gathering.repository;

import com.foodreview.domain.gathering.entity.DepositStatus;
import com.foodreview.domain.gathering.entity.GatheringParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GatheringParticipantRepository extends JpaRepository<GatheringParticipant, Long> {

    // 모임의 특정 사용자 참여 정보 조회
    Optional<GatheringParticipant> findByGatheringIdAndUserId(Long gatheringId, Long userId);

    // 모임의 모든 참여자 조회
    List<GatheringParticipant> findByGatheringId(Long gatheringId);

    // 모임의 보증금 완료된 참여자 조회
    @Query("SELECT p FROM GatheringParticipant p " +
           "WHERE p.gathering.id = :gatheringId " +
           "AND p.depositStatus = :status")
    List<GatheringParticipant> findByGatheringIdAndDepositStatus(
            @Param("gatheringId") Long gatheringId,
            @Param("status") DepositStatus status);

    // 환금 필요한 참여자 조회 (보증금 완료 또는 환금 실패)
    @Query("SELECT p FROM GatheringParticipant p " +
           "WHERE p.gathering.id = :gatheringId " +
           "AND p.depositStatus IN :statuses")
    List<GatheringParticipant> findParticipantsNeedingRefund(
            @Param("gatheringId") Long gatheringId,
            @Param("statuses") List<DepositStatus> statuses);

    // 환금 실패 목록 조회 (Admin용)
    @Query("SELECT p FROM GatheringParticipant p " +
           "WHERE p.depositStatus = :status " +
           "ORDER BY p.createdAt DESC")
    List<GatheringParticipant> findByDepositStatus(@Param("status") DepositStatus status);

    // 사용자가 해당 모임에 이미 참여했는지 확인
    boolean existsByGatheringIdAndUserId(Long gatheringId, Long userId);

    // 통계: 환금 대기 건수
    long countByDepositStatus(DepositStatus status);
}
