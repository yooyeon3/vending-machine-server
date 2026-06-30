package com.example.vendingmachine.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_message")
@Getter
@Setter
@NoArgsConstructor
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_username")
    private String userUsername;
    
    private String content;
    
    @Column(name = "from_admin")
    private boolean fromAdmin;

    @Column(name = "is_read")
    private boolean isRead;

    @Column(name = "created_at")
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
        this.isRead = false;
    }

    public void markAsRead() {
        this.isRead = true;
    }
}
