package org.example.laptopstore.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "user_view_history")
public class UserViewHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "view_count")
    private Integer viewCount = 1;

    @Column(name = "last_viewed")
    private LocalDateTime lastViewed = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.lastViewed = LocalDateTime.now();
    }
}
