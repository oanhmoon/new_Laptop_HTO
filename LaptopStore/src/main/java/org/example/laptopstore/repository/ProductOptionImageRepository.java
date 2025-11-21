package org.example.laptopstore.repository;

import org.example.laptopstore.entity.ProductOptionImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductOptionImageRepository extends JpaRepository<ProductOptionImage, Long> {
    List<ProductOptionImage> findByProductOptionIdAndIsDeleteFalse(Long productOptionId);
}
