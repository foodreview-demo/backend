package com.foodreview.domain.badge.repository;

import com.foodreview.domain.badge.entity.Badge;
import com.foodreview.domain.badge.entity.BadgeCategory;
import com.foodreview.domain.badge.entity.BadgeConditionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BadgeRepository extends JpaRepository<Badge, Long> {

    Optional<Badge> findByCode(String code);

    List<Badge> findByActiveOrderBySortOrderAsc(Boolean active);

    List<Badge> findByCategoryAndActiveOrderBySortOrderAsc(BadgeCategory category, Boolean active);

    // 특정 조건 타입의 배지 목록 조회
    List<Badge> findByConditionTypeAndActiveOrderByConditionValueAsc(BadgeConditionType conditionType, Boolean active);

    // 조건을 만족하는 배지 조회 (조건값 이하인 모든 배지)
    @Query("SELECT b FROM Badge b WHERE b.conditionType = :conditionType AND b.conditionValue <= :value AND b.active = true ORDER BY b.conditionValue DESC")
    List<Badge> findEligibleBadges(@Param("conditionType") BadgeConditionType conditionType, @Param("value") Integer value);
}
