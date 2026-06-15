package com.example.vendingmachine.controller;

import com.example.vendingmachine.service.InquiryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class AdminController {

    private final InquiryService inquiryService;

    @GetMapping("/admin")
    public String adminPage(Model model) {
        // 신규 문의 + 사용자 답글 합산 (빨간 숫자)
        long inquiryCount = inquiryService.countUnanswered() + inquiryService.countUnreadUserReplies();
        model.addAttribute("inquiryCount", inquiryCount);
        // 전체 문의 목록
        model.addAttribute("inquiries", inquiryService.findAll());
        return "admin";
    }
}