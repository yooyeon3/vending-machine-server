package com.example.vendingmachine.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "inquiry")
@Getter
@Setter
@NoArgsConstructor
public class Inquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 제목
    private String title;

    // 내용
    @Column(length = 2000)
    private String content;

    // 작성자 아이디
    private String username;

    // 비밀글 여부
    private boolean secret;

    // 작성 시간
    private LocalDateTime createdAt;

    // 답글 목록
    @OneToMany(mappedBy = "inquiry", cascade = CascadeType.ALL)
    private List<InquiryReply> replies = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}