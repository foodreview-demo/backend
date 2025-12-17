package com.foodreview.domain.restaurant.service;

import com.foodreview.domain.restaurant.dto.RestaurantDto;
import com.foodreview.domain.restaurant.entity.Restaurant;
import com.foodreview.domain.restaurant.repository.RestaurantRepository;
import com.foodreview.global.common.PageResponse;
import com.foodreview.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;

    public RestaurantDto.Response getRestaurant(Long restaurantId) {
        Restaurant restaurant = findRestaurantById(restaurantId);
        return RestaurantDto.Response.from(restaurant);
    }

    public PageResponse<RestaurantDto.SimpleResponse> getRestaurants(String region, String category, Pageable pageable) {
        Page<Restaurant> restaurants;

        if (region != null && category != null) {
            Restaurant.Category cat = Restaurant.Category.valueOf(category);
            restaurants = restaurantRepository.findByRegionAndCategory(region, cat, pageable);
        } else if (region != null) {
            restaurants = restaurantRepository.findByRegion(region, pageable);
        } else if (category != null) {
            Restaurant.Category cat = Restaurant.Category.valueOf(category);
            restaurants = restaurantRepository.findByCategory(cat, pageable);
        } else {
            restaurants = restaurantRepository.findAll(pageable);
        }

        List<RestaurantDto.SimpleResponse> content = restaurants.getContent().stream()
                .map(RestaurantDto.SimpleResponse::from)
                .toList();

        return PageResponse.from(restaurants, content);
    }

    public PageResponse<RestaurantDto.SimpleResponse> searchRestaurants(
            String keyword, String region, String category, Pageable pageable) {

        Page<Restaurant> restaurants;

        if (region != null && category != null) {
            Restaurant.Category cat = Restaurant.Category.valueOf(category);
            restaurants = restaurantRepository.searchByRegionAndCategory(keyword, region, cat, pageable);
        } else if (region != null) {
            restaurants = restaurantRepository.searchByRegion(keyword, region, pageable);
        } else if (category != null) {
            Restaurant.Category cat = Restaurant.Category.valueOf(category);
            restaurants = restaurantRepository.searchByCategory(keyword, cat, pageable);
        } else {
            restaurants = restaurantRepository.search(keyword, pageable);
        }

        List<RestaurantDto.SimpleResponse> content = restaurants.getContent().stream()
                .map(RestaurantDto.SimpleResponse::from)
                .toList();

        return PageResponse.from(restaurants, content);
    }

    // 첫 리뷰 가능한 음식점 조회
    public PageResponse<RestaurantDto.SimpleResponse> getFirstReviewAvailableRestaurants(Pageable pageable) {
        Page<Restaurant> restaurants = restaurantRepository.findByReviewCount(0, pageable);
        List<RestaurantDto.SimpleResponse> content = restaurants.getContent().stream()
                .map(RestaurantDto.SimpleResponse::from)
                .toList();
        return PageResponse.from(restaurants, content);
    }

    @Transactional
    public RestaurantDto.Response createRestaurant(RestaurantDto.CreateRequest request) {
        // 카카오 Place ID가 있으면 중복 체크
        if (request.getKakaoPlaceId() != null) {
            var existing = restaurantRepository.findByKakaoPlaceId(request.getKakaoPlaceId());
            if (existing.isPresent()) {
                // 이미 등록된 음식점이면 해당 음식점 반환
                return RestaurantDto.Response.from(existing.get());
            }
        }

        Restaurant.Category category = Restaurant.Category.valueOf(request.getCategory());

        Restaurant restaurant = Restaurant.builder()
                .name(request.getName())
                .category(category)
                .address(request.getAddress())
                .region(request.getRegion())
                .thumbnail(request.getThumbnail())
                .priceRange(request.getPriceRange())
                .phone(request.getPhone())
                .businessHours(request.getBusinessHours())
                .kakaoPlaceId(request.getKakaoPlaceId())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .build();

        Restaurant saved = restaurantRepository.save(restaurant);
        return RestaurantDto.Response.from(saved);
    }

    // 카카오 Place ID로 음식점 조회
    public RestaurantDto.Response getRestaurantByKakaoPlaceId(String kakaoPlaceId) {
        return restaurantRepository.findByKakaoPlaceId(kakaoPlaceId)
                .map(RestaurantDto.Response::from)
                .orElse(null);
    }

    public Restaurant findRestaurantById(Long restaurantId) {
        return restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new CustomException("음식점을 찾을 수 없습니다", HttpStatus.NOT_FOUND, "RESTAURANT_NOT_FOUND"));
    }
}
