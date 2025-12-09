package com.foodreview.domain.image.service;

import com.foodreview.domain.image.dto.ImageDto;
import com.foodreview.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageService {

    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.region.static}")
    private String region;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final int MAX_IMAGE_COUNT = 5;
    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    public ImageDto.UploadResponse uploadImages(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new CustomException("업로드할 이미지가 없습니다", HttpStatus.BAD_REQUEST);
        }

        if (files.size() > MAX_IMAGE_COUNT) {
            throw new CustomException(
                    String.format("이미지는 최대 %d개까지 업로드할 수 있습니다", MAX_IMAGE_COUNT),
                    HttpStatus.BAD_REQUEST);
        }

        List<String> uploadedUrls = new ArrayList<>();

        for (MultipartFile file : files) {
            validateFile(file);
            String url = uploadSingleImage(file);
            uploadedUrls.add(url);
        }

        return ImageDto.UploadResponse.builder()
                .urls(uploadedUrls)
                .count(uploadedUrls.size())
                .build();
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new CustomException("빈 파일은 업로드할 수 없습니다", HttpStatus.BAD_REQUEST);
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new CustomException(
                    String.format("파일 크기는 %dMB를 초과할 수 없습니다", MAX_FILE_SIZE / (1024 * 1024)),
                    HttpStatus.BAD_REQUEST);
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new CustomException(
                    "지원되지 않는 파일 형식입니다. (JPEG, PNG, GIF, WebP만 허용)",
                    HttpStatus.BAD_REQUEST);
        }
    }

    private String uploadSingleImage(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String key = generateUniqueKey(extension);

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            return getPublicUrl(key);
        } catch (IOException e) {
            log.error("S3 이미지 업로드 실패: {}", e.getMessage());
            throw new CustomException("이미지 업로드에 실패했습니다", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public void deleteImage(String imageUrl) {
        String key = extractKeyFromUrl(imageUrl);

        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("S3 이미지 삭제 완료: {}", key);
        } catch (Exception e) {
            log.error("S3 이미지 삭제 실패: {}", e.getMessage());
            throw new CustomException("이미지 삭제에 실패했습니다", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String generateUniqueKey(String extension) {
        return String.format("reviews/%s%s", UUID.randomUUID().toString(), extension);
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".jpg";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    private String getPublicUrl(String key) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, key);
    }

    private String extractKeyFromUrl(String url) {
        // URL: https://bucket.s3.region.amazonaws.com/key
        String prefix = String.format("https://%s.s3.%s.amazonaws.com/", bucket, region);
        if (url.startsWith(prefix)) {
            return url.substring(prefix.length());
        }
        throw new CustomException("유효하지 않은 이미지 URL입니다", HttpStatus.BAD_REQUEST);
    }
}
