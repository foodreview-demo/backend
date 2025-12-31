package com.foodreview.domain.user.repository;

import com.foodreview.domain.user.entity.User;
import com.foodreview.domain.user.entity.UserBlock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {

    boolean existsByBlockerAndBlockedUser(User blocker, User blockedUser);

    boolean existsByBlockerIdAndBlockedUserId(Long blockerId, Long blockedUserId);

    Optional<UserBlock> findByBlockerAndBlockedUser(User blocker, User blockedUser);

    @Query("SELECT ub.blockedUser FROM UserBlock ub WHERE ub.blocker = :user")
    Page<User> findBlockedUsersByBlocker(@Param("user") User user, Pageable pageable);

    @Query("SELECT ub.blockedUser.id FROM UserBlock ub WHERE ub.blocker.id = :userId")
    List<Long> findBlockedUserIdsByBlockerId(@Param("userId") Long userId);

    @Query("SELECT ub.blocker.id FROM UserBlock ub WHERE ub.blockedUser.id = :userId")
    List<Long> findBlockerIdsByBlockedUserId(@Param("userId") Long userId);

    long countByBlocker(User blocker);

    void deleteByBlockerAndBlockedUser(User blocker, User blockedUser);
}
