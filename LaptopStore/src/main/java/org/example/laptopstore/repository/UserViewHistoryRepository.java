package org.example.laptopstore.repository;

import org.example.laptopstore.entity.ProductOption;
import org.example.laptopstore.entity.UserViewHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserViewHistoryRepository extends JpaRepository<UserViewHistory, Long> {

    Optional<UserViewHistory> findByUserIdAndProductOptionId(Long userId, Long productOptionId);

    // lấy sản phẩm được xem nhiều nhất
    @Query("""
        SELECT u.productOption 
        FROM UserViewHistory u 
        GROUP BY u.productOption 
        ORDER BY SUM(u.viewCount) DESC
    """)
    List<ProductOption> findMostViewedProducts();

}
