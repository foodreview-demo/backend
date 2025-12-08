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

    @Column(name = "taste_score", nullable = false)
    @Builder.Default
    private Integer tasteScore = 0;

    @Column(name = "review_count", nullable = false)
    @Builder.Default
    private Integer reviewCount = 0;

    @ElementCollection
    @CollectionTable(name = "user_favorite_categories", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "category")
    @Builder.Default
    private List<String> favoriteCategories = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Role role = Role.USER;

    // 점수 추가
    public void addScore(int points) {
        this.tasteScore += points;
    }

    // 리뷰 카운트 증가
    public void incrementReviewCount() {
        this.reviewCount++;
    }

    // 프로필 업데이트
    public void updateProfile(String name, String avatar, String region, List<String> favoriteCategories) {
        if (name != null) this.name = name;
        if (avatar != null) this.avatar = avatar;
        if (region != null) this.region = region;
        if (favoriteCategories != null) this.favoriteCategories = favoriteCategories;
    }

    // 맛잘알 등급 계산
    public String getTasteGrade() {
        if (tasteScore >= 2000) return "마스터";
        if (tasteScore >= 1500) return "전문가";
        if (tasteScore >= 1000) return "미식가";
        if (tasteScore >= 500) return "탐험가";
        return "입문자";
    }
}
