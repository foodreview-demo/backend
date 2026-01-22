package com.foodreview.domain.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendationBatchScheduler {

    private final JobLauncher jobLauncher;
    private final Job recommendationJob;

    /**
     * 매 시간 정각에 추천 배치 실행
     * cron: 초 분 시 일 월 요일
     */
    @Scheduled(cron = "0 0 * * * *")
    public void runRecommendationJob() {
        try {
            log.info("Starting recommendation batch job...");

            JobParameters params = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(recommendationJob, params);

            log.info("Recommendation batch job completed successfully");
        } catch (Exception e) {
            log.error("Recommendation batch job failed", e);
        }
    }

    /**
     * 서버 시작 후 5분 뒤에 초기 배치 실행 (기존 캐시가 없을 때를 대비)
     */
    @Scheduled(initialDelay = 300000, fixedDelay = Long.MAX_VALUE)
    public void runInitialBatch() {
        try {
            log.info("Running initial recommendation batch...");

            JobParameters params = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .addString("type", "initial")
                    .toJobParameters();

            jobLauncher.run(recommendationJob, params);

            log.info("Initial recommendation batch completed");
        } catch (Exception e) {
            log.error("Initial recommendation batch failed", e);
        }
    }
}
