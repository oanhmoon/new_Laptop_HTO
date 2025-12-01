package org.example.laptopstore.repository;

import org.example.laptopstore.entity.UserViewHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserViewHistoryRepository extends JpaRepository<UserViewHistory, Long> {

    Optional<UserViewHistory> findByUserIdAndProductOptionId(Long userId, Long productOptionId);

}
