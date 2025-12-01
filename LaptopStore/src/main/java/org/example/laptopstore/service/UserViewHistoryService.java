package org.example.laptopstore.service;

import lombok.RequiredArgsConstructor;
import org.example.laptopstore.entity.Product;
import org.example.laptopstore.entity.ProductOption;
import org.example.laptopstore.entity.User;
import org.example.laptopstore.entity.UserViewHistory;
import org.example.laptopstore.repository.ProductOptionRepository;
import org.example.laptopstore.repository.ProductRepository;
import org.example.laptopstore.repository.UserRepository;
import org.example.laptopstore.repository.UserViewHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserViewHistoryService {
    private final UserViewHistoryRepository historyRepo;
    private final ProductOptionRepository productOptionRepo;
    private final UserRepository userRepo;

    @Transactional

    public void recordView(Long userId, Long optionId) {
        if (userId == null || optionId == null) return;

        User user = userRepo.findById(userId).orElse(null);
        // optionId chính là product_option_id
        ProductOption option = productOptionRepo.findById(optionId).orElse(null);

        if (user == null || option == null) return;

        historyRepo.findByUserIdAndProductOptionId(userId, optionId)
                .ifPresentOrElse(
                history -> {
                    history.setViewCount(history.getViewCount() + 1);
                    historyRepo.save(history);
                },
                () -> {
                    UserViewHistory newHistory = new UserViewHistory();
                    newHistory.setUser(user);
                    newHistory.setProductOption(option);
                    historyRepo.save(newHistory);
                }
        );
    }

}
