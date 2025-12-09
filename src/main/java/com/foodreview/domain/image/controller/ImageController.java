package com.foodreview.domain.image.controller;

import com.foodreview.domain.image.dto.ImageDto;
import com.foodreview.domain.image.service.ImageService;
import com.foodreview.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Image", description = "이미지 업로드 API")
@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    @Operation(summary = "이미지 업로드", description = "최대 5개의 이미지를 업로드합니다 (각 파일 최대 10MB)")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ImageDto.UploadResponse>> uploadImages(
            @RequestPart("files") List<MultipartFile> files) {
        ImageDto.UploadResponse response = imageService.uploadImages(files);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "이미지가 업로드되었습니다"));
    }

    @Operation(summary = "이미지 삭제")
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteImage(
            @RequestBody ImageDto.DeleteRequest request) {
        imageService.deleteImage(request.getUrl());
        return ResponseEntity.ok(ApiResponse.success(null, "이미지가 삭제되었습니다"));
    }
}
