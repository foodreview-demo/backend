package com.foodreview.domain.user.repository;

import com.foodreview.domain.user.entity.AuthProvider;
import com.foodreview.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    // OAuth provider로 사용자 조회
    Optional<User> findByProviderAndProviderId(AuthProvider provider, String providerId);

    boolean existsByEmail(String email);

    // 지역별 랭킹 조회
    Page<User> findByRegionOrderByTasteScoreDesc(String region, Pageable pageable);

    // 전체 랭킹 조회
    Page<User> findAllByOrderByTasteScoreDesc(Pageable pageable);

    // 사용자의 지역 내 순위 조회
    @Query("SELECT COUNT(u) + 1 FROM User u WHERE u.region = :region AND u.tasteScore > :score")
    Integer findRankByRegionAndScore(@Param("region") String region, @Param("score") Integer score);

    // 친구 추천 - 같은 지역, 비슷한 관심사
    @Query("SELECT u FROM User u WHERE u.region = :region AND u.id != :userId ORDER BY u.tasteScore DESC")
    List<User> findRecommendedFriends(@Param("region") String region, @Param("userId") Long userId, Pageable pageable);

    // 이름으로 검색 (대소문자 구분)
    Page<User> findByNameContaining(String name, Pageable pageable);

    // 이름으로 검색 (대소문자 무시)
    Page<User> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
