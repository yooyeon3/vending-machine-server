package com.example.vendingmachine.service;

import com.example.vendingmachine.domain.ChatMessage;
import com.example.vendingmachine.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MM/dd HH:mm");

    public ChatMessage save(ChatMessage message) {
        return chatMessageRepository.save(message);
    }

    public List<ChatMessage> getHistory(String username) {
        return chatMessageRepository.findByUserUsernameOrderByCreatedAtAsc(username);
    }

    public long countUnreadForUser(String username) {
        return chatMessageRepository.countByUserUsernameAndFromAdminTrueAndIsReadFalse(username);
    }

    public long countUnreadForAdmin() {
        return chatMessageRepository.countByFromAdminFalseAndIsReadFalse();
    }

    public long countUnreadFromUser(String username) {
        return chatMessageRepository.countByUserUsernameAndFromAdminFalseAndIsReadFalse(username);
    }

    public void markAdminMessagesAsRead(String username) {
        chatMessageRepository.findByUserUsernameAndFromAdminTrueAndIsReadFalse(username)
                .forEach(ChatMessage::markAsRead);
    }

    public void markUserMessagesAsRead(String username) {
        chatMessageRepository.findByUserUsernameAndFromAdminFalseAndIsReadFalse(username)
                .forEach(ChatMessage::markAsRead);
    }

    public List<Map<String, Object>> getChatList() {
        List<String> usernames = chatMessageRepository.findDistinctUserUsernames();
        return usernames.stream().map(username -> {
            Map<String, Object> item = new HashMap<>();
            item.put("username", username);
            List<ChatMessage> lastList = chatMessageRepository.findTop1ByUserUsernameOrderByCreatedAtDesc(username);
            if (!lastList.isEmpty()) {
                ChatMessage last = lastList.get(0);
                item.put("lastMessage", last.getContent());
                item.put("lastTime", last.getCreatedAt().format(DATE_FORMAT));
                item.put("sortKey", last.getCreatedAt());
            } else {
                item.put("lastMessage", "");
                item.put("lastTime", "");
                item.put("sortKey", null);
            }
            item.put("unreadCount", chatMessageRepository.countByUserUsernameAndFromAdminFalseAndIsReadFalse(username));
            return item;
        }).sorted((a, b) -> {
            var ta = (java.time.LocalDateTime) a.get("sortKey");
            var tb = (java.time.LocalDateTime) b.get("sortKey");
            if (ta == null && tb == null) return 0;
            if (ta == null) return 1;
            if (tb == null) return -1;
            return tb.compareTo(ta);
        }).collect(Collectors.toList());
    }
}
