package com.foodreview.domain.user.dto;

import com.foodreview.domain.user.entity.ScoreEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

public class ScoreEventDto {

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String type;
        private String description;
        private Integer points;
        private LocalDateTime date;
        private FromUserInfo from;

        public static Response from(ScoreEvent event) {
            FromUserInfo fromInfo = null;
            if (event.getFromUser() != null) {
                fromInfo = FromUserInfo.builder()
                        .name(event.getFromUser().getName())
                        .score(event.getFromUser().getTasteScore())
                        .build();
            }

            return Response.builder()
                    .id(event.getId())
                    .type(event.getType().name().toLowerCase())
                    .description(event.getDescription())
                    .points(event.getPoints())
                    .date(event.getCreatedAt())
                    .from(fromInfo)
                    .build();
        }
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class FromUserInfo {
        private String name;
        private Integer score;
    }
}
