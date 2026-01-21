package com.foodreview.domain.user.repository;

import com.foodreview.domain.user.entity.Follow;
import com.foodreview.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FollowRepository extends JpaRepository<Follow, Long> {

    // 팔로우 관계 확인
    boolean existsByFollowerAndFollowing(User follower, User following);

    // 팔로우 관계 조회
    Optional<Follow> findByFollowerAndFollowing(User follower, User following);

    // 팔로잉 목록 (내가 팔로우한 사람들)
    @Query("SELECT f.following FROM Follow f WHERE f.follower = :user")
    Page<User> findFollowingsByFollower(@Param("user") User user, Pageable pageable);

    // 팔로워 목록 (나를 팔로우한 사람들)
    @Query("SELECT f.follower FROM Follow f WHERE f.following = :user")
    Page<User> findFollowersByFollowing(@Param("user") User user, Pageable pageable);

    // 팔로잉 수
    long countByFollower(User follower);

    // 팔로워 수
    long countByFollowing(User following);

    // 팔로잉 ID 목록 조회 (빠른 확인용)
    @Query("SELECT f.following.id FROM Follow f WHERE f.follower.id = :userId")
    List<Long> findFollowingIdsByFollowerId(@Param("userId") Long userId);

    // 팔로우 관계 확인 (ID만 사용 - 최적화)
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Follow f WHERE f.follower.id = :followerId AND f.following.id = :followingId")
    boolean existsByFollowerIdAndFollowingId(@Param("followerId") Long followerId, @Param("followingId") Long followingId);

    // 회원 탈퇴 시 팔로우 관계 전체 삭제
    void deleteByFollowerOrFollowing(User follower, User following);

    // 2촌 관계 조회: 내가 팔로우하는 사람들이 팔로우하는 사용자 + 공통 팔로워 수
    // (나 자신과 이미 팔로우하는 사람 제외)
    @Query("SELECT f2.following.id, COUNT(f2.following.id) as mutualCount " +
           "FROM Follow f1 " +
           "JOIN Follow f2 ON f1.following = f2.follower " +
           "WHERE f1.follower.id = :userId " +
           "AND f2.following.id != :userId " +
           "AND f2.following.id NOT IN (SELECT f3.following.id FROM Follow f3 WHERE f3.follower.id = :userId) " +
           "GROUP BY f2.following.id " +
           "ORDER BY mutualCount DESC")
    List<Object[]> findSecondDegreeConnections(@Param("userId") Long userId);

    // 특정 사용자와의 공통 팔로워 이름 목록 (추천 이유 표시용, 최대 3명)
    @Query("SELECT f1.following.name FROM Follow f1 " +
           "JOIN Follow f2 ON f1.following = f2.follower " +
           "WHERE f1.follower.id = :userId AND f2.following.id = :targetUserId")
    List<String> findMutualFollowerNames(@Param("userId") Long userId, @Param("targetUserId") Long targetUserId);
}
