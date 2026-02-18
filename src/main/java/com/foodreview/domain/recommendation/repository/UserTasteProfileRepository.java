package com.foodreview.domain.recommendation.repository;

import com.foodreview.domain.recommendation.entity.UserTasteProfile;
import com.foodreview.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserTasteProfileRepository extends JpaRepository<UserTasteProfile, Long> {

    Optional<UserTasteProfile> findByUser(User user);

    Optional<UserTasteProfile> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}
