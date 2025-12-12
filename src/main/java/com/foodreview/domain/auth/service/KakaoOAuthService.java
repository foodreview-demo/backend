package com.foodreview.domain.auth.service;

import com.foodreview.domain.auth.dto.AuthDto;
import com.foodreview.domain.auth.dto.KakaoOAuthDto;
import com.foodreview.domain.user.entity.AuthProvider;
import com.foodreview.domain.user.entity.User;
import com.foodreview.domain.user.repository.UserRepository;
import com.foodreview.global.exception.CustomException;
import com.foodreview.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoOAuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final WebClient webClient = WebClient.create();

    @Value("${oauth.kakao.client-id}")
    private String clientId;

    @Value("${oauth.kakao.client-secret}")
    private String clientSecret;

    @Value("${oauth.kakao.redirect-uri}")
    private String redirectUri;

    private static final String KAKAO_TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    @Transactional
    public AuthDto.TokenResponse loginWithKakao(String code) {
        // 1. 카카오에서 access token 받기
        KakaoOAuthDto.TokenResponse kakaoToken = getKakaoToken(code);

        // 2. 카카오에서 사용자 정보 받기
        KakaoOAuthDto.UserInfoResponse userInfo = getKakaoUserInfo(kakaoToken.getAccessToken());

        // 3. 사용자 조회 또는 생성
        User user = findOrCreateUser(userInfo);

        // 4. JWT 토큰 발급
        String accessToken = jwtTokenProvider.createAccessToken(user.getEmail());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getEmail());

        return AuthDto.TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtTokenProvider.getAccessTokenExpiration())
                .build();
    }

    private KakaoOAuthDto.TokenResponse getKakaoToken(String code) {
        try {
            return webClient.post()
                    .uri(KAKAO_TOKEN_URL)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                    .body(BodyInserters.fromFormData("grant_type", "authorization_code")
                            .with("client_id", clientId)
                            .with("client_secret", clientSecret)
                            .with("redirect_uri", redirectUri)
                            .with("code", code))
                    .retrieve()
                    .bodyToMono(KakaoOAuthDto.TokenResponse.class)
                    .block();
        } catch (Exception e) {
            log.error("카카오 토큰 요청 실패", e);
            throw new CustomException("카카오 인증에 실패했습니다", HttpStatus.UNAUTHORIZED, "KAKAO_AUTH_FAILED");
        }
    }

    private KakaoOAuthDto.UserInfoResponse getKakaoUserInfo(String accessToken) {
        try {
            KakaoOAuthDto.UserInfoResponse response = webClient.get()
                    .uri(KAKAO_USER_INFO_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(KakaoOAuthDto.UserInfoResponse.class)
                    .block();

            // 디버그 로그
            log.info("카카오 사용자 정보: id={}", response.getId());
            if (response.getKakaoAccount() != null) {
                KakaoOAuthDto.KakaoAccount account = response.getKakaoAccount();
                log.info("카카오 계정: email={}", account.getEmail());
                if (account.getProfile() != null) {
                    log.info("카카오 프로필: nickname={}, profileImageUrl={}",
                            account.getProfile().getNickname(),
                            account.getProfile().getProfileImageUrl());
                } else {
                    log.warn("카카오 프로필이 null입니다");
                }
            } else {
                log.warn("카카오 계정 정보가 null입니다");
            }

            return response;
        } catch (Exception e) {
            log.error("카카오 사용자 정보 요청 실패", e);
            throw new CustomException("카카오 사용자 정보를 가져오는데 실패했습니다", HttpStatus.UNAUTHORIZED, "KAKAO_USER_INFO_FAILED");
        }
    }

    private User findOrCreateUser(KakaoOAuthDto.UserInfoResponse userInfo) {
        String kakaoId = String.valueOf(userInfo.getId());
        KakaoOAuthDto.KakaoAccount account = userInfo.getKakaoAccount();

        // 이메일, 이름, 아바타 추출 (account가 null일 수 있음)
        String email = null;
        String name = "사용자";
        String avatar = null;

        if (account != null) {
            email = account.getEmail();

            // 프로필 정보 추출
            if (account.getProfile() != null) {
                if (account.getProfile().getNickname() != null) {
                    name = account.getProfile().getNickname();
                }
                if (account.getProfile().getProfileImageUrl() != null) {
                    avatar = account.getProfile().getProfileImageUrl();
                }
            }
        }

        // 1. providerId로 기존 카카오 연동 사용자 조회 (프로필 동기화 안함 - 최초 가입 시에만 적용)
        Optional<User> existingKakaoUser = userRepository.findByProviderAndProviderId(AuthProvider.KAKAO, kakaoId);
        if (existingKakaoUser.isPresent()) {
            log.info("기존 카카오 연동 사용자 로그인: {}", existingKakaoUser.get().getEmail());
            return existingKakaoUser.get();
        }

        // 2. 이메일로 기존 회원 조회 - 있으면 카카오 계정 연동 (이름, 프로필 사진 동기화)
        if (email != null) {
            Optional<User> existingUserByEmail = userRepository.findByEmail(email);
            if (existingUserByEmail.isPresent()) {
                User existingUser = existingUserByEmail.get();
                existingUser.linkKakaoAccount(kakaoId, name, avatar);
                log.info("기존 회원에 카카오 계정 연동 완료: {}", email);
                return existingUser;
            }
        }

        // 3. 신규 회원 - 자동 회원가입
        // 이메일이 없는 경우 임시 이메일 생성
        if (email == null) {
            email = "kakao_" + kakaoId + "@foodreview.local";
        }

        User newUser = User.builder()
                .email(email)
                .password(UUID.randomUUID().toString()) // 임의 패스워드 (OAuth 사용자는 패스워드 로그인 불가)
                .name(name)
                .avatar(avatar)
                .region("서울") // 기본값, 나중에 프로필에서 수정 가능
                .provider(AuthProvider.KAKAO)
                .providerId(kakaoId)
                .build();

        log.info("카카오 신규 회원가입 완료: {}", email);
        return userRepository.save(newUser);
    }
}
