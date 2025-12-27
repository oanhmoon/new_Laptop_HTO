package org.example.laptopstore.controller;

import lombok.RequiredArgsConstructor;
import org.example.laptopstore.dto.request.ChatRequest;
import org.example.laptopstore.dto.response.ChatResponse;
import org.example.laptopstore.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatAIController {

    private final ChatService chatService;

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest req) {
        String answer = chatService.handleChat(req.getQuestion(), req.getUserId());
        ChatResponse resp = new ChatResponse();
        resp.setAnswer(answer);
        return ResponseEntity.ok(resp);
    }
}
