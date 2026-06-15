package com.example.vendingmachine.controller;

import com.example.vendingmachine.domain.Member;
import com.example.vendingmachine.service.MemberService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.RequestParam;
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

    @PostMapping("/login")
    public String login(@RequestParam("username") String username,
                        @RequestParam("password") String password,
                        HttpServletRequest request) {

        // 서비스에 로그인 확인 요청
        Member loginMember = memberService.login(username, password);

        // 로그인 실패 시
        if (loginMember == null) {
            // 이미 만들어두신 login.html의 에러 메시지가 뜨도록 URL에 ?error를 붙여서 보냅니다.
            return "redirect:/login?error";
        }

        // 로그인 성공 시: 세션(Session)을 생성하여 로그인 상태를 기억하게 만듭니다.
        HttpSession session = request.getSession();
        session.setAttribute("loginMember", loginMember);

        // 로그인 성공 후 메인 홈 화면으로 이동
        return "redirect:/";
    }

    // 👇 새로 추가된 로그아웃 기능 👇
    @GetMapping("/logout")
    public String logout(HttpServletRequest request) {
        // 기존 세션이 있으면 가져오고, 없으면 새로 만들지 않음 (false)
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate(); // 현재 로그인된 세션 정보를 완전히 삭제합니다.
        }
        // 로그아웃 처리 후 비로그인 홈 화면(랜딩 페이지)으로 이동
        return "redirect:/";
    }
}