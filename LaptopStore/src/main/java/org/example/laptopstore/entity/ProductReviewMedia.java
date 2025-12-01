package org.example.laptopstore.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "product_review_media")
public class ProductReviewMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_review_id", nullable = false)
    private ProductReview productReview;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    private String type; // IMAGE or VIDEO
}
