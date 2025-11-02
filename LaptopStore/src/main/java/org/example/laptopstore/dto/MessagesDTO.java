package org.example.laptopstore.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class MessagesDTO {
    private Long id;
    private Long senderId;
    private Long receiverId;
    private String content;
    private LocalDateTime createdAt;

    private String messageType; // "TEXT", "IMAGE", "VIDEO"
    private String mediaUrl;

    // JPQL projection constructor (ensure order matches repository query)
    public MessagesDTO(Long id, String content, LocalDateTime createdAt, Long senderId, Long receiverId, String messageType, String mediaUrl) {
        this.id = id;
        this.content = content;
        this.createdAt = createdAt;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.messageType = messageType;
        this.mediaUrl = mediaUrl;
    }
}
