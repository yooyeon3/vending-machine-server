package com.example.vendingmachine.controller;

import com.example.vendingmachine.domain.Member;
import com.example.vendingmachine.domain.PurchaseHistory;
import com.example.vendingmachine.repository.PurchaseHistoryRepository;
import com.example.vendingmachine.service.MemberService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/orders")
public class OrderViewController {

    private final PurchaseHistoryRepository purchaseHistoryRepository;
    private final MemberService memberService;

    @GetMapping("/my")
    public String myOrders(HttpSession session, Model model) {
        Member loginMember = (Member) session.getAttribute("loginMember");
        if (loginMember == null) {
            return "redirect:/login";
        }

        // 현재 사용자의 주문 내역 조회 (이름 기준 - 현재 시스템 사양)
        List<PurchaseHistory> orders = purchaseHistoryRepository.findByBuyerName(loginMember.getName());
        
        // 내림차순 정렬 (최신순)
        orders.sort((a, b) -> b.getPurchaseTime().compareTo(a.getPurchaseTime()));

        // 등급 정보 조회
        String grade = memberService.getGrade(loginMember);

        model.addAttribute("orders", orders);
        model.addAttribute("grade", grade);
        model.addAttribute("totalSpent", orders.stream().mapToInt(o -> o.getPaidPrice() != null ? o.getPaidPrice() : 0).sum());
        model.addAttribute("orderCount", orders.size());
        model.addAttribute("now", java.time.LocalDateTime.now());

        return "order-history";
    }
}
