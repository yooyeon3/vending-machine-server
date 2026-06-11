package com.example.vendingmachine.service;

import com.example.vendingmachine.domain.Member;
import com.example.vendingmachine.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberService {

    private final MemberRepository memberRepository;

    public void join(Member member) {
        // 아이디 중복 체크 로직
        memberRepository.findByUsername(member.getUsername()).ifPresent(m -> {
            throw new IllegalStateException("이미 존재하는 회원입니다.");
        });
        memberRepository.save(member);
    }
}