package com.foodreview.domain.user.controller;

import com.foodreview.domain.user.dto.UserDto;
import com.foodreview.domain.user.service.UserService;
import com.foodreview.global.common.ApiResponse;
import com.foodreview.global.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Ranking", description = "랭킹 API")
@RestController
@RequestMapping("/api/ranking")
@RequiredArgsConstructor
public class RankingController {

    private final UserService userService;

    @Operation(summary = "랭킹 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<UserDto.RankingResponse>>> getRanking(
            @RequestParam(required = false) String region,
            @PageableDefault(size = 20) Pageable pageable) {
        PageResponse<UserDto.RankingResponse> response = userService.getRanking(region, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
