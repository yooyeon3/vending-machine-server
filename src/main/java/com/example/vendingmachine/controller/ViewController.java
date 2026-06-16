package com.example.vendingmachine.controller;

import com.example.vendingmachine.domain.Member;
import com.example.vendingmachine.repository.MemberRepository;
import com.example.vendingmachine.repository.ProductRepository;
import com.example.vendingmachine.service.ChatService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class ViewController {

    private final ChatService chatService;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;

    @GetMapping("/")
    public String home(Authentication authentication, HttpSession session, Model model) {
        if (authentication != null && authentication.isAuthenticated()
                && !authentication.getName().equals("anonymousUser")) {

            // 관리자이면 바로 관리자 페이지로
            if (authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
                return "redirect:/admin";
            }

            // loginMember 세션이 끊겼으면 DB에서 다시 복구
            Member loginMember = (Member) session.getAttribute("loginMember");
            if (loginMember == null) {
                memberRepository.findByUsername(authentication.getName()).ifPresent(m -> {
                    session.setAttribute("loginMember", m);
                });
                loginMember = (Member) session.getAttribute("loginMember");
            }

            if (loginMember != null) {
                model.addAttribute("unreadCount", chatService.countUnreadForUser(loginMember.getUsername()));
            }
        }
        model.addAttribute("products", productRepository.findAll());
        return "index";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }
}
