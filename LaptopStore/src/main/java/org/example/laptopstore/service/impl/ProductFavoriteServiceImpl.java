package org.example.laptopstore.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.laptopstore.dto.response.productfavorite.ProductFavoriteResponse;
import org.example.laptopstore.entity.*;
import org.example.laptopstore.exception.ResourceNotFoundException;
import org.example.laptopstore.mapper.ProductFavoriteMapper;
import org.example.laptopstore.repository.*;
import org.example.laptopstore.service.ProductFavoriteService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductFavoriteServiceImpl implements ProductFavoriteService {

    private final ProductFavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final ProductOptionRepository productOptionRepository;
    private final ProductFavoriteMapper favoriteMapper;
    private final RetrainService retrainService;

    @Override
    public void addFavorite(Long userId, Long productOptionId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        ProductOption option = productOptionRepository.findById(productOptionId)
                .orElseThrow(() -> new RuntimeException("Product option not found"));

        favoriteRepository.findByUserAndProductOption(user, option)
                .ifPresent(fav -> { throw new RuntimeException("Already in favorites"); });

        ProductFavorite favorite = new ProductFavorite();
        favorite.setUser(user);
        favorite.setProductOption(option);
        favoriteRepository.save(favorite);
        retrainService.notifyRetrain();
    }

    @Override

    public void removeFavorite(Long userId, Long productOptionId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        ProductOption option = productOptionRepository.findById(productOptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cấu hình sản phẩm"));

        ProductFavorite favorite = favoriteRepository.findByUserAndProductOption(user, option)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không nằm trong danh sách yêu thích"));

        favoriteRepository.delete(favorite);
        retrainService.notifyRetrain();
    }

    @Override
    public List<ProductFavoriteResponse> getFavorites(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return favoriteRepository.findByUser(user)
                .stream()
                .map(favoriteMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isFavorite(Long userId, Long productOptionId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        ProductOption option = productOptionRepository.findById(productOptionId)
                .orElseThrow(() -> new RuntimeException("Product option not found"));
        return favoriteRepository.findByUserAndProductOption(user, option).isPresent();
    }
}
