package com.example.vendingmachine.controller;

import com.example.vendingmachine.domain.Member;
import com.example.vendingmachine.domain.PurchaseHistory;
import com.example.vendingmachine.repository.PurchaseHistoryRepository;
import com.example.vendingmachine.service.ChatService;
import com.example.vendingmachine.service.MemberService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/orders")
public class OrderViewController {

    private final PurchaseHistoryRepository purchaseHistoryRepository;
    private final MemberService memberService;
    private final ChatService chatService;

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
        MemberService.GradeProgress progress = memberService.getGradeProgress(loginMember);

        // 최애 음료 TOP 3 조회
        List<Object[]> topProductsRaw = purchaseHistoryRepository.findTopProductsByBuyerName(loginMember.getName(), PageRequest.of(0, 3));
        List<Map<String, Object>> topProducts = topProductsRaw.stream()
                .map(obj -> Map.of("name", obj[0], "count", obj[1]))
                .collect(Collectors.toList());

        model.addAttribute("orders", orders);
        model.addAttribute("progress", progress);
        model.addAttribute("topProducts", topProducts);
        model.addAttribute("totalSpent", orders.stream().mapToInt(o -> o.getPaidPrice() != null ? o.getPaidPrice() : 0).sum());
        model.addAttribute("totalEarnedPoints", orders.stream().mapToInt(o -> o.getEarnedPoints() != null ? o.getEarnedPoints() : 0).sum());
        model.addAttribute("orderCount", orders.size());
        model.addAttribute("now", java.time.LocalDateTime.now());
        model.addAttribute("unreadCount", chatService.countUnreadForUser(loginMember.getUsername()));

        return "order-history";
    }
}
