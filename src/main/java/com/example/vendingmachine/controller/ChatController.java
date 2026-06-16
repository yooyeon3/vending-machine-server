package com.example.vendingmachine.controller;

import com.example.vendingmachine.domain.ChatMessage;
import com.example.vendingmachine.service.ChatService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.security.Principal;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    @GetMapping("/chat")
    public String userChatPage(Authentication authentication, Model model) {
        String username = authentication.getName();
        List<ChatMessage> history = chatService.getHistory(username);
        chatService.markAdminMessagesAsRead(username);
        model.addAttribute("messages", history);
        model.addAttribute("username", username);
        return "chat";
    }

    @GetMapping("/admin/chat")
    public String adminChatList(Model model) {
        model.addAttribute("chatList", chatService.getChatList());
        return "admin-chat";
    }

    @GetMapping("/admin/chat/{username}")
    public String adminChatDetail(@PathVariable String username, Model model) {
        List<ChatMessage> history = chatService.getHistory(username);
        chatService.markUserMessagesAsRead(username);
        model.addAttribute("messages", history);
        model.addAttribute("targetUsername", username);
        return "admin-chat-detail";
    }

    @MessageMapping("/chat/user/send")
    public void userSend(@Payload MessageDto dto, Principal principal) {
        if (principal == null) return;
        String username = principal.getName();

        ChatMessage message = ChatMessage.builder()
                .userUsername(username)
                .content(dto.getContent())
                .fromAdmin(false)
                .build();
        chatService.save(message);

        Map<String, Object> payload = buildPayload(message);
        messagingTemplate.convertAndSendToUser(username, "/queue/chat", payload);
        messagingTemplate.convertAndSendToUser("admin", "/queue/chat", payload);
    }

    @MessageMapping("/chat/admin/send")
    public void adminSend(@Payload MessageDto dto, Principal principal) {
        if (principal == null) return;
        String targetUsername = dto.getTargetUsername();

        ChatMessage message = ChatMessage.builder()
                .userUsername(targetUsername)
                .content(dto.getContent())
                .fromAdmin(true)
                .build();
        chatService.save(message);

        Map<String, Object> payload = buildPayload(message);
        messagingTemplate.convertAndSendToUser(targetUsername, "/queue/chat", payload);
        messagingTemplate.convertAndSendToUser("admin", "/queue/chat", payload);
    }

    private Map<String, Object> buildPayload(ChatMessage message) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("content", message.getContent());
        payload.put("fromAdmin", message.isFromAdmin());
        payload.put("userUsername", message.getUserUsername());
        payload.put("time", message.getCreatedAt() != null
                ? message.getCreatedAt().format(TIME_FORMAT) : "");
        return payload;
    }

    @Getter
    @Setter
    public static class MessageDto {
        private String content;
        private String targetUsername;
    }
}
