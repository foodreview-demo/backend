package com.foodreview.domain.gathering.service;

import com.foodreview.domain.gathering.entity.DepositStatus;
import com.foodreview.domain.gathering.entity.Gathering;
import com.foodreview.domain.gathering.entity.GatheringParticipant;
import com.foodreview.domain.gathering.entity.GatheringStatus;
import com.foodreview.domain.gathering.repository.GatheringParticipantRepository;
import com.foodreview.domain.gathering.repository.GatheringRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 번개모임 리마인더 스케줄러
 * - 모임 시작 1시간 전에 참여자들에게 알림 발송
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GatheringReminderScheduler {

    private final GatheringRepository gatheringRepository;
    private final GatheringParticipantRepository participantRepository;
    private final GatheringNotificationService gatheringNotificationService;

    /**
     * 5분마다 실행 - 리마인더 알림 대상 조회 및 발송
     */
    @Scheduled(fixedRate = 300000) // 5분 = 300,000ms
    @Transactional
    public void sendReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourLater = now.plusHours(1);

        List<GatheringStatus> targetStatuses = Arrays.asList(
            GatheringStatus.RECRUITING,
            GatheringStatus.CONFIRMED
        );

        List<Gathering> gatherings = gatheringRepository.findGatheringsForReminder(
            targetStatuses, now, oneHourLater);

        if (gatherings.isEmpty()) {
            return;
        }

        log.info("Sending reminders for {} gatherings", gatherings.size());

        for (Gathering gathering : gatherings) {
            try {
                // 보증금 납부 완료한 참여자 조회
                List<GatheringParticipant> participants = participantRepository
                    .findByGatheringIdAndDepositStatus(gathering.getId(), DepositStatus.DEPOSITED);

                for (GatheringParticipant participant : participants) {
                    gatheringNotificationService.sendGatheringReminderNotification(
                        participant.getUser(), gathering);
                }

                // 리마인더 발송 완료 표시
                gathering.markReminderSent();

                log.info("Reminder sent for gathering: uuid={}, participants={}",
                    gathering.getUuid(), participants.size());

            } catch (Exception e) {
                log.error("Failed to send reminder for gathering: uuid={}", gathering.getUuid(), e);
            }
        }
    }
}
