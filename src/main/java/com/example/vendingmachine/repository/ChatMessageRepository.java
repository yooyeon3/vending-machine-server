package com.example.vendingmachine.repository;

import com.example.vendingmachine.domain.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByUserUsernameOrderByCreatedAtAsc(String userUsername);

    List<ChatMessage> findTop1ByUserUsernameOrderByCreatedAtDesc(String userUsername);

    long countByUserUsernameAndFromAdminTrueAndIsReadFalse(String userUsername);

    long countByFromAdminFalseAndIsReadFalse();

    long countByUserUsernameAndFromAdminFalseAndIsReadFalse(String userUsername);

    List<ChatMessage> findByUserUsernameAndFromAdminTrueAndIsReadFalse(String userUsername);

    List<ChatMessage> findByUserUsernameAndFromAdminFalseAndIsReadFalse(String userUsername);

    @Query("SELECT DISTINCT m.userUsername FROM ChatMessage m")
    List<String> findDistinctUserUsernames();
}
