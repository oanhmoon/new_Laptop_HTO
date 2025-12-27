package org.example.laptopstore.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private String role; // "user" hoặc "assistant"

    @Column(columnDefinition = "TEXT")
    private String message;

    private LocalDateTime createdAt;
}
