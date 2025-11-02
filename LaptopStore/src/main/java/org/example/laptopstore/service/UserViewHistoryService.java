package org.example.laptopstore.service;

import lombok.RequiredArgsConstructor;
import org.example.laptopstore.entity.Product;
import org.example.laptopstore.entity.User;
import org.example.laptopstore.entity.UserViewHistory;
import org.example.laptopstore.repository.ProductRepository;
import org.example.laptopstore.repository.UserRepository;
import org.example.laptopstore.repository.UserViewHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserViewHistoryService {
    private final UserViewHistoryRepository historyRepo;
    private final ProductRepository productRepo;
    private final UserRepository userRepo;

    @Transactional
    public void recordView(Long userId, Long productId) {
        if (userId == null || productId == null) return;

        User user = userRepo.findById(userId).orElse(null);
        Product product = productRepo.findById(productId).orElse(null);
        if (user == null || product == null) return;

        historyRepo.findByUserIdAndProductId(userId, productId).ifPresentOrElse(
                history -> {
                    history.setViewCount(history.getViewCount() + 1);
                    historyRepo.save(history);
                },
                () -> {
                    UserViewHistory newHistory = new UserViewHistory();
                    newHistory.setUser(user);
                    newHistory.setProduct(product);
                    historyRepo.save(newHistory);
                }
        );
    }
}
