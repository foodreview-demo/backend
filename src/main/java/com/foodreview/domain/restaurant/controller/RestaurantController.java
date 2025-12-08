package com.foodreview.domain.restaurant.controller;

import com.foodreview.domain.restaurant.dto.RestaurantDto;
import com.foodreview.domain.restaurant.service.RestaurantService;
import com.foodreview.global.common.ApiResponse;
import com.foodreview.global.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Restaurant", description = "음식점 API")
@RestController
@RequestMapping("/api/restaurants")
@RequiredArgsConstructor
public class RestaurantController {

    private final RestaurantService restaurantService;

    @Operation(summary = "음식점 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<RestaurantDto.SimpleResponse>>> getRestaurants(
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String category,
            @PageableDefault(size = 20) Pageable pageable) {
        PageResponse<RestaurantDto.SimpleResponse> response = restaurantService.getRestaurants(region, category, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "음식점 상세 조회")
    @GetMapping("/{restaurantId}")
    public ResponseEntity<ApiResponse<RestaurantDto.Response>> getRestaurant(@PathVariable Long restaurantId) {
        RestaurantDto.Response response = restaurantService.getRestaurant(restaurantId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "음식점 검색")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<RestaurantDto.SimpleResponse>>> searchRestaurants(
            @RequestParam String keyword,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String category,
            @PageableDefault(size = 20) Pageable pageable) {
        PageResponse<RestaurantDto.SimpleResponse> response = restaurantService.searchRestaurants(keyword, region, category, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "첫 리뷰 가능한 음식점 조회")
    @GetMapping("/first-review-available")
    public ResponseEntity<ApiResponse<PageResponse<RestaurantDto.SimpleResponse>>> getFirstReviewAvailable(
            @PageableDefault(size = 20) Pageable pageable) {
        PageResponse<RestaurantDto.SimpleResponse> response = restaurantService.getFirstReviewAvailableRestaurants(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "음식점 등록")
    @PostMapping
    public ResponseEntity<ApiResponse<RestaurantDto.Response>> createRestaurant(
            @Valid @RequestBody RestaurantDto.CreateRequest request) {
        RestaurantDto.Response response = restaurantService.createRestaurant(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response, "음식점이 등록되었습니다"));
    }
}
