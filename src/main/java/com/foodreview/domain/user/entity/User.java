package com.foodreview.domain.user.entity;

import com.foodreview.domain.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 30)
    private String name;

    @Column(length = 500)
    private String avatar;

    @Column(nullable = false, length = 50)
    private String region;

    @Column(length = 50)
    private String district;

    @Column(length = 50)
    private String neighborhood;

    @Column(name = "taste_score", nullable = false)
    @Builder.Default
    private Integer tasteScore = 0;

    @Column(name = "review_count", nullable = false)
    @Builder.Default
    private Integer reviewCount = 0;

    @Column(name = "received_sympathy_count", nullable = false)
    @Builder.Default
    private Integer receivedSympathyCount = 0;

    @ElementCollection
    @CollectionTable(name = "user_favorite_categories", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "category")
    @Builder.Default
    private List<String> favoriteCategories = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Role role = Role.USER;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider")
    private AuthProvider provider;

    @Column(name = "provider_id")
    private String providerId;

    // 알림 설정
    @Column(name = "notify_reviews", nullable = false)
    @Builder.Default
    private Boolean notifyReviews = true;

    @Column(name = "notify_follows", nullable = false)
    @Builder.Default
    private Boolean notifyFollows = true;

    @Column(name = "notify_messages", nullable = false)
    @Builder.Default
    private Boolean notifyMessages = true;

    @Column(name = "notify_marketing", nullable = false)
    @Builder.Default
    private Boolean notifyMarketing = false;

    // 점수 추가
    public void addScore(int points) {
        this.tasteScore += points;
    }

    // 리뷰 카운트 증가
    public void incrementReviewCount() {
        this.reviewCount++;
    }

    // 받은 공감 수 증가
    public void incrementReceivedSympathyCount() {
        this.receivedSympathyCount++;
    }

    // 받은 공감 수 감소
    public void decrementReceivedSympathyCount() {
        if (this.receivedSympathyCount > 0) {
            this.receivedSympathyCount--;
        }
    }

    // 프로필 업데이트
    public void updateProfile(String name, String avatar, String region, String district, String neighborhood, List<String> favoriteCategories) {
        if (name != null) this.name = name;
        if (avatar != null) this.avatar = avatar;
        if (region != null) this.region = region;
        if (district != null) this.district = district;
        if (neighborhood != null) this.neighborhood = neighborhood;
        if (favoriteCategories != null) this.favoriteCategories = favoriteCategories;
    }

    // 카카오 계정 연동 (이름, 프로필 사진 동기화)
    public void linkKakaoAccount(String providerId, String name, String avatar) {
        this.provider = AuthProvider.KAKAO;
        this.providerId = providerId;
        if (name != null) {
            this.name = name;
        }
        if (avatar != null) {
            this.avatar = avatar;
        }
    }

    // 맛잘알 등급 계산
    public String getTasteGrade() {
        if (tasteScore >= 2000) return "마스터";
        if (tasteScore >= 1500) return "전문가";
        if (tasteScore >= 1000) return "미식가";
        if (tasteScore >= 500) return "탐험가";
        return "입문자";
    }

    // 알림 설정 업데이트
    public void updateNotificationSettings(Boolean notifyReviews, Boolean notifyFollows,
                                           Boolean notifyMessages, Boolean notifyMarketing) {
        if (notifyReviews != null) this.notifyReviews = notifyReviews;
        if (notifyFollows != null) this.notifyFollows = notifyFollows;
        if (notifyMessages != null) this.notifyMessages = notifyMessages;
        if (notifyMarketing != null) this.notifyMarketing = notifyMarketing;
    }
}
