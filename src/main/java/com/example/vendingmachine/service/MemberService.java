package com.example.vendingmachine.service;

import com.example.vendingmachine.domain.Member;
import com.example.vendingmachine.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; // 암호화 라이브러리
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberService {

    private final MemberRepository memberRepository;
    private final BCryptPasswordEncoder passwordEncoder; // 암호화 객체 주입

    public void join(Member member) {
        // 1. 아이디 중복 체크
        memberRepository.findByUsername(member.getUsername()).ifPresent(m -> {
            throw new IllegalStateException("이미 존재하는 회원입니다.");
        });

        // 2. 비밀번호 암호화 후 저장
        String encodedPassword = passwordEncoder.encode(member.getPassword());
        member.setPassword(encodedPassword);

        memberRepository.save(member);
    }

    public Member login(String username, String password) {
        // 1. DB에서 아이디로 회원 찾아보기
        Member member = memberRepository.findByUsername(username)
                .orElse(null); // 회원이 없으면 null 반환

        // 2. 회원이 없거나, 비밀번호가 일치하지 않으면 null 반환 (passwordEncoder.matches 사용!)
        if (member == null || !passwordEncoder.matches(password, member.getPassword())) {
            return null; // 로그인 실패
        }

        // 3. 모두 통과하면 회원 정보 반환 (로그인 성공)
        return member;

    }

}