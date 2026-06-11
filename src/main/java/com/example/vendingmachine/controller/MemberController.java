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

    @GetMapping("/signup")
    public String signupPage() {
        return "signup"; // templates/signup.html 호출
    }

    @PostMapping("/signup")
    public String signup(Member member) {
        memberService.join(member);
        return "redirect:/login"; // 가입 후 로그인 페이지로 이동
    }
}