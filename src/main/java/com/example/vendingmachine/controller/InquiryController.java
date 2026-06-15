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

    // 전체 문의 목록
    @GetMapping("/list")
    public String listPage(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        model.addAttribute("inquiries", inquiryService.findAll());
        model.addAttribute("currentUser", username);
        // 사용자 알림 개수
        model.addAttribute("unreadCount", inquiryService.countUnreadReplies(username));
        return "inquiry-list";
    }

    // 내 문의 목록
    @GetMapping("/my")
    public String myListPage(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        model.addAttribute("inquiries", inquiryService.findByUsername(username));
        model.addAttribute("currentUser", username);
        model.addAttribute("unreadCount", inquiryService.countUnreadReplies(username));
        return "inquiry-my";
    }

    // 문의 상세 + 답글
    @GetMapping("/detail/{id}")
    public String detailPage(@PathVariable Long id, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUser = auth.getName();
        Inquiry inquiry = inquiryService.findById(id);

        // 비밀글이면 작성자랑 관리자만
        if (inquiry.isSecret() && !inquiry.getUsername().equals(currentUser) && !currentUser.equals("admin")) {
            return "redirect:/inquiry/list";
        }

        // 읽음 처리
        inquiryService.markRepliesAsRead(inquiry, currentUser);

        List<InquiryReply> replies = inquiryService.findReplies(inquiry);
        model.addAttribute("inquiry", inquiry);
        model.addAttribute("replies", replies);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("unreadCount", inquiryService.countUnreadReplies(currentUser));
        return "inquiry-detail";
    }

    // 답글 등록 (관리자 또는 문의 작성자만)
    @PostMapping("/reply/{id}")
    public String reply(@PathVariable Long id, @RequestParam String content) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUser = auth.getName();
        Inquiry inquiry = inquiryService.findById(id);

        // 관리자이거나 문의 작성자만 답글 가능
        if (!currentUser.equals("admin") && !inquiry.getUsername().equals(currentUser)) {
            return "redirect:/inquiry/list";
        }

        InquiryReply reply = new InquiryReply();
        reply.setInquiry(inquiry);
        reply.setContent(content);
        reply.setUsername(currentUser);
        reply.setAdminReply(currentUser.equals("admin"));

        inquiryService.saveReply(reply);
        return "redirect:/inquiry/detail/" + id;
    }
    // 문의 수정 페이지
    @GetMapping("/edit/{id}")
    public String editPage(@PathVariable Long id, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Inquiry inquiry = inquiryService.findById(id);

        // 본인 글만 수정 가능
        if (!inquiry.getUsername().equals(auth.getName())) {
            return "redirect:/inquiry/list";
        }

        model.addAttribute("inquiry", inquiry);
        return "inquiry-edit";
    }

    // 문의 수정 처리
    @PostMapping("/edit/{id}")
    public String edit(@PathVariable Long id,
                       @RequestParam String title,
                       @RequestParam String content,
                       @RequestParam(defaultValue = "false") boolean secret) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Inquiry inquiry = inquiryService.findById(id);

        if (!inquiry.getUsername().equals(auth.getName())) {
            return "redirect:/inquiry/list";
        }

        inquiry.setTitle(title);
        inquiry.setContent(content);
        inquiry.setSecret(secret);

        inquiryService.save(inquiry);
        return "redirect:/inquiry/detail/" + id;
    }
}