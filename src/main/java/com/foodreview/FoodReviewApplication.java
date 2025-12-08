package com.foodreview;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class FoodReviewApplication {

    public static void main(String[] args) {
        SpringApplication.run(FoodReviewApplication.class, args);
    }
}
