package com.example.vendingmachine.controller;

import com.example.vendingmachine.domain.Member;
import com.example.vendingmachine.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    // 1. 로그인 화면 보여주기
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    // 2. 회원가입 화면 보여주기
    @GetMapping("/signup")
    public String signupPage(Model model) {
        model.addAttribute("member", new Member());
        return "signup";
    }

    // 3. 회원가입 처리하기
    @PostMapping("/signup")
    public String signup(@ModelAttribute Member member, Model model) {
        System.out.println("=== 회원가입 컨트롤러 요청 도착 ===");
        System.out.println("아이디: " + member.getUsername());

        try {
            memberService.join(member);
        } catch (IllegalStateException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("member", member);
            return "signup";
        }

        System.out.println("=== 서비스 로직 실행 후 리다이렉트 ===");
        return "redirect:/login";
    }
}