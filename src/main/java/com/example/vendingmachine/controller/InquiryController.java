package com.example.vendingmachine.controller;

import com.example.vendingmachine.domain.Inquiry;
import com.example.vendingmachine.domain.InquiryReply;
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

    // 전체 문의 목록 (비밀글은 제목 숨김)
    @GetMapping("/list")
    public String listPage(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        List<Inquiry> inquiries = inquiryService.findAll();
        model.addAttribute("inquiries", inquiries);
        model.addAttribute("currentUser", auth.getName());
        return "inquiry-list";
    }

    // 내 문의 목록
    @GetMapping("/my")
    public String myListPage(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        List<Inquiry> inquiries = inquiryService.findByUsername(auth.getName());
        model.addAttribute("inquiries", inquiries);
        model.addAttribute("currentUser", auth.getName());
        return "inquiry-my";
    }

    // 문의 상세 + 답글
    @GetMapping("/detail/{id}")
    public String detailPage(@PathVariable Long id, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Inquiry inquiry = inquiryService.findById(id);
        String currentUser = auth.getName();

        // 비밀글이면 작성자랑 관리자만 볼 수 있음
        if (inquiry.isSecret() && !inquiry.getUsername().equals(currentUser) && !currentUser.equals("admin")) {
            return "redirect:/inquiry/list";
        }

        List<InquiryReply> replies = inquiryService.findReplies(inquiry);
        model.addAttribute("inquiry", inquiry);
        model.addAttribute("replies", replies);
        model.addAttribute("currentUser", currentUser);
        return "inquiry-detail";
    }

    // 답글 등록
    @PostMapping("/reply/{id}")
    public String reply(@PathVariable Long id, @RequestParam String content) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Inquiry inquiry = inquiryService.findById(id);

        InquiryReply reply = new InquiryReply();
        reply.setInquiry(inquiry);
        reply.setContent(content);
        reply.setUsername(auth.getName());
        reply.setAdminReply(auth.getName().equals("admin"));

        inquiryService.saveReply(reply);
        return "redirect:/inquiry/detail/" + id;
    }
}