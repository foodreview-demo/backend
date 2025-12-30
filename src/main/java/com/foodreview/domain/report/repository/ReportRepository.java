package com.foodreview.domain.report.repository;

import com.foodreview.domain.report.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {

    boolean existsByReviewIdAndReporterId(Long reviewId, Long reporterId);
}
