package org.example.laptopstore.controller;

import lombok.RequiredArgsConstructor;
import org.example.laptopstore.dto.response.productfavorite.ProductFavoriteResponse;
import org.example.laptopstore.service.ProductFavoriteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class ProductFavoriteController {

    private final ProductFavoriteService favoriteService;

    @PostMapping("/{userId}/add/{productOptionId}")
    public ResponseEntity<String> addFavorite(@PathVariable Long userId, @PathVariable Long productOptionId) {
        favoriteService.addFavorite(userId, productOptionId);
        return ResponseEntity.ok("Added to favorites");
    }

    @DeleteMapping("/{userId}/remove/{productOptionId}")
    public ResponseEntity<String> removeFavorite(@PathVariable Long userId, @PathVariable Long productOptionId) {
        favoriteService.removeFavorite(userId, productOptionId);
        return ResponseEntity.ok("Removed from favorites");
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<ProductFavoriteResponse>> getFavorites(@PathVariable Long userId) {
        return ResponseEntity.ok(favoriteService.getFavorites(userId));
    }

    @GetMapping("/{userId}/check/{productOptionId}")
    public ResponseEntity<Boolean> isFavorite(@PathVariable Long userId, @PathVariable Long productOptionId) {
        return ResponseEntity.ok(favoriteService.isFavorite(userId, productOptionId));
    }
}
