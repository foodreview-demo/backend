package com.foodreview.domain.user.repository;

import com.foodreview.domain.user.entity.ScoreEvent;
import com.foodreview.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScoreEventRepository extends JpaRepository<ScoreEvent, Long> {

    // 사용자의 점수 획득 내역 조회
    Page<ScoreEvent> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
}
