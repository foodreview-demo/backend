package com.foodreview.global.util;

import org.springframework.web.util.HtmlUtils;

/**
 * HTML/XSS 새니타이저 유틸리티
 *
 * 사용자 입력에서 XSS 공격을 방지하기 위한 HTML 이스케이프 처리
 */
public class HtmlSanitizer {

    private HtmlSanitizer() {
        // 유틸리티 클래스 - 인스턴스화 방지
    }

    /**
     * HTML 특수문자 이스케이프
     *
     * < > & " ' 등을 HTML 엔티티로 변환
     * 예: <script> → &lt;script&gt;
     */
    public static String sanitize(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        return HtmlUtils.htmlEscape(input);
    }

    /**
     * HTML 이스케이프 해제 (디코딩)
     *
     * &lt; &gt; 등을 원래 문자로 복원
     */
    public static String unescape(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        return HtmlUtils.htmlUnescape(input);
    }

    /**
     * 스크립트 태그 제거
     *
     * 정규식으로 <script>...</script> 태그 완전 제거
     */
    public static String removeScriptTags(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        // <script> 태그 제거 (대소문자 무시, 속성 포함)
        String result = input.replaceAll("(?i)<script[^>]*>.*?</script>", "");

        // 인라인 이벤트 핸들러 제거 (onclick, onload 등)
        result = result.replaceAll("(?i)\\s+on\\w+\\s*=\\s*[\"'][^\"']*[\"']", "");
        result = result.replaceAll("(?i)\\s+on\\w+\\s*=\\s*[^\\s>]+", "");

        // javascript: 프로토콜 제거
        result = result.replaceAll("(?i)javascript\\s*:", "");

        return result;
    }

    /**
     * 모든 HTML 태그 제거 (텍스트만 추출)
     */
    public static String stripAllTags(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        return input.replaceAll("<[^>]*>", "");
    }

    /**
     * 채팅 메시지용 새니타이저
     *
     * 기본 텍스트만 허용하고 HTML/스크립트 모두 제거
     * 단, 줄바꿈(\n)은 보존 (답장 기능에서 사용)
     */
    public static String sanitizeChatMessage(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }

        // 1. 줄바꿈 보존을 위해 임시 치환
        String result = message.replace("\n", "{{NEWLINE}}");

        // 2. 모든 HTML 태그 제거
        result = stripAllTags(result);

        // 3. 스크립트 태그 제거
        result = removeScriptTags(result);

        // 4. 줄바꿈 복원
        result = result.replace("{{NEWLINE}}", "\n");

        // 5. 앞뒤 공백 제거 (줄바꿈은 유지)
        result = result.trim();

        return result;
    }

    /**
     * 리뷰 콘텐츠용 새니타이저
     *
     * 줄바꿈은 허용하되 HTML 태그는 제거
     */
    public static String sanitizeReviewContent(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }

        // 1. 줄바꿈 보존을 위해 임시 치환
        String result = content.replace("\n", "{{NEWLINE}}");

        // 2. 스크립트 태그 제거
        result = removeScriptTags(result);

        // 3. 모든 HTML 태그 제거
        result = stripAllTags(result);

        // 4. HTML 특수문자 이스케이프
        result = sanitize(result);

        // 5. 줄바꿈 복원
        result = result.replace("{{NEWLINE}}", "\n");

        return result.trim();
    }
}
