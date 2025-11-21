package org.example.laptopstore.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "product_option_images")
public class ProductOptionImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Size(max = 255)
    @Column(name = "url")
    private String url;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_option_id", nullable = false)
    private ProductOption productOption;

    @Column(name = "is_delete")
    private Boolean isDelete;

    @PrePersist
    private void prePersist() {
        if (this.isDelete == null) {
            this.isDelete = false;
        }
    }
}
