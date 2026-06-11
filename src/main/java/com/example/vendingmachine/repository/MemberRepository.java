package com.example.vendingmachine.repository;

import com.example.vendingmachine.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    // 아이디(username)로 회원을 찾기 위한 메서드
    Optional<Member> findByUsername(String username);
}