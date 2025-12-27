package org.example.laptopstore.repository;

import org.example.laptopstore.entity.ChatHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ChatHistoryRepository extends JpaRepository<ChatHistory, Long> {

    @Query("""
        SELECT c FROM ChatHistory c
        WHERE c.userId = :userId
        ORDER BY c.createdAt DESC
        LIMIT 10
    """)
    List<ChatHistory> findLast10(Long userId);
}
