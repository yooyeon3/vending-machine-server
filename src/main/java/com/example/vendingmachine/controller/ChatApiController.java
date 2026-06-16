package com.example.vendingmachine.controller;

import com.example.vendingmachine.domain.ChatMessage;
import com.example.vendingmachine.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatApiController {

    private final ChatService chatService;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    @GetMapping("/history")
    public List<Map<String, Object>> getUserHistory(Authentication auth) {
        String username = auth.getName();
        chatService.markAdminMessagesAsRead(username);
        return toResponse(chatService.getHistory(username));
    }

    @GetMapping("/history/{username}")
    public List<Map<String, Object>> getHistoryForAdmin(@PathVariable String username) {
        chatService.markUserMessagesAsRead(username);
        return toResponse(chatService.getHistory(username));
    }

    @GetMapping("/list")
    public List<Map<String, Object>> getChatList() {
        return chatService.getChatList();
    }

    private List<Map<String, Object>> toResponse(List<ChatMessage> messages) {
        return messages.stream().map(msg -> {
            Map<String, Object> m = new HashMap<>();
            m.put("content", msg.getContent());
            m.put("fromAdmin", msg.isFromAdmin());
            m.put("time", msg.getCreatedAt() != null ? msg.getCreatedAt().format(TIME_FORMAT) : "");
            m.put("userUsername", msg.getUserUsername());
            return m;
        }).collect(Collectors.toList());
    }
}
