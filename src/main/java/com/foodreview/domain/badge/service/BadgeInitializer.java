package com.foodreview.domain.badge.service;

import com.foodreview.domain.badge.entity.Badge;
import com.foodreview.domain.badge.entity.BadgeCategory;
import com.foodreview.domain.badge.entity.BadgeConditionType;
import com.foodreview.domain.badge.repository.BadgeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BadgeInitializer implements ApplicationRunner {

    private final BadgeRepository badgeRepository;
    private final BadgeService badgeService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // 이미 배지가 있으면 배지 생성 스킵
        boolean badgesExist = badgeRepository.count() > 0;

        if (badgesExist) {
            log.info("Badges already initialized, skipping badge creation...");
        } else {
            log.info("Initializing badges...");
            initializeBadges();
        }

        // 기존 사용자들에게 배지 마이그레이션 (항상 실행 - 중복 지급 방지 로직 있음)
        badgeService.migrateExistingUserBadges();
    }

    private void initializeBadges() {
        log.info("Creating badge definitions...");

        List<Badge> badges = List.of(
                // === 등급 배지 (GRADE) ===
                Badge.builder()
                        .code("GRADE_BEGINNER")
                        .name("입문자")
                        .description("맛잘알의 첫 걸음을 내딛었습니다")
                        .icon("🌱")
                        .category(BadgeCategory.GRADE)
                        .conditionType(BadgeConditionType.SCORE)
                        .conditionValue(0)
                        .sortOrder(1)
                        .build(),
                Badge.builder()
                        .code("GRADE_EXPLORER")
                        .name("탐험가")
                        .description("500점을 달성한 열정적인 탐험가")
                        .icon("🧭")
                        .category(BadgeCategory.GRADE)
                        .conditionType(BadgeConditionType.SCORE)
                        .conditionValue(500)
                        .sortOrder(2)
                        .build(),
                Badge.builder()
                        .code("GRADE_GOURMET")
                        .name("미식가")
                        .description("1000점을 달성한 진정한 미식가")
                        .icon("🍽️")
                        .category(BadgeCategory.GRADE)
                        .conditionType(BadgeConditionType.SCORE)
                        .conditionValue(1000)
                        .sortOrder(3)
                        .build(),
                Badge.builder()
                        .code("GRADE_EXPERT")
                        .name("전문가")
                        .description("1500점을 달성한 음식 전문가")
                        .icon("⭐")
                        .category(BadgeCategory.GRADE)
                        .conditionType(BadgeConditionType.SCORE)
                        .conditionValue(1500)
                        .sortOrder(4)
                        .build(),
                Badge.builder()
                        .code("GRADE_MASTER")
                        .name("마스터")
                        .description("2000점을 달성한 맛잘알 마스터")
                        .icon("👑")
                        .category(BadgeCategory.GRADE)
                        .conditionType(BadgeConditionType.SCORE)
                        .conditionValue(2000)
                        .sortOrder(5)
                        .build(),

                // === 도전과제 배지 (ACHIEVEMENT) - 리뷰 관련 ===
                Badge.builder()
                        .code("ACHIEVEMENT_FIRST_REVIEW")
                        .name("첫 발걸음")
                        .description("첫 리뷰를 작성했습니다")
                        .icon("🎉")
                        .category(BadgeCategory.ACHIEVEMENT)
                        .conditionType(BadgeConditionType.FIRST_REVIEW)
                        .conditionValue(1)
                        .sortOrder(10)
                        .build(),
                Badge.builder()
                        .code("ACHIEVEMENT_REVIEWER_10")
                        .name("리뷰어")
                        .description("10개의 리뷰를 작성했습니다")
                        .icon("✍️")
                        .category(BadgeCategory.ACHIEVEMENT)
                        .conditionType(BadgeConditionType.REVIEW_COUNT)
                        .conditionValue(10)
                        .sortOrder(11)
                        .build(),
                Badge.builder()
                        .code("ACHIEVEMENT_REVIEWER_50")
                        .name("열정 리뷰어")
                        .description("50개의 리뷰를 작성했습니다")
                        .icon("📝")
                        .category(BadgeCategory.ACHIEVEMENT)
                        .conditionType(BadgeConditionType.REVIEW_COUNT)
                        .conditionValue(50)
                        .sortOrder(12)
                        .build(),
                Badge.builder()
                        .code("ACHIEVEMENT_REVIEWER_100")
                        .name("리뷰 마스터")
                        .description("100개의 리뷰를 작성했습니다")
                        .icon("🏆")
                        .category(BadgeCategory.ACHIEVEMENT)
                        .conditionType(BadgeConditionType.REVIEW_COUNT)
                        .conditionValue(100)
                        .sortOrder(13)
                        .build(),

                // === 도전과제 배지 (ACHIEVEMENT) - 공감 관련 ===
                Badge.builder()
                        .code("ACHIEVEMENT_SYMPATHY_10")
                        .name("공감받는 사람")
                        .description("10개의 공감을 받았습니다")
                        .icon("💕")
                        .category(BadgeCategory.ACHIEVEMENT)
                        .conditionType(BadgeConditionType.RECEIVED_SYMPATHY)
                        .conditionValue(10)
                        .sortOrder(20)
                        .build(),
                Badge.builder()
                        .code("ACHIEVEMENT_SYMPATHY_100")
                        .name("공감왕")
                        .description("100개의 공감을 받았습니다")
                        .icon("❤️")
                        .category(BadgeCategory.ACHIEVEMENT)
                        .conditionType(BadgeConditionType.RECEIVED_SYMPATHY)
                        .conditionValue(100)
                        .sortOrder(21)
                        .build(),

                // === 도전과제 배지 (ACHIEVEMENT) - 소셜 관련 ===
                Badge.builder()
                        .code("ACHIEVEMENT_FIRST_FOLLOWER")
                        .name("첫 팔로워")
                        .description("첫 팔로워를 얻었습니다")
                        .icon("🤝")
                        .category(BadgeCategory.ACHIEVEMENT)
                        .conditionType(BadgeConditionType.FIRST_FOLLOWER)
                        .conditionValue(1)
                        .sortOrder(30)
                        .build(),
                Badge.builder()
                        .code("ACHIEVEMENT_FOLLOWER_10")
                        .name("인기인")
                        .description("10명의 팔로워를 달성했습니다")
                        .icon("🌟")
                        .category(BadgeCategory.ACHIEVEMENT)
                        .conditionType(BadgeConditionType.FOLLOWER_COUNT)
                        .conditionValue(10)
                        .sortOrder(31)
                        .build(),
                Badge.builder()
                        .code("ACHIEVEMENT_FOLLOWER_50")
                        .name("인플루언서")
                        .description("50명의 팔로워를 달성했습니다")
                        .icon("👥")
                        .category(BadgeCategory.ACHIEVEMENT)
                        .conditionType(BadgeConditionType.FOLLOWER_COUNT)
                        .conditionValue(50)
                        .sortOrder(32)
                        .build()
        );

        badgeRepository.saveAll(badges);
        log.info("Initialized {} badges", badges.size());
    }
}
