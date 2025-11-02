package org.example.laptopstore.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.laptopstore.dto.MessagesDTO;
import org.example.laptopstore.dto.response.PageResponse;
import org.example.laptopstore.dto.response.message.UserMessage;
import org.example.laptopstore.entity.Message;
import org.example.laptopstore.entity.User;
import org.example.laptopstore.repository.MessageChatRepository;
import org.example.laptopstore.repository.UserRepository;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


@Service
@RequiredArgsConstructor
public class MessageChatService {

    private final MessageChatRepository messageChatRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;


public Message saveMessage(MessagesDTO message) {
    Message m = new Message();

    // map content and media info
    m.setContent(message.getContent() != null ? message.getContent() : "");
    m.setMessageType(message.getMessageType());
    m.setMediaUrl(message.getMediaUrl());

    // set createdAt if provided else now
    if (message.getCreatedAt() != null) {
        m.setCreatedAt(message.getCreatedAt());
    } else {
        m.setCreatedAt(LocalDateTime.now());
    }

    User sender = userRepository.findById(message.getSenderId())
            .orElseThrow(() -> new RuntimeException("Sender not found"));
    User receiver = userRepository.findById(message.getReceiverId())
            .orElseThrow(() -> new RuntimeException("Receiver not found"));
    m.setSender(sender);
    m.setReceiver(receiver);

    return messageChatRepository.save(m);
}

    public PageResponse<UserMessage> getAllUser(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Long adminId = 1L; // or inject dynamically if needed
        Page<UserMessage> users = userRepository.getAllUser(adminId, pageable);
        return new PageResponse<>(users);
    }

    public PageResponse<MessagesDTO> getMessageUser(int page, int size, Long receiverId, Long senderId) {
        Pageable pageable = PageRequest.of(page , size);
        // repository expects senderId, receiverId
        Page<MessagesDTO> userMessages = messageChatRepository.getMessagesById(senderId, receiverId, pageable);
        List<MessagesDTO> modifiableMessages = new ArrayList<>(userMessages.getContent());
        Collections.reverse(modifiableMessages);
        Page<MessagesDTO> reversedPage = new PageImpl<>(modifiableMessages, pageable, userMessages.getTotalElements());
        return new PageResponse<>(reversedPage);
    }
}
