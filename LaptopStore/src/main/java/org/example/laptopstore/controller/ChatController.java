package org.example.laptopstore.controller;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.example.laptopstore.dto.MessagesDTO;
import org.example.laptopstore.service.impl.MessageChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.ParseException;
import java.util.Map;


@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public  class ChatController {


private final SimpMessagingTemplate messagingTemplate;
    private final MessageChatService messageChatService;
    private final Cloudinary cloudinary;

    @MessageMapping("/sendMessage")
    public void sendMessage(@Payload MessagesDTO message) {
        // Save to DB (returns saved Message entity)
        var saved = messageChatService.saveMessage(message);

        // Build response DTO to send over WS (use MessagesDTO)
        MessagesDTO responseDto = new MessagesDTO(
                saved.getId(),
                saved.getContent(),
                saved.getCreatedAt(),
                saved.getSender().getId(),
                saved.getReceiver().getId(),
                saved.getMessageType(),
                saved.getMediaUrl()
        );

        // Send to receiver and sender (so both update UI)
        messagingTemplate.convertAndSendToUser(
                saved.getReceiver().getId().toString(),
                "/queue/messages",
                responseDto
        );

        messagingTemplate.convertAndSendToUser(
                saved.getSender().getId().toString(),
                "/queue/messages",
                responseDto
        );
    }

    // Upload endpoint: upload file to Cloudinary via server and return secure_url
    @PostMapping("/api/v1/chat/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.emptyMap());
            String url = (String) uploadResult.get("secure_url");
            return ResponseEntity.ok(url);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Upload failed");
        }
    }

}
