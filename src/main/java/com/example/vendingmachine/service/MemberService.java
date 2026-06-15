package com.example.vendingmachine.service;

import com.example.vendingmachine.domain.Member;
import com.example.vendingmachine.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public void join(Member member) {
        // [로그 추가] 가입 시도 확인
        System.out.println("가입 시도 확인! 이름: " + member.getUsername());

        // 1. 아이디 중복 체크
        memberRepository.findByUsername(member.getUsername()).ifPresent(m -> {
            throw new IllegalStateException("이미 존재하는 회원입니다.");
        });

        // 2. 비밀번호 암호화 후 저장
        String encodedPassword = passwordEncoder.encode(member.getPassword());
        member.setPassword(encodedPassword);

        memberRepository.save(member);

        // [로그 추가] DB 저장 완료
        System.out.println("DB 저장 완료!");
    }

    public Member login(String username, String password) {
        // 1. DB에서 아이디로 회원 찾아보기
        Member member = memberRepository.findByUsername(username)
                .orElse(null); // 회원이 없으면 null 반환

        // 2. 회원이 없거나, 비밀번호가 일치하지 않으면 null 반환
        if (member == null || !passwordEncoder.matches(password, member.getPassword())) {
            return null; // 로그인 실패
        }

        // 3. 모두 통과하면 회원 정보 반환 (로그인 성공)
        return member;
    }
}