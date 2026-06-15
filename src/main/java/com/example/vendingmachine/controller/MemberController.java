package com.example.vendingmachine.controller;

import com.example.vendingmachine.domain.Member;
import com.example.vendingmachine.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;



    // 회원가입 화면 보여주기
    @GetMapping("/signup")
    public String signupPage() {
        return "signup";
    }

    // 회원가입 처리하기
    @PostMapping("/signup")
    public String signup(Member member) {
        memberService.join(member);
        return "redirect:/login"; // 가입 후 로그인 페이지로 이동
    }

    /* 💡 기존에 있던 @PostMapping("/login") 과 @GetMapping("/logout") 은
       이제 SecurityConfig가 자동으로 처리하므로 모두 삭제했습니다! */
}