package com.example.vendingmachine.controller;

import com.example.vendingmachine.domain.Member;
import com.example.vendingmachine.repository.MemberRepository;
import com.example.vendingmachine.repository.ProductRepository;
import com.example.vendingmachine.service.ChatService;
import com.example.vendingmachine.service.MemberService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import com.example.vendingmachine.repository.PurchaseHistoryRepository;
import com.example.vendingmachine.domain.PurchaseHistory;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class ViewController {

    private final ChatService chatService;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;
    private final MemberService memberService;
    private final PurchaseHistoryRepository purchaseHistoryRepository;

    @GetMapping("/")
    public String home(Authentication authentication, HttpSession session, Model model) {
        if (authentication != null && authentication.isAuthenticated()
                && !authentication.getName().equals("anonymousUser")) {

            if (authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
                return "redirect:/admin";
            }

            Member loginMember = (Member) session.getAttribute("loginMember");
            if (loginMember == null) {
                memberRepository.findByUsername(authentication.getName()).ifPresent(m -> {
                    session.setAttribute("loginMember", m);
                });
                loginMember = (Member) session.getAttribute("loginMember");
            }

            if (loginMember != null) {
                model.addAttribute("unreadCount", chatService.countUnreadForUser(loginMember.getUsername()));
                
                // 등급 및 혜택 정보 추가
                String grade = memberService.getGrade(loginMember);
                model.addAttribute("grade", grade);
                model.addAttribute("discountRate", (int)(memberService.getDiscountRate(grade) * 100));
                model.addAttribute("pointRate", (int)(memberService.getPointRate(grade) * 100));
            }
        }
        model.addAttribute("products", productRepository.findAll());
        return "index";
    }

    @GetMapping("/robot")
    public String robot(Authentication authentication, HttpSession session, Model model) {
        if (authentication != null && authentication.isAuthenticated()
                && !authentication.getName().equals("anonymousUser")) {

            if (authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
                return "redirect:/admin";
            }

            Member loginMember = (Member) session.getAttribute("loginMember");
            if (loginMember == null) {
                memberRepository.findByUsername(authentication.getName()).ifPresent(m -> {
                    session.setAttribute("loginMember", m);
                });
                loginMember = (Member) session.getAttribute("loginMember");
            }

            if (loginMember != null) {
                model.addAttribute("unreadCount", chatService.countUnreadForUser(loginMember.getUsername()));
                
                String grade = memberService.getGrade(loginMember);
                model.addAttribute("grade", grade);
                model.addAttribute("discountRate", (int)(memberService.getDiscountRate(grade) * 100));
                model.addAttribute("pointRate", (int)(memberService.getPointRate(grade) * 100));
            }
        } else {
            return "redirect:/login"; // 로봇 메뉴는 로그인 필수
        }
        
        return "robot";
    }

}
