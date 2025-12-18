package com.foodreview.domain.review.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.foodreview.domain.review.entity.Comment;
import com.foodreview.domain.user.dto.UserDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

public class CommentDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        @NotBlank(message = "댓글 내용을 입력해주세요")
        @Size(max = 500, message = "댓글은 500자 이하로 입력해주세요")
        private String content;

        private Long parentId; // 대댓글인 경우 부모 댓글 ID
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        @NotBlank(message = "댓글 내용을 입력해주세요")
        @Size(max = 500, message = "댓글은 500자 이하로 입력해주세요")
        private String content;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private Long reviewId;
        private UserDto.SimpleResponse user;
        private String content;
        private Long parentId;
        private int replyCount; // 대댓글 수
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        @JsonProperty("isMine")
        private boolean mine; // 본인 댓글 여부

        @JsonProperty("isDeleted")
        private boolean deleted; // 삭제 여부

        public static Response from(Comment comment, Long currentUserId, int replyCount) {
            return Response.builder()
                    .id(comment.getId())
                    .reviewId(comment.getReview().getId())
                    .user(UserDto.SimpleResponse.from(comment.getUser()))
                    .content(comment.getContent())
                    .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                    .replyCount(replyCount)
                    .createdAt(comment.getCreatedAt())
                    .updatedAt(comment.getUpdatedAt())
                    .mine(comment.getUser().getId().equals(currentUserId))
                    .deleted(comment.getIsDeleted())
                    .build();
        }

        public static Response from(Comment comment, Long currentUserId) {
            return from(comment, currentUserId, 0);
        }
    }
}
