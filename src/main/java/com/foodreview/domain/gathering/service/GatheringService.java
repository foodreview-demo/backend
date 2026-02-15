package com.foodreview.domain.gathering.service;

import com.foodreview.domain.chat.entity.ChatRoom;
import com.foodreview.domain.chat.entity.ChatRoomMember;
import com.foodreview.domain.chat.repository.ChatRoomMemberRepository;
import com.foodreview.domain.chat.repository.ChatRoomRepository;
import com.foodreview.domain.gathering.dto.GatheringDto;
import com.foodreview.domain.gathering.entity.*;
import com.foodreview.domain.gathering.repository.GatheringDepositTransactionRepository;
import com.foodreview.domain.gathering.repository.GatheringParticipantRepository;
import com.foodreview.domain.gathering.repository.GatheringRepository;
import com.foodreview.domain.restaurant.entity.Restaurant;
import com.foodreview.domain.restaurant.repository.RestaurantRepository;
import com.foodreview.domain.user.entity.User;
import com.foodreview.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GatheringService {

    private final GatheringRepository gatheringRepository;
    private final GatheringParticipantRepository participantRepository;
    private final GatheringDepositTransactionRepository transactionRepository;
    private final RestaurantRepository restaurantRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final PortoneService portoneService;

    /**
     * 모임 생성
     */
    @Transactional
    public GatheringDto.Response createGathering(GatheringDto.CreateRequest request, User creator) {
        Restaurant restaurant = restaurantRepository.findById(request.getRestaurantId())
                .orElseThrow(() -> new CustomException("음식점을 찾을 수 없습니다", HttpStatus.NOT_FOUND, "RESTAURANT_NOT_FOUND"));

        // 모임 생성
        Gathering gathering = Gathering.builder()
                .restaurant(restaurant)
                .creator(creator)
                .title(request.getTitle())
                .description(request.getDescription())
                .targetTime(request.getTargetTime())
                .maxParticipants(request.getMaxParticipants())
                .depositAmount(request.getDepositAmount())
                .refundType(request.getRefundType())
                .build();

        gathering = gatheringRepository.save(gathering);

        // 그룹 채팅방 생성
        ChatRoom chatRoom = ChatRoom.builder()
                .name(gathering.getTitle())
                .roomType(ChatRoom.RoomType.GROUP)
                .build();
        chatRoom = chatRoomRepository.save(chatRoom);

        gathering.setChatRoomUuid(chatRoom.getUuid());

        // 호스트를 채팅방 멤버로 추가
        ChatRoomMember creatorMember = ChatRoomMember.builder()
                .chatRoom(chatRoom)
                .user(creator)
                .build();
        chatRoomMemberRepository.save(creatorMember);

        // 호스트를 참여자로 추가 (보증금은 면제 - 즉시 DEPOSITED 처리)
        GatheringParticipant hostParticipant = GatheringParticipant.builder()
                .gathering(gathering)
                .user(creator)
                .depositStatus(DepositStatus.DEPOSITED)
                .joinedAt(LocalDateTime.now())
                .build();
        participantRepository.save(hostParticipant);

        return GatheringDto.Response.from(gathering, creator.getId());
    }

    /**
     * 모임 상세 조회
     */
    public GatheringDto.DetailResponse getGathering(String uuid, Long currentUserId) {
        Gathering gathering = gatheringRepository.findByUuid(uuid)
                .orElseThrow(() -> new CustomException("모임을 찾을 수 없습니다", HttpStatus.NOT_FOUND, "GATHERING_NOT_FOUND"));

        GatheringParticipant myParticipation = null;
        if (currentUserId != null) {
            myParticipation = participantRepository.findByGatheringIdAndUserId(gathering.getId(), currentUserId)
                    .orElse(null);
        }

        List<GatheringParticipant> allParticipants = participantRepository.findByGatheringId(gathering.getId());

        return GatheringDto.DetailResponse.from(gathering, currentUserId, myParticipation, allParticipants);
    }

    /**
     * 음식점별 모임 조회
     */
    public List<GatheringDto.Response> getGatheringsByRestaurant(Long restaurantId, Long currentUserId) {
        List<GatheringStatus> activeStatuses = Arrays.asList(
                GatheringStatus.RECRUITING,
                GatheringStatus.CONFIRMED
        );

        List<Gathering> gatherings = gatheringRepository.findActiveByRestaurantId(
                restaurantId, activeStatuses, LocalDateTime.now());

        return gatherings.stream()
                .map(g -> {
                    GatheringParticipant myParticipation = null;
                    if (currentUserId != null) {
                        myParticipation = participantRepository.findByGatheringIdAndUserId(g.getId(), currentUserId)
                                .orElse(null);
                    }
                    return GatheringDto.Response.from(g, currentUserId, myParticipation);
                })
                .toList();
    }

    /**
     * 지역별 모임 조회
     */
    public Page<GatheringDto.Response> getGatheringsByRegion(String region, String district, Long currentUserId, Pageable pageable) {
        List<GatheringStatus> activeStatuses = Arrays.asList(
                GatheringStatus.RECRUITING,
                GatheringStatus.CONFIRMED
        );

        Page<Gathering> gatherings = gatheringRepository.findByRegionAndStatus(
                region, district, activeStatuses, LocalDateTime.now(), pageable);

        return gatherings.map(g -> {
            GatheringParticipant myParticipation = null;
            if (currentUserId != null) {
                myParticipation = participantRepository.findByGatheringIdAndUserId(g.getId(), currentUserId)
                        .orElse(null);
            }
            return GatheringDto.Response.from(g, currentUserId, myParticipation);
        });
    }

    /**
     * 내가 생성한 모임 조회
     */
    public Page<GatheringDto.Response> getMyCreatedGatherings(Long userId, Pageable pageable) {
        Page<Gathering> gatherings = gatheringRepository.findByCreatorId(userId, pageable);
        return gatherings.map(g -> GatheringDto.Response.from(g, userId));
    }

    /**
     * 내가 참여한 모임 조회
     */
    public Page<GatheringDto.Response> getMyJoinedGatherings(Long userId, Pageable pageable) {
        Page<Gathering> gatherings = gatheringRepository.findByParticipantUserId(userId, pageable);
        return gatherings.map(g -> {
            GatheringParticipant myParticipation = participantRepository.findByGatheringIdAndUserId(g.getId(), userId)
                    .orElse(null);
            return GatheringDto.Response.from(g, userId, myParticipation);
        });
    }

    /**
     * 모임 참여 (결제 전 예약)
     */
    @Transactional
    public GatheringDto.ParticipantInfo joinGathering(String uuid, User user) {
        Gathering gathering = gatheringRepository.findByUuid(uuid)
                .orElseThrow(() -> new CustomException("모임을 찾을 수 없습니다", HttpStatus.NOT_FOUND, "GATHERING_NOT_FOUND"));

        // 참여 가능 여부 체크
        if (!gathering.canJoin()) {
            throw new CustomException("참여할 수 없는 모임입니다", HttpStatus.BAD_REQUEST, "GATHERING_CANNOT_JOIN");
        }

        // 이미 참여했는지 체크
        if (participantRepository.existsByGatheringIdAndUserId(gathering.getId(), user.getId())) {
            throw new CustomException("이미 참여한 모임입니다", HttpStatus.BAD_REQUEST, "ALREADY_JOINED_GATHERING");
        }

        // 참여자 생성 (결제 대기 상태)
        GatheringParticipant participant = GatheringParticipant.builder()
                .gathering(gathering)
                .user(user)
                .depositStatus(DepositStatus.PENDING)
                .build();

        participant = participantRepository.save(participant);

        return GatheringDto.ParticipantInfo.from(participant);
    }

    /**
     * 결제 검증 및 참여 확정
     */
    @Transactional
    public GatheringDto.ParticipantInfo verifyDeposit(String uuid, GatheringDto.JoinRequest request, User user) {
        Gathering gathering = gatheringRepository.findByUuid(uuid)
                .orElseThrow(() -> new CustomException("모임을 찾을 수 없습니다", HttpStatus.NOT_FOUND, "GATHERING_NOT_FOUND"));

        GatheringParticipant participant = participantRepository.findByGatheringIdAndUserId(gathering.getId(), user.getId())
                .orElseThrow(() -> new CustomException("참여 정보를 찾을 수 없습니다", HttpStatus.NOT_FOUND, "PARTICIPANT_NOT_FOUND"));

        // 이미 결제 완료된 경우
        if (participant.isDeposited()) {
            throw new CustomException("이미 보증금을 결제했습니다", HttpStatus.BAD_REQUEST, "ALREADY_DEPOSITED");
        }

        // 포트원 V2 결제 검증
        boolean isValid = portoneService.verifyPayment(request.getImpUid(), gathering.getDepositAmount());
        if (!isValid) {
            throw new CustomException("결제 검증에 실패했습니다", HttpStatus.BAD_REQUEST, "PAYMENT_VERIFICATION_FAILED");
        }

        // 결제 확정
        participant.confirmDeposit(request.getImpUid(), request.getMerchantUid());
        gathering.addParticipant();

        // 거래 기록 생성
        GatheringDepositTransaction transaction = GatheringDepositTransaction.builder()
                .participant(participant)
                .amount(gathering.getDepositAmount())
                .transactionType(GatheringDepositTransaction.TransactionType.CHARGE)
                .status(GatheringDepositTransaction.TransactionStatus.COMPLETED)
                .impUid(request.getImpUid())
                .merchantUid(request.getMerchantUid())
                .build();
        transactionRepository.save(transaction);

        // 채팅방에 추가
        ChatRoom chatRoom = chatRoomRepository.findByUuid(gathering.getChatRoomUuid())
                .orElseThrow(() -> new CustomException("채팅방을 찾을 수 없습니다", HttpStatus.NOT_FOUND, "CHAT_ROOM_NOT_FOUND"));

        ChatRoomMember member = ChatRoomMember.builder()
                .chatRoom(chatRoom)
                .user(user)
                .build();
        chatRoomMemberRepository.save(member);

        return GatheringDto.ParticipantInfo.from(participant);
    }

    /**
     * 환금 처리 (호스트용)
     */
    @Transactional
    public void refundParticipant(String uuid, Long participantId, User host) {
        Gathering gathering = gatheringRepository.findByUuid(uuid)
                .orElseThrow(() -> new CustomException("모임을 찾을 수 없습니다", HttpStatus.NOT_FOUND, "GATHERING_NOT_FOUND"));

        // 호스트 권한 체크
        if (!gathering.isHost(host)) {
            throw new CustomException("모임 호스트만 환금할 수 있습니다", HttpStatus.FORBIDDEN, "NOT_GATHERING_HOST");
        }

        GatheringParticipant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new CustomException("참여 정보를 찾을 수 없습니다", HttpStatus.NOT_FOUND, "PARTICIPANT_NOT_FOUND"));

        if (!participant.needsRefund()) {
            throw new CustomException("환금이 필요하지 않습니다", HttpStatus.BAD_REQUEST, "REFUND_NOT_NEEDED");
        }

        processRefund(participant, gathering.getDepositAmount(), RefundType.MANUAL);
    }

    /**
     * 모임 완료 처리
     */
    @Transactional
    public void completeGathering(String uuid, User host) {
        Gathering gathering = gatheringRepository.findByUuid(uuid)
                .orElseThrow(() -> new CustomException("모임을 찾을 수 없습니다", HttpStatus.NOT_FOUND, "GATHERING_NOT_FOUND"));

        if (!gathering.isHost(host)) {
            throw new CustomException("모임 호스트만 완료할 수 있습니다", HttpStatus.FORBIDDEN, "NOT_GATHERING_HOST");
        }

        gathering.completeGathering();

        // 자동 환금인 경우 환금 처리
        if (gathering.isAutoRefund()) {
            processAutoRefund(gathering);
        }
    }

    /**
     * 모임 취소 처리
     */
    @Transactional
    public void cancelGathering(String uuid, User host) {
        Gathering gathering = gatheringRepository.findByUuid(uuid)
                .orElseThrow(() -> new CustomException("모임을 찾을 수 없습니다", HttpStatus.NOT_FOUND, "GATHERING_NOT_FOUND"));

        if (!gathering.isHost(host)) {
            throw new CustomException("모임 호스트만 취소할 수 있습니다", HttpStatus.FORBIDDEN, "NOT_GATHERING_HOST");
        }

        gathering.cancelGathering();

        // 모든 참여자에게 환금
        List<GatheringParticipant> participants = participantRepository.findByGatheringIdAndDepositStatus(
                gathering.getId(), DepositStatus.DEPOSITED);

        for (GatheringParticipant participant : participants) {
            if (!participant.isHost()) { // 호스트는 보증금이 없으므로 제외
                processRefund(participant, gathering.getDepositAmount(), RefundType.AUTO);
            }
        }
    }

    /**
     * 자동 환금 처리
     */
    private void processAutoRefund(Gathering gathering) {
        List<GatheringParticipant> participants = participantRepository.findByGatheringIdAndDepositStatus(
                gathering.getId(), DepositStatus.DEPOSITED);

        for (GatheringParticipant participant : participants) {
            if (!participant.isHost()) {
                processRefund(participant, gathering.getDepositAmount(), RefundType.AUTO);
            }
        }
    }

    /**
     * 환금 처리 공통 로직
     */
    private void processRefund(GatheringParticipant participant, BigDecimal amount, RefundType refundType) {
        PortoneService.CancelResult result = portoneService.cancelPayment(
                participant.getImpUid(),
                amount,
                "번개모임 보증금 환금"
        );

        // 거래 기록 생성
        GatheringDepositTransaction transaction = GatheringDepositTransaction.builder()
                .participant(participant)
                .amount(amount)
                .transactionType(GatheringDepositTransaction.TransactionType.REFUND)
                .refundType(refundType)
                .impUid(participant.getImpUid())
                .merchantUid(participant.getMerchantUid())
                .build();

        if (result.success()) {
            participant.markRefunded();
            transaction.complete(participant.getImpUid());
        } else {
            participant.markRefundFailed(result.errorMessage());
            transaction.fail(result.errorMessage());
        }

        transactionRepository.save(transaction);
    }

    /**
     * 모임 상태 변경 (호스트용)
     */
    @Transactional
    public GatheringDto.Response updateStatus(String uuid, GatheringStatus status, User host) {
        Gathering gathering = gatheringRepository.findByUuid(uuid)
                .orElseThrow(() -> new CustomException("모임을 찾을 수 없습니다", HttpStatus.NOT_FOUND, "GATHERING_NOT_FOUND"));

        if (!gathering.isHost(host)) {
            throw new CustomException("모임 호스트만 상태를 변경할 수 있습니다", HttpStatus.FORBIDDEN, "NOT_GATHERING_HOST");
        }

        switch (status) {
            case IN_PROGRESS -> gathering.startGathering();
            case COMPLETED -> {
                gathering.completeGathering();
                if (gathering.isAutoRefund()) {
                    processAutoRefund(gathering);
                }
            }
            case CANCELLED -> {
                gathering.cancelGathering();
                processAutoRefund(gathering);
            }
            default -> throw new CustomException("유효하지 않은 상태입니다", HttpStatus.BAD_REQUEST, "INVALID_GATHERING_STATUS");
        }

        return GatheringDto.Response.from(gathering, host.getId());
    }
}
