package com.foodreview.domain.report.controller;

import com.foodreview.domain.report.dto.ReportRequest;
import com.foodreview.domain.report.dto.ReportResponse;
import com.foodreview.domain.report.service.ReportService;
import com.foodreview.global.common.ApiResponse;
import com.foodreview.global.security.CurrentUser;
import com.foodreview.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    public ResponseEntity<ApiResponse<ReportResponse>> createReport(
            @CurrentUser CustomUserDetails userDetails,
            @Valid @RequestBody ReportRequest request) {
        ReportResponse response = reportService.createReport(userDetails.getUser(), request);
        return ResponseEntity.ok(ApiResponse.success(response, "신고가 접수되었습니다"));
    }
}
