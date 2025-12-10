package com.foodreview.domain.playlist.entity;

import com.foodreview.domain.common.BaseTimeEntity;
import com.foodreview.domain.restaurant.entity.Restaurant;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "playlist_items", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"playlist_id", "restaurant_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PlaylistItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "playlist_id", nullable = false)
    private Playlist playlist;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @Column(nullable = false)
    @Builder.Default
    private Integer position = 0;

    @Column(length = 500)
    private String memo;

    public void updatePosition(int position) {
        this.position = position;
    }

    public void updateMemo(String memo) {
        this.memo = memo;
    }
}
