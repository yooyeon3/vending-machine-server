package com.example.vendingmachine.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter @Setter
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // DB에서 관리하는 고유 번호

    @Column(name = "userId", unique = true, nullable = false)
    private String username; // 로그인 아이디

    @Column(nullable = false)
    private String password; // 비밀번호

    private String name;     // 회원 이름

    @Column(name = "phone_number")
    private String phoneNumber; // 전화번호

    private java.time.LocalDateTime createdAt; // 가입일

    @Column(name = "admin_memo", columnDefinition = "TEXT")
    private String adminMemo; // 관리자용 메모

    private Integer points = 0; // 고객 보유 포인트

    @Column(name = "role")
    private String role = "user";

    @PrePersist
    public void prePersist() {
        this.createdAt = java.time.LocalDateTime.now();
    }
}