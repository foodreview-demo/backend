package com.foodreview.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${firebase.config-path:firebase-service-account.json}")
    private String firebaseConfigPath;

    @PostConstruct
    public void initialize() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                Resource resource = getFirebaseResource();

                if (resource == null || !resource.exists()) {
                    log.warn("Firebase config file not found: {}. Push notifications will be disabled.", firebaseConfigPath);
                    return;
                }

                InputStream serviceAccount = resource.getInputStream();

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();

                FirebaseApp.initializeApp(options);
                log.info("Firebase initialized successfully from: {}", firebaseConfigPath);
            }
        } catch (IOException e) {
            log.error("Failed to initialize Firebase: {}", e.getMessage());
        }
    }

    private Resource getFirebaseResource() {
        // 절대 경로인 경우 (/ 또는 C: 로 시작)
        if (firebaseConfigPath.startsWith("/") || firebaseConfigPath.matches("^[A-Za-z]:.*")) {
            FileSystemResource fileResource = new FileSystemResource(firebaseConfigPath);
            if (fileResource.exists()) {
                log.info("Loading Firebase config from file system: {}", firebaseConfigPath);
                return fileResource;
            }
        }

        // classpath에서 찾기
        ClassPathResource classpathResource = new ClassPathResource(firebaseConfigPath);
        if (classpathResource.exists()) {
            log.info("Loading Firebase config from classpath: {}", firebaseConfigPath);
            return classpathResource;
        }

        // 현재 디렉토리에서 찾기 (배포 환경)
        FileSystemResource currentDirResource = new FileSystemResource(firebaseConfigPath);
        if (currentDirResource.exists()) {
            log.info("Loading Firebase config from current directory: {}", firebaseConfigPath);
            return currentDirResource;
        }

        return null;
    }

    @Bean
    public FirebaseMessaging firebaseMessaging() {
        if (FirebaseApp.getApps().isEmpty()) {
            log.warn("FirebaseApp not initialized. FirebaseMessaging will be null.");
            return null;
        }
        return FirebaseMessaging.getInstance();
    }
}
