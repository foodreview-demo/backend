package com.foodreview.domain.recommendation.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 사용자 취향 프로필 배치 스케줄러
 * - 매일 새벽 3시에 TasteProfileJob 실행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TasteProfileScheduler {

    private final JobLauncher jobLauncher;
    private final Job tasteProfileJob;

    /**
     * 매일 새벽 3시에 실행
     * cron: 초 분 시 일 월 요일
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void runTasteProfileJob() {
        log.info("TasteProfile batch job started (scheduled)");
        runJob();
    }

    /**
     * 서버 시작 후 5분 뒤 초기 실행 (테스트 및 초기 데이터 생성용)
     */
    @Scheduled(initialDelay = 300000, fixedDelay = Long.MAX_VALUE)
    public void runInitialTasteProfileJob() {
        log.info("TasteProfile batch job started (initial run)");
        runJob();
    }

    private void runJob() {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(tasteProfileJob, params);
            log.info("TasteProfile batch job completed successfully");
        } catch (Exception e) {
            log.error("TasteProfile batch job failed", e);
        }
    }
}
