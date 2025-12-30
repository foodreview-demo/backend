package com.foodreview.domain.image.service;

import com.foodreview.domain.image.dto.ImageDto;
import com.foodreview.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
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
    private static final List<String> ALLOWED_EXTENSIONS = List.of(
            ".jpg", ".jpeg", ".png", ".gif", ".webp"
    );

    // Magic Bytes (파일 시그니처)
    private static final byte[] JPEG_MAGIC = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_MAGIC = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] GIF_MAGIC_87 = new byte[]{0x47, 0x49, 0x46, 0x38, 0x37, 0x61}; // GIF87a
    private static final byte[] GIF_MAGIC_89 = new byte[]{0x47, 0x49, 0x46, 0x38, 0x39, 0x61}; // GIF89a
    private static final byte[] WEBP_MAGIC = new byte[]{0x52, 0x49, 0x46, 0x46}; // RIFF (WebP는 RIFF 컨테이너)

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

        // 1. Content-Type 검증
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new CustomException(
                    "지원되지 않는 파일 형식입니다. (JPEG, PNG, GIF, WebP만 허용)",
                    HttpStatus.BAD_REQUEST);
        }

        // 2. Magic Bytes (파일 시그니처) 검증 - 보안 강화
        if (!validateMagicBytes(file)) {
            log.warn("파일 시그니처 검증 실패: contentType={}, filename={}",
                    contentType, file.getOriginalFilename());
            throw new CustomException(
                    "파일 형식이 올바르지 않습니다. 실제 이미지 파일만 업로드해주세요.",
                    HttpStatus.BAD_REQUEST);
        }

        // 3. 실제 이미지로 읽을 수 있는지 검증
        if (!isValidImage(file)) {
            log.warn("이미지 디코딩 실패: contentType={}, filename={}",
                    contentType, file.getOriginalFilename());
            throw new CustomException(
                    "손상된 이미지 파일입니다. 다른 파일을 업로드해주세요.",
                    HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Magic Bytes (파일 시그니처) 검증
     * Content-Type 위조 방지를 위해 파일의 실제 바이너리 헤더를 검사
     */
    private boolean validateMagicBytes(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[12]; // 충분한 크기의 헤더 읽기
            int bytesRead = is.read(header);

            if (bytesRead < 3) {
                return false;
            }

            // JPEG: FF D8 FF
            if (startsWith(header, JPEG_MAGIC)) {
                return true;
            }

            // PNG: 89 50 4E 47 0D 0A 1A 0A
            if (bytesRead >= 8 && startsWith(header, PNG_MAGIC)) {
                return true;
            }

            // GIF87a 또는 GIF89a
            if (bytesRead >= 6 && (startsWith(header, GIF_MAGIC_87) || startsWith(header, GIF_MAGIC_89))) {
                return true;
            }

            // WebP: RIFF....WEBP
            if (bytesRead >= 12 && startsWith(header, WEBP_MAGIC)) {
                // WebP는 RIFF 컨테이너이므로 8-11 바이트가 "WEBP"인지 확인
                byte[] webpSignature = new byte[]{0x57, 0x45, 0x42, 0x50}; // "WEBP"
                byte[] headerPart = Arrays.copyOfRange(header, 8, 12);
                return Arrays.equals(headerPart, webpSignature);
            }

            return false;
        } catch (IOException e) {
            log.error("파일 읽기 실패", e);
            return false;
        }
    }

    private boolean startsWith(byte[] array, byte[] prefix) {
        if (array.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (array[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * 실제 이미지로 디코딩 가능한지 검증
     */
    private boolean isValidImage(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            BufferedImage image = ImageIO.read(is);
            return image != null && image.getWidth() > 0 && image.getHeight() > 0;
        } catch (IOException e) {
            log.error("이미지 디코딩 실패", e);
            return false;
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
        return String.format("reviews/%s%s", UUID.randomUUID(), extension);
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".jpg";
        }
        String ext = filename.substring(filename.lastIndexOf(".")).toLowerCase();
        // 화이트리스트에 있는 확장자만 허용
        return ALLOWED_EXTENSIONS.contains(ext) ? ext : ".jpg";
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
