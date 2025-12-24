package com.foodreview.global.util;

/**
 * 민감 정보 마스킹 유틸리티
 *
 * 로그에 기록되는 개인정보를 마스킹하여 보안 강화
 */
public class MaskingUtil {

    private MaskingUtil() {
        // 유틸리티 클래스 - 인스턴스화 방지
    }

    /**
     * 이메일 마스킹
     * example@domain.com → exa***@domain.com
     */
    public static String maskEmail(String email) {
        if (email == null || email.isEmpty()) {
            return email;
        }

        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return "***";
        }

        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex);

        if (localPart.length() <= 3) {
            return localPart.charAt(0) + "***" + domain;
        }

        return localPart.substring(0, 3) + "***" + domain;
    }

    /**
     * 전화번호 마스킹
     * 010-1234-5678 → 010-****-5678
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.isEmpty()) {
            return phone;
        }

        // 숫자만 추출
        String digits = phone.replaceAll("[^0-9]", "");

        if (digits.length() < 7) {
            return "***";
        }

        // 마지막 4자리만 표시
        return digits.substring(0, 3) + "-****-" + digits.substring(digits.length() - 4);
    }

    /**
     * 이름 마스킹
     * 홍길동 → 홍*동
     * John → J**n
     */
    public static String maskName(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }

        if (name.length() == 1) {
            return "*";
        }

        if (name.length() == 2) {
            return name.charAt(0) + "*";
        }

        // 첫 글자와 마지막 글자만 표시
        StringBuilder masked = new StringBuilder();
        masked.append(name.charAt(0));
        for (int i = 1; i < name.length() - 1; i++) {
            masked.append('*');
        }
        masked.append(name.charAt(name.length() - 1));
        return masked.toString();
    }

    /**
     * IP 주소 마스킹
     * 192.168.1.100 → 192.168.***.***
     */
    public static String maskIp(String ip) {
        if (ip == null || ip.isEmpty()) {
            return ip;
        }

        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            return "***";
        }

        return parts[0] + "." + parts[1] + ".***.***";
    }

    /**
     * 사용자 ID 마스킹 (로그용)
     * User ID만 표시 (이메일 대신)
     */
    public static String maskUserId(Long userId) {
        if (userId == null) {
            return "unknown";
        }
        return "user#" + userId;
    }
}
