package org.example.laptopstore.mapper;

import org.example.laptopstore.dto.response.productfavorite.ProductFavoriteResponse;
import org.example.laptopstore.entity.ProductFavorite;
import org.springframework.stereotype.Component;

@Component
public class ProductFavoriteMapper {

    public ProductFavoriteResponse toResponse(ProductFavorite entity) {
        ProductFavoriteResponse dto = new ProductFavoriteResponse();
        dto.setId(entity.getId());
        dto.setProductOptionId(entity.getProductOption().getId());
        dto.setProductName(entity.getProductOption().getProduct().getName());
        dto.setPrice(entity.getProductOption().getPrice());
        dto.setCreatedAt(entity.getCreatedAt());
        if (entity.getProductOption().getProductVariants() != null &&
                !entity.getProductOption().getProductVariants().isEmpty()) {
            dto.setImageUrl(entity.getProductOption().getProductVariants().get(0).getImageUrl());
        } else {
            dto.setImageUrl(null);
        }

        return dto;
    }
}
