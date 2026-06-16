package com.example.vendingmachine.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userUsername;
    private String content;
    private boolean fromAdmin;
    private boolean read;
    private LocalDateTime createdAt;

    @PrePersist
    protected void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    @Builder
    public ChatMessage(String userUsername, String content, boolean fromAdmin) {
        this.userUsername = userUsername;
        this.content = content;
        this.fromAdmin = fromAdmin;
        this.read = false;
    }

    public void markAsRead() {
        this.read = true;
    }
}
