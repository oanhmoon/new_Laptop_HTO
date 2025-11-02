package org.example.laptopstore.service;

import org.example.laptopstore.dto.response.productfavorite.ProductFavoriteResponse;
import org.example.laptopstore.dto.response.productfavorite.ProductFavoriteResponse;

import java.util.List;

public interface ProductFavoriteService {
    void addFavorite(Long userId, Long productOptionId);
    void removeFavorite(Long userId, Long productOptionId);
    List<ProductFavoriteResponse> getFavorites(Long userId);
    boolean isFavorite(Long userId, Long productOptionId);
}
