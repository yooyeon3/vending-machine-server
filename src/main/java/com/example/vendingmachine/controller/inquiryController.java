package com.example.vendingmachine.controller;

import com.example.vendingmachine.domain.Inquiry;
import com.example.vendingmachine.service.InquiryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/inquiry")
public class InquiryController {

    private final InquiryService inquiryService;

    // 문의 작성 페이지
    @GetMapping("/write")
    public String writePage() {
        return "inquiry-write";
    }

    // 문의 등록 처리
    @PostMapping("/write")
    public String write(@RequestParam String title,
                        @RequestParam String content,
                        @RequestParam(defaultValue = "false") boolean secret) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        Inquiry inquiry = new Inquiry();
        inquiry.setTitle(title);
        inquiry.setContent(content);
        inquiry.setSecret(secret);
        inquiry.setUsername(auth.getName());

        inquiryService.save(inquiry);
        return "redirect:/inquiry/list";
    }

    // 문의 목록 페이지
    @GetMapping("/list")
    public String listPage(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        List<Inquiry> inquiries = inquiryService.findByUsername(username);
        model.addAttribute("inquiries", inquiries);
        return "inquiry-list";
    }
}