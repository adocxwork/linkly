package com.gupta.linkly.service;

import com.gupta.linkly.dto.MessageRequest;
import com.gupta.linkly.dto.MessageResponse;
import com.gupta.linkly.entity.Message;
import com.gupta.linkly.entity.User;
import com.gupta.linkly.exception.BadRequestException;
import com.gupta.linkly.exception.ResourceNotFoundException;
import com.gupta.linkly.repository.MessageRepository;
import com.gupta.linkly.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MessageService {

    private static final int INBOX_LIMIT = 200;

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    @Transactional
    public void saveMessage(String username, MessageRequest request) {
        User user = userRepository.findByUsername(username)
                .filter(u -> !u.suspended())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!Boolean.TRUE.equals(user.getEnablePublicMessaging())) {
            throw new BadRequestException("This creator is not accepting messages");
        }
        String sender = AuthService.blankToNull(request.getSenderName());
        messageRepository.save(Message.builder()
                .senderName(sender == null ? "Anonymous" : sender)
                .content(request.getContent().trim())
                .user(user)
                .build());
    }

    public List<MessageResponse> getUserMessages(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return messageRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), PageRequest.of(0, INBOX_LIMIT)).stream()
                .map(MessageResponse::from)
                .toList();
    }

    @Transactional
    public void deleteMessage(String username, UUID messageId) {
        // Not found and "belongs to someone else" look the same, so IDs cannot be probed.
        Message message = messageRepository.findByIdAndUserUsername(messageId, username)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));
        messageRepository.delete(message);
    }
}
