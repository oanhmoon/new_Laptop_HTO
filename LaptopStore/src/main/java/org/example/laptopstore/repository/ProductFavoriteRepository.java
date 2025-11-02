package org.example.laptopstore.repository;

import org.example.laptopstore.entity.ProductFavorite;
import org.example.laptopstore.entity.User;
import org.example.laptopstore.entity.ProductOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductFavoriteRepository extends JpaRepository<ProductFavorite, Long> {
    List<ProductFavorite> findByUser(User user);
    Optional<ProductFavorite> findByUserAndProductOption(User user, ProductOption productOption);
    void deleteByUserAndProductOption(User user, ProductOption productOption);
}
