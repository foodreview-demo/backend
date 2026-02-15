package com.foodreview.domain.gathering.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Service
public class PortoneService {

    @Value("${portone.secret-key}")
    private String secretKey;

    private WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        this.webClient = WebClient.builder()
                .baseUrl("https://api.portone.io")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "PortOne " + secretKey)
                .build();
    }

    /**
     * 결제 정보 조회
     */
    public PaymentInfo getPayment(String paymentId) {
        try {
            String response = webClient.get()
                    .uri("/payments/{paymentId}", paymentId)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);
            String status = root.path("status").asText();
            BigDecimal amount = new BigDecimal(root.path("amount").path("total").asText("0"));

            return new PaymentInfo(paymentId, status, amount);
        } catch (Exception e) {
            log.error("Failed to get payment info: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 결제 검증 - 금액이 일치하는지 확인
     */
    public boolean verifyPayment(String paymentId, BigDecimal expectedAmount) {
        try {
            PaymentInfo payment = getPayment(paymentId);
            if (payment == null) {
                log.error("Payment not found for paymentId: {}", paymentId);
                return false;
            }

            // 결제 상태 확인
            if (!"PAID".equals(payment.status())) {
                log.error("Payment status is not 'PAID': {}", payment.status());
                return false;
            }

            // 금액 확인
            if (payment.amount().compareTo(expectedAmount) != 0) {
                log.error("Amount mismatch. Expected: {}, Paid: {}", expectedAmount, payment.amount());
                return false;
            }

            return true;
        } catch (Exception e) {
            log.error("Failed to verify payment: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 결제 취소 (환금)
     */
    public CancelResult cancelPayment(String paymentId, BigDecimal amount, String reason) {
        try {
            Map<String, Object> requestBody = Map.of(
                    "amount", amount,
                    "reason", reason
            );

            String response = webClient.post()
                    .uri("/payments/{paymentId}/cancel", paymentId)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);
            String status = root.path("cancellation").path("status").asText();

            if ("SUCCEEDED".equals(status)) {
                log.info("Payment cancelled successfully. paymentId: {}, amount: {}", paymentId, amount);
                return new CancelResult(true, null);
            } else {
                String errorMessage = root.path("message").asText("Unknown error");
                log.error("Failed to cancel payment. paymentId: {}, message: {}", paymentId, errorMessage);
                return new CancelResult(false, errorMessage);
            }
        } catch (Exception e) {
            log.error("Failed to cancel payment: {}", e.getMessage(), e);
            return new CancelResult(false, e.getMessage());
        }
    }

    /**
     * 부분 환금
     */
    public CancelResult partialCancel(String paymentId, BigDecimal cancelAmount, String reason) {
        return cancelPayment(paymentId, cancelAmount, reason);
    }

    public record PaymentInfo(String paymentId, String status, BigDecimal amount) {}
    public record CancelResult(boolean success, String errorMessage) {}
}
