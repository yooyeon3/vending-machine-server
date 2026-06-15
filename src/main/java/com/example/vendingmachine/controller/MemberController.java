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

    // 회원가입 화면 보여주기
    @GetMapping("/signup")
    public String signupPage(Model model) {
        // th:object="${member}"를 사용하기 위해 빈 객체를 생성하여 모델에 전달합니다.
        model.addAttribute("member", new Member());
        return "signup";
    }

    // 회원가입 처리하기
    @PostMapping("/signup")
    public String signup(@ModelAttribute Member member) {
        // [디버깅용 로그] 데이터가 컨트롤러까지 오는지 확인
        System.out.println("=== 회원가입 컨트롤러 요청 도착 ===");
        System.out.println("아이디: " + member.getUsername());

        // 회원가입 서비스 호출
        memberService.join(member);

        System.out.println("=== 서비스 로직 실행 후 리다이렉트 ===");
        return "redirect:/login"; // 가입 후 로그인 페이지로 이동
    }
}