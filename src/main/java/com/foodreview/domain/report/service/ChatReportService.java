package com.foodreview.domain.report.service;

import com.foodreview.domain.chat.entity.ChatRoom;
import com.foodreview.domain.chat.repository.ChatRoomRepository;
import com.foodreview.domain.report.dto.ChatReportDto;
import com.foodreview.domain.report.entity.ChatReport;
import com.foodreview.domain.report.entity.ReportStatus;
import com.foodreview.domain.report.repository.ChatReportRepository;
import com.foodreview.domain.user.entity.User;
import com.foodreview.domain.user.repository.UserRepository;
import com.foodreview.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatReportService {

    private final ChatReportRepository chatReportRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;

    @Transactional
    public ChatReportDto.Response createReport(Long reporterId, ChatReportDto.CreateRequest request) {
        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new CustomException("사용자를 찾을 수 없습니다", HttpStatus.NOT_FOUND));

        User reportedUser = userRepository.findById(request.getReportedUserId())
                .orElseThrow(() -> new CustomException("신고 대상 사용자를 찾을 수 없습니다", HttpStatus.NOT_FOUND));

        ChatRoom chatRoom = chatRoomRepository.findById(request.getChatRoomId())
                .orElseThrow(() -> new CustomException("채팅방을 찾을 수 없습니다", HttpStatus.NOT_FOUND));

        // 자기 자신은 신고 불가
        if (reporterId.equals(request.getReportedUserId())) {
            throw new CustomException("자기 자신은 신고할 수 없습니다", HttpStatus.BAD_REQUEST);
        }

        // 채팅방 멤버인지 확인
        if (!chatRoom.isMember(reporterId)) {
            throw new CustomException("해당 채팅방의 멤버가 아닙니다", HttpStatus.FORBIDDEN);
        }

        // 이미 같은 채팅방에서 같은 사용자를 신고했는지 확인 (대기 중인 신고만 체크)
        if (chatReportRepository.existsByReporterIdAndChatRoomIdAndReportedUserIdAndStatus(
                reporterId, request.getChatRoomId(), request.getReportedUserId(), ReportStatus.PENDING)) {
            throw new CustomException("이미 처리 대기 중인 신고가 있습니다", HttpStatus.CONFLICT);
        }

        ChatReport report = ChatReport.builder()
                .reporter(reporter)
                .reportedUser(reportedUser)
                .chatRoom(chatRoom)
                .messageId(request.getMessageId())
                .messageContent(request.getMessageContent())
                .reason(request.getReason())
                .description(request.getDescription())
                .build();

        ChatReport saved = chatReportRepository.save(report);
        log.info("채팅 신고 생성: reporterId={}, reportedUserId={}, chatRoomId={}, reason={}",
                reporterId, request.getReportedUserId(), request.getChatRoomId(), request.getReason());

        return ChatReportDto.Response.from(saved);
    }
}
