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
}
