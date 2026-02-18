package com.foodreview.domain.gathering.repository;

import com.foodreview.domain.gathering.entity.Gathering;
import com.foodreview.domain.gathering.entity.GatheringStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface GatheringRepository extends JpaRepository<Gathering, Long> {

    Optional<Gathering> findByUuid(String uuid);

    // 음식점별 모임 조회 (모집중/확정만)
    @Query("SELECT g FROM Gathering g " +
           "WHERE g.restaurant.id = :restaurantId " +
           "AND g.status IN :statuses " +
           "AND g.targetTime > :now " +
           "ORDER BY g.targetTime ASC")
    List<Gathering> findActiveByRestaurantId(
            @Param("restaurantId") Long restaurantId,
            @Param("statuses") List<GatheringStatus> statuses,
            @Param("now") LocalDateTime now);

    // 지역별 모임 조회
    @Query("SELECT g FROM Gathering g " +
           "WHERE g.region = :region " +
           "AND (:district IS NULL OR g.district = :district) " +
           "AND g.status IN :statuses " +
           "AND g.targetTime > :now " +
           "ORDER BY g.targetTime ASC")
    Page<Gathering> findByRegionAndStatus(
            @Param("region") String region,
            @Param("district") String district,
            @Param("statuses") List<GatheringStatus> statuses,
            @Param("now") LocalDateTime now,
            Pageable pageable);

    // 사용자가 생성한 모임 조회
    @Query("SELECT g FROM Gathering g WHERE g.creator.id = :userId ORDER BY g.createdAt DESC")
    Page<Gathering> findByCreatorId(@Param("userId") Long userId, Pageable pageable);

    // 사용자가 참여한 모임 조회
    @Query("SELECT g FROM Gathering g " +
           "JOIN g.participants p " +
           "WHERE p.user.id = :userId " +
           "ORDER BY g.targetTime DESC")
    Page<Gathering> findByParticipantUserId(@Param("userId") Long userId, Pageable pageable);

    // 자동 환금 대상 모임 조회 (완료 상태, 자동환금 타입)
    @Query("SELECT g FROM Gathering g " +
           "WHERE g.status = :status " +
           "AND g.refundType = com.foodreview.domain.gathering.entity.RefundType.AUTO " +
           "AND g.targetTime < :before")
    List<Gathering> findCompletedAutoRefundGatherings(
            @Param("status") GatheringStatus status,
            @Param("before") LocalDateTime before);

    // 시작 시간이 지난 모집중 모임 (상태 변경 대상)
    @Query("SELECT g FROM Gathering g " +
           "WHERE g.status = :status " +
           "AND g.targetTime <= :now")
    List<Gathering> findByStatusAndTargetTimeBefore(
            @Param("status") GatheringStatus status,
            @Param("now") LocalDateTime now);

    // 통계: 전체 모임 수
    long countByStatus(GatheringStatus status);

    // 통계: 오늘 생성된 모임 수
    @Query("SELECT COUNT(g) FROM Gathering g WHERE g.createdAt >= :startOfDay")
    long countCreatedToday(@Param("startOfDay") LocalDateTime startOfDay);

    // 리마인더 알림 대상 모임 (1시간 전 ~ 현재 사이에 시작, 아직 리마인더 미발송)
    @Query("SELECT g FROM Gathering g " +
           "WHERE g.status IN :statuses " +
           "AND g.targetTime BETWEEN :now AND :oneHourLater " +
           "AND g.reminderSent = false")
    List<Gathering> findGatheringsForReminder(
            @Param("statuses") List<GatheringStatus> statuses,
            @Param("now") LocalDateTime now,
            @Param("oneHourLater") LocalDateTime oneHourLater);
}
