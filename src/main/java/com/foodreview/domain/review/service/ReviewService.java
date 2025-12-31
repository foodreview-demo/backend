package com.foodreview.domain.review.service;

import com.foodreview.domain.notification.entity.Notification;
import com.foodreview.domain.notification.service.NotificationService;
import com.foodreview.domain.restaurant.entity.Restaurant;
import com.foodreview.domain.restaurant.repository.RestaurantRepository;
import com.foodreview.domain.review.dto.ReviewDto;
import com.foodreview.domain.review.entity.Review;
import com.foodreview.domain.review.entity.ReviewReference;
import com.foodreview.domain.review.entity.Sympathy;
import com.foodreview.domain.review.repository.ReviewReferenceRepository;
import com.foodreview.domain.review.repository.ReviewRepository;
import com.foodreview.domain.review.repository.SympathyRepository;
import com.foodreview.domain.user.entity.ScoreEvent;
import com.foodreview.domain.user.entity.User;
import com.foodreview.domain.user.repository.ScoreEventRepository;
import com.foodreview.domain.user.repository.UserBlockRepository;
import com.foodreview.domain.user.repository.UserRepository;
import com.foodreview.global.common.PageResponse;
import com.foodreview.global.exception.CustomException;
import com.foodreview.global.util.HtmlSanitizer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final RestaurantRepository restaurantRepository;
    private final UserRepository userRepository;
    private final SympathyRepository sympathyRepository;
    private final ScoreEventRepository scoreEventRepository;
    private final ReviewReferenceRepository reviewReferenceRepository;
    private final UserBlockRepository userBlockRepository;
    private final NotificationService notificationService;

    private static final int FIRST_REVIEW_POINTS = 100;
    private static final int NORMAL_REVIEW_POINTS = 50;
    private static final int MASTER_SYMPATHY_BONUS = 25;
    private static final int MASTER_SCORE_THRESHOLD = 2000;
    private static final int INFLUENCE_POINTS = 5;
    private static final int INFLUENCE_FIRST_REVIEW_POINTS = 10;

    // 리뷰 조회
    public ReviewDto.Response getReview(Long reviewId, Long currentUserId) {
        Review review = findReviewById(reviewId);
        boolean hasSympathized = currentUserId != null &&
                sympathyRepository.existsByUserAndReview(findUserById(currentUserId), review);

        // 참고 정보 조회
        ReviewDto.ReferenceInfo referenceInfo = null;
        Optional<ReviewReference> reference = reviewReferenceRepository.findByReview(review);
        if (reference.isPresent()) {
            referenceInfo = ReviewDto.ReferenceInfo.from(
                    reference.get().getReferenceReview().getId(),
                    reference.get().getReferenceUser()
            );
        }

        // 이 리뷰를 참고한 횟수
        int referenceCount = reviewReferenceRepository.countByReferenceReview(review);

        return ReviewDto.Response.from(review, hasSympathized, referenceInfo, referenceCount);
    }

    // 리뷰 목록 조회 (필터링) - 기존 호환
    public PageResponse<ReviewDto.Response> getReviews(String region, String category, Long currentUserId, Pageable pageable) {
        return getReviews(region, null, null, category, currentUserId, pageable);
    }

    // 리뷰 목록 조회 (동 단위 필터링 지원)
    public PageResponse<ReviewDto.Response> getReviews(String region, String district, String neighborhood,
                                                        String category, Long currentUserId, Pageable pageable) {
        Page<Review> reviews;
        Restaurant.Category cat = category != null ? Restaurant.Category.valueOf(category) : null;

        // 동 단위 필터링 (가장 세밀한 필터)
        // neighborhood가 콤마로 구분된 경우 IN 절 사용
        if (neighborhood != null) {
            List<String> neighborhoods = Arrays.asList(neighborhood.split(","));
            if (neighborhoods.size() > 1) {
                // 복수 동 필터링
                if (cat != null) {
                    reviews = reviewRepository.findByNeighborhoodInAndCategory(neighborhoods, cat, pageable);
                } else {
                    reviews = reviewRepository.findByNeighborhoodIn(neighborhoods, pageable);
                }
            } else {
                // 단일 동 필터링
                if (cat != null) {
                    reviews = reviewRepository.findByNeighborhoodAndCategory(neighborhood, cat, pageable);
                } else {
                    reviews = reviewRepository.findByNeighborhood(neighborhood, pageable);
                }
            }
        }
        // 구 단위 필터링
        else if (district != null) {
            if (cat != null) {
                reviews = reviewRepository.findByDistrictAndCategory(district, cat, pageable);
            } else {
                reviews = reviewRepository.findByDistrict(district, pageable);
            }
        }
        // 시/도 단위 필터링 (기존)
        else if (region != null) {
            if (cat != null) {
                reviews = reviewRepository.findByRegionAndCategory(region, cat, pageable);
            } else {
                reviews = reviewRepository.findByRegion(region, pageable);
            }
        }
        // 카테고리만 필터링
        else if (cat != null) {
            reviews = reviewRepository.findByCategory(cat, pageable);
        }
        // 전체 조회
        else {
            reviews = reviewRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        Set<Long> sympathizedReviewIds = getSympathizedReviewIds(currentUserId);
        Set<Long> blockedUserIds = getBlockedUserIds(currentUserId);
        List<ReviewDto.Response> content = convertToResponseDtos(reviews.getContent(), sympathizedReviewIds, blockedUserIds);

        return PageResponse.from(reviews, content);
    }

    // 동별 리뷰 수 집계 (지도 마커용)
    public List<ReviewDto.NeighborhoodCount> getReviewCountByNeighborhood(String region, String district) {
        List<Object[]> results = reviewRepository.countByNeighborhood(region, district);
        return results.stream()
                .map(row -> new ReviewDto.NeighborhoodCount((String) row[0], (Long) row[1]))
                .collect(Collectors.toList());
    }

    // 구별 리뷰 수 집계
    public List<ReviewDto.DistrictCount> getReviewCountByDistrict(String region) {
        List<Object[]> results = reviewRepository.countByDistrict(region);
        return results.stream()
                .map(row -> new ReviewDto.DistrictCount((String) row[0], (Long) row[1]))
                .collect(Collectors.toList());
    }

    // 음식점별 리뷰 조회
    public PageResponse<ReviewDto.Response> getRestaurantReviews(Long restaurantId, Long currentUserId, Pageable pageable) {
        Restaurant restaurant = findRestaurantById(restaurantId);
        Page<Review> reviews = reviewRepository.findByRestaurantOrderByCreatedAtDesc(restaurant, pageable);

        Set<Long> sympathizedReviewIds = getSympathizedReviewIds(currentUserId);
        Set<Long> blockedUserIds = getBlockedUserIds(currentUserId);
        List<ReviewDto.Response> content = convertToResponseDtos(reviews.getContent(), sympathizedReviewIds, blockedUserIds);

        return PageResponse.from(reviews, content);
    }

    // 사용자별 리뷰 조회
    public PageResponse<ReviewDto.Response> getUserReviews(Long userId, Long currentUserId, Pageable pageable) {
        User user = findUserById(userId);
        Page<Review> reviews = reviewRepository.findByUserOrderByCreatedAtDesc(user, pageable);

        Set<Long> sympathizedReviewIds = getSympathizedReviewIds(currentUserId);
        Set<Long> blockedUserIds = getBlockedUserIds(currentUserId);
        List<ReviewDto.Response> content = convertToResponseDtos(reviews.getContent(), sympathizedReviewIds, blockedUserIds);

        return PageResponse.from(reviews, content);
    }

    // 리뷰 작성
    @Transactional
    public ReviewDto.Response createReview(Long userId, ReviewDto.CreateRequest request) {
        User user = findUserById(userId);
        Restaurant restaurant = findRestaurantById(request.getRestaurantId());

        // 이미 리뷰 작성 여부 확인
        if (reviewRepository.existsByUserAndRestaurant(user, restaurant)) {
            throw new CustomException("이미 이 음식점에 리뷰를 작성하셨습니다", HttpStatus.CONFLICT, "REVIEW_EXISTS");
        }

        boolean isFirstReview = restaurant.isFirstReviewAvailable();

        // XSS 방지를 위한 리뷰 콘텐츠 새니타이징
        String sanitizedContent = HtmlSanitizer.sanitizeReviewContent(request.getContent());

        Review review = Review.builder()
                .user(user)
                .restaurant(restaurant)
                .content(sanitizedContent)
                .rating(request.getRating())
                .tasteRating(request.getTasteRating())
                .priceRating(request.getPriceRating())
                .atmosphereRating(request.getAtmosphereRating())
                .serviceRating(request.getServiceRating())
                .images(request.getImages() != null ? request.getImages() : List.of())
                .menu(request.getMenu())
                .price(request.getPrice())
                .visitDate(request.getVisitDate())
                .isFirstReview(isFirstReview)
                .build();

        Review savedReview = reviewRepository.save(review);

        // 음식점 평점 업데이트
        restaurant.addReview(request.getRating());
        restaurant.addDetailRatings(
                request.getTasteRating(),
                request.getPriceRating(),
                request.getAtmosphereRating(),
                request.getServiceRating()
        );

        // 점수 부여
        int points = isFirstReview ? FIRST_REVIEW_POINTS : NORMAL_REVIEW_POINTS;
        user.addScore(points);
        user.incrementReviewCount();

        // 점수 획득 이벤트 기록
        ScoreEvent.ScoreEventType eventType = isFirstReview ?
                ScoreEvent.ScoreEventType.FIRST_REVIEW : ScoreEvent.ScoreEventType.REVIEW;
        String description = isFirstReview ?
                String.format("%s 첫 리뷰 작성", restaurant.getName()) :
                String.format("%s 리뷰 작성", restaurant.getName());

        ScoreEvent event = ScoreEvent.builder()
                .user(user)
                .type(eventType)
                .description(description)
                .points(points)
                .build();
        scoreEventRepository.save(event);

        // 참고 리뷰 처리
        ReviewDto.ReferenceInfo referenceInfo = null;
        if (request.getReferenceReviewId() != null) {
            referenceInfo = processReferenceReview(savedReview, user, request.getReferenceReviewId());
        }

        return ReviewDto.Response.from(savedReview, false, referenceInfo, 0);
    }

    // 참고 리뷰 처리 (포인트 지급 및 기록)
    private ReviewDto.ReferenceInfo processReferenceReview(Review review, User reviewer, Long referenceReviewId) {
        Review referenceReview = findReviewById(referenceReviewId);
        User referenceUser = referenceReview.getUser();

        // 본인 리뷰 참고 불가
        if (referenceUser.getId().equals(reviewer.getId())) {
            return null;
        }

        // 상호 참고 체크 (어뷰징 방지): A→B 참고 후 B→A 참고 시 포인트 미지급
        boolean isMutualReference = reviewReferenceRepository.existsMutualReference(reviewer, referenceUser);

        // 포인트 계산
        int influencePoints = 0;
        if (!isMutualReference) {
            influencePoints = referenceReview.getIsFirstReview() ? INFLUENCE_FIRST_REVIEW_POINTS : INFLUENCE_POINTS;
            referenceUser.addScore(influencePoints);

            // 점수 획득 이벤트 기록
            ScoreEvent.ScoreEventType eventType = referenceReview.getIsFirstReview() ?
                    ScoreEvent.ScoreEventType.INFLUENCE_FIRST_REVIEW : ScoreEvent.ScoreEventType.INFLUENCE;
            String description = String.format("%s님이 내 리뷰를 참고하여 %s 리뷰 작성",
                    reviewer.getName(), review.getRestaurant().getName());

            ScoreEvent event = ScoreEvent.builder()
                    .user(referenceUser)
                    .type(eventType)
                    .description(description)
                    .points(influencePoints)
                    .fromUser(reviewer)
                    .build();

            scoreEventRepository.save(event);

            // 알림 생성
            notificationService.createNotification(
                    referenceUser,
                    reviewer,
                    Notification.NotificationType.INFLUENCE,
                    String.format("%s님이 회원님의 리뷰를 참고하여 리뷰를 작성했습니다. (+%d점)", reviewer.getName(), influencePoints),
                    referenceReview.getRestaurant().getId()
            );
        }

        // 참고 기록 저장
        ReviewReference reference = ReviewReference.builder()
                .review(review)
                .referenceReview(referenceReview)
                .referenceUser(referenceUser)
                .pointsAwarded(influencePoints)
                .build();
        reviewReferenceRepository.save(reference);

        return ReviewDto.ReferenceInfo.from(referenceReviewId, referenceUser);
    }

    // 리뷰 수정
    @Transactional
    public ReviewDto.Response updateReview(Long userId, Long reviewId, ReviewDto.UpdateRequest request) {
        Review review = findReviewById(reviewId);

        if (!review.getUser().getId().equals(userId)) {
            throw new CustomException("본인의 리뷰만 수정할 수 있습니다", HttpStatus.FORBIDDEN, "FORBIDDEN");
        }

        // 평점 변경 시 음식점 평점 업데이트
        if (request.getRating() != null && !request.getRating().equals(review.getRating())) {
            review.getRestaurant().removeReview(review.getRating());
            review.getRestaurant().addReview(request.getRating());
        }

        review.update(
                request.getContent(),
                request.getRating(),
                request.getTasteRating(),
                request.getPriceRating(),
                request.getAtmosphereRating(),
                request.getServiceRating(),
                request.getImages(),
                request.getMenu(),
                request.getPrice(),
                request.getVisitDate()
        );

        return ReviewDto.Response.from(review, false);
    }

    // 리뷰 삭제
    @Transactional
    public void deleteReview(Long userId, Long reviewId) {
        Review review = findReviewById(reviewId);

        if (!review.getUser().getId().equals(userId)) {
            throw new CustomException("본인의 리뷰만 삭제할 수 있습니다", HttpStatus.FORBIDDEN, "FORBIDDEN");
        }

        // 음식점 평점 업데이트
        review.getRestaurant().removeReview(review.getRating());
        review.getRestaurant().removeDetailRatings(
                review.getTasteRating(),
                review.getPriceRating(),
                review.getAtmosphereRating(),
                review.getServiceRating()
        );

        reviewRepository.delete(review);
    }

    // 공감 추가
    @Transactional
    public ReviewDto.SympathyResponse addSympathy(Long userId, Long reviewId) {
        User user = findUserById(userId);
        Review review = findReviewById(reviewId);

        if (review.getUser().getId().equals(userId)) {
            throw new CustomException("본인의 리뷰에는 공감할 수 없습니다", HttpStatus.BAD_REQUEST, "SELF_SYMPATHY");
        }

        if (sympathyRepository.existsByUserAndReview(user, review)) {
            throw new CustomException("이미 공감한 리뷰입니다", HttpStatus.CONFLICT, "SYMPATHY_EXISTS");
        }

        Sympathy sympathy = Sympathy.builder()
                .user(user)
                .review(review)
                .build();
        sympathyRepository.save(sympathy);

        review.addSympathy();

        // 리뷰 작성자에게 점수 부여 (공감한 유저 점수의 0.5%)
        User reviewAuthor = review.getUser();

        // 리뷰 작성자의 받은 공감 수 증가
        reviewAuthor.incrementReceivedSympathyCount();
        int points = (int) (user.getTasteScore() * 0.005);
        if (points < 1) points = 1;

        reviewAuthor.addScore(points);

        // 점수 획득 이벤트 기록
        ScoreEvent event = ScoreEvent.builder()
                .user(reviewAuthor)
                .type(ScoreEvent.ScoreEventType.SYMPATHY_RECEIVED)
                .description(String.format("%s님이 공감", user.getName()))
                .points(points)
                .fromUser(user)
                .build();
        scoreEventRepository.save(event);

        // 마스터 보너스
        if (user.getTasteScore() >= MASTER_SCORE_THRESHOLD) {
            reviewAuthor.addScore(MASTER_SYMPATHY_BONUS);

            ScoreEvent bonusEvent = ScoreEvent.builder()
                    .user(reviewAuthor)
                    .type(ScoreEvent.ScoreEventType.SYMPATHY_BONUS)
                    .description(String.format("마스터 %s님의 공감 보너스", user.getName()))
                    .points(MASTER_SYMPATHY_BONUS)
                    .fromUser(user)
                    .build();
            scoreEventRepository.save(bonusEvent);
        }

        // 공감 알림 생성
        notificationService.createNotification(
                reviewAuthor,
                user,
                Notification.NotificationType.SYMPATHY,
                String.format("%s님이 회원님의 리뷰에 공감했습니다.", user.getName()),
                review.getRestaurant().getId()
        );

        return ReviewDto.SympathyResponse.builder()
                .reviewId(reviewId)
                .sympathyCount(review.getSympathyCount())
                .hasSympathized(true)
                .build();
    }

    // 공감 취소
    @Transactional
    public ReviewDto.SympathyResponse removeSympathy(Long userId, Long reviewId) {
        User user = findUserById(userId);
        Review review = findReviewById(reviewId);

        Sympathy sympathy = sympathyRepository.findByUserAndReview(user, review)
                .orElseThrow(() -> new CustomException("공감 기록이 없습니다", HttpStatus.NOT_FOUND, "SYMPATHY_NOT_FOUND"));

        sympathyRepository.delete(sympathy);
        review.removeSympathy();

        // 리뷰 작성자의 받은 공감 수 감소
        review.getUser().decrementReceivedSympathyCount();

        return ReviewDto.SympathyResponse.builder()
                .reviewId(reviewId)
                .sympathyCount(review.getSympathyCount())
                .hasSympathized(false)
                .build();
    }

    // 사용자 영향력 통계 조회 (단일 쿼리로 최적화)
    public ReviewDto.InfluenceStats getInfluenceStats(Long userId) {
        List<Object[]> result = reviewReferenceRepository.getInfluenceStatsByUserId(userId);

        int totalReferenceCount = 0;
        int totalInfluencePoints = 0;

        if (result != null && !result.isEmpty()) {
            Object[] stats = result.get(0);
            if (stats[0] != null) {
                totalReferenceCount = ((Number) stats[0]).intValue();
            }
            if (stats[1] != null) {
                totalInfluencePoints = ((Number) stats[1]).intValue();
            }
        }

        return ReviewDto.InfluenceStats.builder()
                .totalReferenceCount(totalReferenceCount)
                .totalInfluencePoints(totalInfluencePoints)
                .build();
    }

    private Set<Long> getSympathizedReviewIds(Long userId) {
        if (userId == null) return new HashSet<>();
        return new HashSet<>(sympathyRepository.findReviewIdsByUserId(userId));
    }

    private Set<Long> getBlockedUserIds(Long userId) {
        if (userId == null) return new HashSet<>();
        return new HashSet<>(userBlockRepository.findBlockedUserIdsByBlockerId(userId));
    }

    // 리뷰 목록을 DTO로 변환 (배치 쿼리로 N+1 방지)
    private List<ReviewDto.Response> convertToResponseDtos(List<Review> reviews, Set<Long> sympathizedReviewIds, Set<Long> blockedUserIds) {
        if (reviews.isEmpty()) {
            return List.of();
        }

        List<Long> reviewIds = reviews.stream().map(Review::getId).toList();

        // 배치로 참고 정보 조회 (1개 쿼리)
        Map<Long, ReviewReference> referenceMap = reviewReferenceRepository.findByReviewIds(reviewIds)
                .stream()
                .collect(Collectors.toMap(rr -> rr.getReview().getId(), Function.identity()));

        // 배치로 참고된 횟수 조회 (1개 쿼리)
        Map<Long, Integer> referenceCountMap = reviewReferenceRepository.countByReferenceReviewIds(reviewIds)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> ((Number) row[1]).intValue()
                ));

        return reviews.stream()
                .filter(review -> !blockedUserIds.contains(review.getUser().getId()))
                .map(review -> {
                    ReviewDto.ReferenceInfo referenceInfo = null;
                    ReviewReference reference = referenceMap.get(review.getId());
                    if (reference != null) {
                        referenceInfo = ReviewDto.ReferenceInfo.from(
                                reference.getReferenceReview().getId(),
                                reference.getReferenceUser()
                        );
                    }
                    int referenceCount = referenceCountMap.getOrDefault(review.getId(), 0);
                    return ReviewDto.Response.from(review, sympathizedReviewIds.contains(review.getId()), referenceInfo, referenceCount);
                })
                .toList();
    }

    private Review findReviewById(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new CustomException("리뷰를 찾을 수 없습니다", HttpStatus.NOT_FOUND, "REVIEW_NOT_FOUND"));
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException("사용자를 찾을 수 없습니다", HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));
    }

    private Restaurant findRestaurantById(Long restaurantId) {
        return restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new CustomException("음식점을 찾을 수 없습니다", HttpStatus.NOT_FOUND, "RESTAURANT_NOT_FOUND"));
    }
}
