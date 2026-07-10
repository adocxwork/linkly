package com.gupta.linkly.service;

import com.gupta.linkly.dto.MessageRequest;
import com.gupta.linkly.dto.MessageResponse;
import com.gupta.linkly.entity.Message;
import com.gupta.linkly.entity.User;
import com.gupta.linkly.exception.ResourceNotFoundException;
import com.gupta.linkly.repository.MessageRepository;
import com.gupta.linkly.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    public void saveMessage(String username, MessageRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (Boolean.FALSE.equals(user.getEnablePublicMessaging())) {
            throw new IllegalArgumentException("Public messaging is not enabled for this user");
        }

        Message message = Message.builder()
                .senderName(request.getSenderName())
                .content(request.getContent())
                .user(user)
                .build();

        messageRepository.save(message);
    }

    public List<MessageResponse> getUserMessages(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return messageRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(msg -> MessageResponse.builder()
                        .id(msg.getId())
                        .senderName(msg.getSenderName())
                        .content(msg.getContent())
                        .createdAt(msg.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    public void deleteMessage(String username, java.util.UUID messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));

        if (!message.getUser().getUsername().equals(username)) {
            throw new IllegalArgumentException("You don't have permission to delete this message");
        }

        messageRepository.delete(message);
    }
}
