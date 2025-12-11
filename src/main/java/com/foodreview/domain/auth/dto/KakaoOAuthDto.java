package com.foodreview.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class KakaoOAuthDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenRequest {
        private String code;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenResponse {
        @JsonProperty("access_token")
        private String accessToken;

        @JsonProperty("token_type")
        private String tokenType;

        @JsonProperty("refresh_token")
        private String refreshToken;

        @JsonProperty("expires_in")
        private Integer expiresIn;

        @JsonProperty("scope")
        private String scope;

        @JsonProperty("refresh_token_expires_in")
        private Integer refreshTokenExpiresIn;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfoResponse {
        private Long id;

        @JsonProperty("connected_at")
        private String connectedAt;

        @JsonProperty("kakao_account")
        private KakaoAccount kakaoAccount;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KakaoAccount {
        private String email;

        @JsonProperty("email_needs_agreement")
        private Boolean emailNeedsAgreement;

        @JsonProperty("is_email_valid")
        private Boolean isEmailValid;

        @JsonProperty("is_email_verified")
        private Boolean isEmailVerified;

        private Profile profile;

        @JsonProperty("profile_nickname_needs_agreement")
        private Boolean profileNicknameNeedsAgreement;

        @JsonProperty("profile_image_needs_agreement")
        private Boolean profileImageNeedsAgreement;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Profile {
        private String nickname;

        @JsonProperty("thumbnail_image_url")
        private String thumbnailImageUrl;

        @JsonProperty("profile_image_url")
        private String profileImageUrl;

        @JsonProperty("is_default_image")
        private Boolean isDefaultImage;
    }
}
