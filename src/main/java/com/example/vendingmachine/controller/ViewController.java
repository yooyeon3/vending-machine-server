package com.example.vendingmachine.controller;

import com.example.vendingmachine.domain.Member;
import com.example.vendingmachine.service.InquiryService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class ViewController {

    private final InquiryService inquiryService;

    @GetMapping("/")
    public String home(HttpSession session, Model model) {
        Member loginMember = (Member) session.getAttribute("loginMember");
        if (loginMember != null) {
            model.addAttribute("unreadCount", inquiryService.countUnreadReplies(loginMember.getUsername()));
        }
        return "index";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }
}