package com.foodreview.domain.report.dto;

import com.foodreview.domain.report.entity.ReportReason;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReportRequest {

    @NotNull(message = "리뷰 ID는 필수입니다")
    private Long reviewId;

    @NotNull(message = "신고 사유는 필수입니다")
    private ReportReason reason;

    private String description;
}
