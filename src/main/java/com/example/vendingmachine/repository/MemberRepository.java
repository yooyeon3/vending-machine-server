package com.example.vendingmachine.repository;

import com.example.vendingmachine.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    // 아이디(username)로 회원을 찾기 위한 메서드
    Optional<Member> findByUsername(String username);

    // 이름과 전화번호로 아이디 찾기
    Optional<Member> findByNameAndPhoneNumber(String name, String phoneNumber);

    // 아이디와 전화번호로 비밀번호 찾기(존재 확인용)
    Optional<Member> findByUsernameAndPhoneNumber(String username, String phoneNumber);
}