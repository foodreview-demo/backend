package com.foodreview.domain.report.service;

import com.foodreview.domain.report.dto.ReportRequest;
import com.foodreview.domain.report.dto.ReportResponse;
import com.foodreview.domain.report.entity.Report;
import com.foodreview.domain.report.repository.ReportRepository;
import com.foodreview.domain.review.entity.Review;
import com.foodreview.domain.review.repository.ReviewRepository;
import com.foodreview.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final ReportRepository reportRepository;
    private final ReviewRepository reviewRepository;

    @Transactional
    public ReportResponse createReport(User reporter, ReportRequest request) {
        Review review = reviewRepository.findById(request.getReviewId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 리뷰입니다"));

        // 자신의 리뷰는 신고할 수 없음
        if (review.getUser().getId().equals(reporter.getId())) {
            throw new IllegalArgumentException("자신의 리뷰는 신고할 수 없습니다");
        }

        // 이미 신고한 리뷰인지 확인
        if (reportRepository.existsByReviewIdAndReporterId(request.getReviewId(), reporter.getId())) {
            throw new IllegalArgumentException("이미 신고한 리뷰입니다");
        }

        Report report = Report.builder()
                .review(review)
                .reporter(reporter)
                .reason(request.getReason())
                .description(request.getDescription())
                .build();

        return ReportResponse.from(reportRepository.save(report));
    }
}
